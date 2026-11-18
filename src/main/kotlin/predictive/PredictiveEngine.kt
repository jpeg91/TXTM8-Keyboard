// src/main/kotlin/predictive/PredictiveEngine.kt
package predictive

import java.io.InputStream

class Entry internal constructor(val word: String, val baseScore: Long) {
    internal var userCount = 0
    internal var userAdded = false
}

/** One suggestion. [isCompletion] is true when the word is longer than the keys pressed so far. */
data class Candidate(val word: String, val isCompletion: Boolean)

/**
 * Digit-keyed trie. Each node keeps:
 *  - exact: words whose key sequence ends exactly here
 *  - deeper: the best [topK] words further down the trie (for completions)
 * so a lookup never has to scan a whole subtree, even for a single key press.
 *
 * Score = baseScore (dictionary frequency) + userCount * userWeight
 *         (+ bigramWeight per time the word followed the previous word, at lookup time).
 * Dictionaries built by tools/build_dictionary.py use 100 * ln(count), roughly 1..1700,
 * so the default userWeight of 200 makes each pick worth about a 7x frequency gap.
 * If you load other data, tune the weights to its scale.
 */
class PredictiveEngine(
    private val userWeight: Long = 200L,
    private val topK: Int = 12,
    private val bigramWeight: Long = 120L,
) {
    private class Node {
        val children = arrayOfNulls<Node>(8)
        val exact = ArrayList<Entry>()
        var deeper: List<Entry> = emptyList()
    }

    private val root = Node()
    private val byWord = HashMap<String, Entry>()
    private val bigrams = HashMap<String, HashMap<String, Int>>()
    private val removed = HashSet<String>() // words the user removed from the suggestions
    private var pairCount = 0
    private var built = false

    /** Goes up whenever something is learned, so callers know when to save. */
    var changeCount: Int = 0
        private set

    private fun score(e: Entry): Long = e.baseScore + e.userCount * userWeight
    private val byScore = compareByDescending<Entry> { score(it) }.thenBy { it.word }

    /** Adds a word. Call [build] after a bulk load (or use [load]). */
    fun add(word: String, baseScore: Long = 1L) {
        if (byWord.containsKey(word) || word in removed) return
        val digits = KeypadMap.encode(word) ?: return
        var node = root
        for (d in digits) {
            val i = d - '2'
            val next = node.children[i] ?: Node().also { node.children[i] = it }
            node = next
        }
        val entry = Entry(word, baseScore)
        node.exact.add(entry)
        byWord[word] = entry
        if (built) refreshPath(digits)
    }

    /** Loads lines of "word<space or tab>frequency". Blank lines and #comments are skipped. */
    fun load(lines: Sequence<String>) {
        built = false
        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val parts = t.split('\t', ' ')
            add(parts[0], parts.getOrNull(1)?.toLongOrNull() ?: 1L)
        }
        build()
    }

    /** Loads a dictionary file (UTF-8, same format as [load]), e.g. from Android assets. */
    fun loadFrom(stream: InputStream) {
        stream.bufferedReader(Charsets.UTF_8).useLines { load(it) }
    }

    fun build() {
        buildAll(root)
        built = true
    }

    /**
     * Exact matches first, then longer completions. If [previousWord] is given, words that
     * have followed it before are ranked higher (and can appear as completions).
     */
    fun lookup(digits: String, previousWord: String? = null, maxCompletions: Int = 8): List<Candidate> {
        var node = root
        for (d in digits) {
            if (d !in '2'..'9') return emptyList()
            node = node.children[d - '2'] ?: return emptyList()
        }
        val prev = previousWord?.lowercase()
        val rank = compareByDescending<Entry> { score(it) + bigramBonus(prev, it.word) }.thenBy { it.word }

        val out = ArrayList<Candidate>()
        node.exact.sortedWith(rank).forEach { out += Candidate(it.word, false) }

        val pool = LinkedHashSet<Entry>(node.deeper)
        if (prev != null) {
            bigrams[prev]?.keys?.forEach { w ->
                val e = byWord[w] ?: return@forEach
                val enc = KeypadMap.encode(w) ?: return@forEach
                if (enc.length > digits.length && enc.startsWith(digits)) pool.add(e)
            }
        }
        pool.sortedWith(rank).take(maxCompletions).forEach { out += Candidate(it.word, true) }
        return out
    }

    /** Call when the user picks a word: it will rank higher next time. */
    fun learn(word: String) {
        val entry = byWord[word] ?: return
        entry.userCount++
        changeCount++
        refreshWord(word)
    }

    /** Remembers that [word] followed [prev] (both lowercase dictionary forms). */
    fun learnPair(prev: String, word: String) {
        if (!byWord.containsKey(word)) return
        val m = bigrams.getOrPut(prev) { HashMap() }
        val c = m[word]
        if (c == null) {
            if (pairCount >= MAX_PAIRS) return
            pairCount++
        }
        m[word] = (c ?: 0) + 1
        changeCount++
    }

    /**
     * Words that have followed [previousWord] before, most often first (ties: higher word score).
     * Empty when nothing has been learned for that word yet.
     */
    fun nextWords(previousWord: String?, max: Int = 6): List<String> {
        val m = bigrams[previousWord?.lowercase() ?: return emptyList()] ?: return emptyList()
        return m.entries
            .filter { byWord.containsKey(it.key) }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { score(byWord.getValue(it.key)) }
                    .thenBy { it.key },
            )
            .take(max)
            .map { it.key }
    }

    /**
     * Takes a word out of the suggestions for good (it is remembered in the saved data, so it
     * stays gone after a restart). Its word pairs go with it. Typing the word letter by letter
     * ([addUserWord]) brings it back. Returns false if the engine did not have the word.
     */
    fun removeWord(word: String): Boolean {
        if (!dropWord(word)) return false
        removed.add(word)
        forgetPairsOf(word)
        changeCount++
        return true
    }

    /** Forgets that [word] follows [prev], so it stops being offered as a next word. */
    fun forgetPair(prev: String, word: String): Boolean {
        val p = prev.lowercase()
        val m = bigrams[p] ?: return false
        if (m.remove(word) == null) return false
        pairCount--
        if (m.isEmpty()) bigrams.remove(p)
        changeCount++
        return true
    }

    /** Adds a user-spelled word (e.g. from multi-tap mode) and boosts it. */
    fun addUserWord(word: String, baseScore: Long = USER_WORD_BASE_SCORE) {
        if (removed.remove(word)) changeCount++ // typed on purpose: it is welcome again
        val isNew = !byWord.containsKey(word)
        add(word, baseScore)
        if (isNew) byWord[word]?.userAdded = true
        learn(word)
    }

    // --- saving what the user has taught the engine ---

    /** Text form of everything learned: boosted words, user-added words and word pairs. */
    fun exportUserData(): String = buildString {
        for (w in removed) append("X\t").append(w).append("\t0\t0\n")
        for (e in byWord.values) {
            if (e.userCount > 0 || e.userAdded) {
                append("W\t").append(e.word).append('\t').append(e.userCount)
                    .append('\t').append(if (e.userAdded) 1 else 0).append('\n')
            }
        }
        for ((prev, m) in bigrams) {
            for ((w, c) in m) {
                append("B\t").append(prev).append('\t').append(w).append('\t').append(c).append('\n')
            }
        }
    }

    /** Restores data written by [exportUserData]. Call after the dictionary is loaded. */
    fun importUserData(lines: Sequence<String>) {
        for (line in lines) {
            val p = line.split('\t')
            if (p.size < 2) continue
            when {
                p[0] == "X" -> {
                    dropWord(p[1])
                    removed.add(p[1])
                }
                p.size >= 4 && p[0] == "W" -> restoreWord(p[1], p[2].toIntOrNull() ?: 0, p[3] == "1")
                p.size >= 4 && p[0] == "B" -> restorePair(p[1], p[2], p[3].toIntOrNull() ?: 0)
            }
        }
    }

    private fun restoreWord(word: String, count: Int, added: Boolean) {
        if (count < 0) return
        var e = byWord[word]
        if (e == null) {
            if (!added) return // no longer in the dictionary
            add(word, USER_WORD_BASE_SCORE)
            e = byWord[word] ?: return
            e.userAdded = true
        }
        e.userCount = count
        refreshWord(word)
    }

    private fun restorePair(prev: String, word: String, count: Int) {
        if (count <= 0 || !byWord.containsKey(word) || prev in removed || pairCount >= MAX_PAIRS) return
        val m = bigrams.getOrPut(prev) { HashMap() }
        if (!m.containsKey(word)) pairCount++
        m[word] = count
    }

    // --- internals ---

    /** Takes [word] out of the trie and the word table. Returns false if it was not there. */
    private fun dropWord(word: String): Boolean {
        val entry = byWord.remove(word) ?: return false
        val digits = KeypadMap.encode(word) ?: return true
        var node: Node? = root
        for (d in digits) node = node?.children?.get(d - '2')
        node?.exact?.remove(entry)
        if (built) refreshPath(digits)
        return true
    }

    /** Removes every word pair that has [word] on either side. */
    private fun forgetPairsOf(word: String) {
        bigrams.remove(word)?.let { pairCount -= it.size }
        val iter = bigrams.entries.iterator()
        while (iter.hasNext()) {
            val m = iter.next().value
            if (m.remove(word) != null) pairCount--
            if (m.isEmpty()) iter.remove()
        }
    }

    private fun bigramBonus(prev: String?, word: String): Long {
        if (prev == null) return 0L
        val c = bigrams[prev]?.get(word) ?: return 0L
        return minOf(c, BIGRAM_CAP) * bigramWeight
    }

    private fun refreshWord(word: String) {
        if (built) KeypadMap.encode(word)?.let { refreshPath(it) }
    }

    private fun rebuildDeeper(node: Node) {
        val pool = ArrayList<Entry>()
        for (c in node.children) {
            if (c != null) {
                pool.addAll(c.exact)
                pool.addAll(c.deeper)
            }
        }
        pool.sortWith(byScore)
        node.deeper = if (pool.size > topK) pool.subList(0, topK).toList() else pool
    }

    private fun buildAll(node: Node) {
        for (c in node.children) if (c != null) buildAll(c)
        rebuildDeeper(node)
    }

    /** Recompute `deeper` only along the path to [digits], bottom-up. */
    private fun refreshPath(digits: String) {
        val path = ArrayList<Node>(digits.length + 1)
        var node = root
        path += node
        for (d in digits) {
            node = node.children[d - '2'] ?: return
            path += node
        }
        // The leaf's own `deeper` is unaffected; fix its ancestors.
        for (i in path.size - 2 downTo 0) rebuildDeeper(path[i])
    }

    companion object {
        const val USER_WORD_BASE_SCORE = 400L
        const val BIGRAM_CAP = 6
        const val MAX_PAIRS = 20_000
    }
}

package t9

import java.io.InputStream

class Entry internal constructor(val word: String, val baseScore: Long) {
    internal var userCount = 0
}

/** One suggestion. [isCompletion] is true when the word is longer than the keys pressed so far. */
data class Candidate(val word: String, val isCompletion: Boolean)

/**
 * Digit-keyed trie. Each node keeps:
 *  - exact: words whose key sequence ends exactly here
 *  - deeper: the best [topK] words further down the trie (for completions)
 * so a lookup never has to scan a whole subtree, even for a single key press.
 *
 * Score = baseScore (dictionary frequency) + userCount * userWeight.
 * Dictionaries built by tools/build_dictionary.py use 100 * ln(count), roughly 1..1700,
 * so the default userWeight of 200 makes each pick worth about a 7x frequency gap.
 * If you load other data, tune [userWeight] to its scale.
 */
class T9Engine(
    private val userWeight: Long = 200L,
    private val topK: Int = 12,
) {
    private class Node {
        val children = arrayOfNulls<Node>(8)
        val exact = ArrayList<Entry>()
        var deeper: List<Entry> = emptyList()
    }

    private val root = Node()
    private val byWord = HashMap<String, Entry>()
    private var built = false

    private fun score(e: Entry): Long = e.baseScore + e.userCount * userWeight
    private val byScore = compareByDescending<Entry> { score(it) }.thenBy { it.word }

    /** Adds a word. Call [build] after a bulk load (or use [load]). */
    fun add(word: String, baseScore: Long = 1L) {
        if (byWord.containsKey(word)) return
        val digits = T9Keymap.encode(word) ?: return
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

    /** Exact matches first (best score first), then longer completions. */
    fun lookup(digits: String, maxCompletions: Int = 8): List<Candidate> {
        var node = root
        for (d in digits) {
            if (d !in '2'..'9') return emptyList()
            node = node.children[d - '2'] ?: return emptyList()
        }
        val out = ArrayList<Candidate>()
        node.exact.sortedWith(byScore).forEach { out += Candidate(it.word, false) }
        node.deeper.take(maxCompletions).forEach { out += Candidate(it.word, true) }
        return out
    }

    /** Call when the user picks a word: it will rank higher next time. */
    fun learn(word: String) {
        val entry = byWord[word] ?: return
        entry.userCount++
        val digits = T9Keymap.encode(word) ?: return
        if (built) refreshPath(digits)
    }

    /** Adds a user-spelled word (e.g. from multi-tap mode) and boosts it. */
    fun addUserWord(word: String, baseScore: Long = 400L) {
        add(word, baseScore)
        learn(word)
    }

    // --- internals ---

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
}

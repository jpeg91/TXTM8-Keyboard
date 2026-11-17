// src/main/kotlin/predictive/PredictiveSession.kt
package predictive

/**
 * The state of the word currently being typed in predictive mode.
 * The keyboard service owns one of these and mirrors [displayText]
 * into the editor's composing region.
 */
class PredictiveSession(private val engine: PredictiveEngine) {
    private val digits = StringBuilder()
    private var candidates: List<Candidate> = emptyList()
    private var index = 0

    /**
     * Set by [backspace] when no word has as many letters as keys are left: the shown word, cut back to that many
     * letters, so each backspace visibly removes one character instead of holding on to a long completion.
     */
    private var heldText: String? = null

    /** The (lowercase) word before the one being typed. Set by the keyboard; used for ranking. */
    var previousWord: String? = null

    val isComposing: Boolean get() = digits.isNotEmpty()
    val digitString: String get() = digits.toString()
    val candidateList: List<Candidate> get() = candidates
    val selectedIndex: Int get() = index

    /** What the suggestion strip lists: the held text (see [heldText]) first if there is one, then the candidates. */
    val stripWords: List<String>
        get() = (heldText?.let { listOf(it) } ?: emptyList()) + candidates.map { it.word }

    /** Which of [stripWords] is the one being typed. */
    val stripSelected: Int get() = if (heldText != null) 0 else index

    /** What to show underlined in the text field. */
    val displayText: String
        get() = heldText ?: candidates.getOrNull(index)?.word ?: fallbackText()

    fun pressDigit(d: Char) {
        require(d in '2'..'9') { "Not a letter key: $d" }
        digits.append(d)
        refresh()
    }

    /** Replaces the whole key sequence (used to re-open an existing word). */
    fun restore(keys: String) {
        cancel()
        for (d in keys) pressDigit(d)
    }

    /**
     * Removes the last key. The shown word loses its last letter too: if the shorter start of it is a word it
     * is shown, and if no word is that long the start of the shown word is held, so a long completion never
     * stays on screen through several presses. Returns false if there was nothing to delete (the caller should
     * delete a real character).
     */
    fun backspace(): Boolean {
        if (digits.isEmpty()) return false
        val shown = displayText
        digits.setLength(digits.length - 1)
        refresh()
        if (digits.isNotEmpty()) stepBack(shown)
        return true
    }

    private fun stepBack(shown: String) {
        val start = firstLetters(shown, digits.length)
        val match = candidates.indexOfFirst { !it.isCompletion && it.word.equals(start, ignoreCase = true) }
        if (match >= 0) {
            index = match
        } else if (candidates.firstOrNull()?.isCompletion == true) {
            heldText = start
        }
    }

    /** The first [letters] letters of [word]; apostrophes are not letters (they are not typed) but stay inside. */
    private fun firstLetters(word: String, letters: Int): String {
        val sb = StringBuilder()
        var count = 0
        for (ch in word) {
            if (count == letters) break
            sb.append(ch)
            if (ch != '\'' && ch != '\u2019') count++
        }
        return sb.toString()
    }

    fun next() {
        if (heldText != null) {
            heldText = null
            index = 0
        } else if (candidates.isNotEmpty()) {
            index = (index + 1) % candidates.size
        }
    }

    fun previous() {
        if (heldText != null) {
            heldText = null
            index = candidates.lastIndex.coerceAtLeast(0)
        } else if (candidates.isNotEmpty()) {
            index = (index - 1 + candidates.size) % candidates.size
        }
    }

    /** Jump straight to an entry of [stripWords] (e.g. the user tapped it in the strip). */
    fun select(i: Int) {
        if (heldText != null) {
            if (i == 0) return // the held start of the word is already the choice
            if (i - 1 in candidates.indices) {
                heldText = null
                index = i - 1
            }
        } else if (i in candidates.indices) {
            index = i
        }
    }

    /** When false, finishing a word does not teach the engine anything. */
    var learning: Boolean = true

    /** Finishes the word, teaches the engine, and returns the text to insert. */
    fun commit(): String? {
        if (digits.isEmpty()) return null
        val word = displayText
        if (learning && candidates.isNotEmpty()) {
            engine.learn(word)
            previousWord?.let { engine.learnPair(it, word) }
        }
        cancel()
        return word
    }

    fun cancel() {
        digits.setLength(0)
        candidates = emptyList()
        index = 0
        heldText = null
    }

    private fun refresh() {
        heldText = null
        candidates = if (digits.isEmpty()) emptyList() else engine.lookup(digits.toString(), previousWord)
        index = 0
    }

    // Unknown word: show the first letter of each key so the user sees something.
    private fun fallbackText(): String = buildString {
        for (d in digits) append(KeypadMap.keyLetters.getValue(d)[0])
    }
}

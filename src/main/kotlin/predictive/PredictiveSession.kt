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

    /** The (lowercase) word before the one being typed. Set by the keyboard; used for ranking. */
    var previousWord: String? = null

    val isComposing: Boolean get() = digits.isNotEmpty()
    val digitString: String get() = digits.toString()
    val candidateList: List<Candidate> get() = candidates
    val selectedIndex: Int get() = index

    /** What to show underlined in the text field. */
    val displayText: String
        get() = candidates.getOrNull(index)?.word ?: fallbackText()

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

    /** Returns false if there was nothing to delete (caller should delete a real character). */
    fun backspace(): Boolean {
        if (digits.isEmpty()) return false
        digits.setLength(digits.length - 1)
        refresh()
        return true
    }

    fun next() {
        if (candidates.isNotEmpty()) index = (index + 1) % candidates.size
    }

    fun previous() {
        if (candidates.isNotEmpty()) index = (index - 1 + candidates.size) % candidates.size
    }

    /** Jump straight to a candidate (e.g. the user tapped it in the strip). */
    fun select(i: Int) {
        if (i in candidates.indices) index = i
    }

    /** Finishes the word, teaches the engine, and returns the text to insert. */
    fun commit(): String? {
        if (digits.isEmpty()) return null
        val word = displayText
        if (candidates.isNotEmpty()) {
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
    }

    private fun refresh() {
        candidates = if (digits.isEmpty()) emptyList() else engine.lookup(digits.toString(), previousWord)
        index = 0
    }

    // Unknown word: show the first letter of each key so the user sees something.
    private fun fallbackText(): String = buildString {
        for (d in digits) append(KeypadMap.keyLetters.getValue(d)[0])
    }
}

package t9

import java.text.Normalizer

/** The classic ITU keypad layout: 2=ABC ... 9=WXYZ. */
object T9Keymap {

    val keyLetters: Map<Char, String> = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz",
    )

    private val letterToDigit: Map<Char, Char> = buildMap {
        for ((digit, letters) in keyLetters) {
            for (c in letters) put(c, digit)
        }
    }

    fun digitFor(c: Char): Char? = letterToDigit[c.lowercaseChar()]

    /**
     * Converts a word to its key sequence ("home" -> "4663").
     * Accents are stripped ("café" -> "2233") and apostrophes are skipped
     * ("don't" -> "366"), so the user never has to type them.
     * Returns null if the word contains anything that isn't a letter.
     */
    fun encode(word: String): String? {
        val sb = StringBuilder(word.length)
        for (ch in stripAccents(word)) {
            if (ch == '\'' || ch == '\u2019') continue
            sb.append(digitFor(ch) ?: return null)
        }
        return if (sb.isEmpty()) null else sb.toString()
    }

    private fun stripAccents(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
}

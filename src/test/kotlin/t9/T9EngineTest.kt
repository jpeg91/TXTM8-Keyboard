// src/test/kotlin/t9/T9EngineTest.kt
package t9

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class T9EngineTest {

    private fun engine() = T9Engine().apply {
        load(
            sequenceOf(
                "good 700", "home 650", "gone 600", "hood 400", "hone 300",
                "goods 350", "homes 340", "the 900", "he 850", "if 750", "id 500",
            )
        )
    }

    @Test
    fun encodesWords() {
        assertEquals("4663", T9Keymap.encode("home"))
        assertEquals("3668", T9Keymap.encode("don't"))
        assertEquals("2233", T9Keymap.encode("caf\u00e9"))
        assertEquals(null, T9Keymap.encode("a1"))
    }

    @Test
    fun exactMatchesRankByFrequency() {
        assertEquals(listOf("he", "if", "id"), engine().lookup("43").map { it.word })
    }

    @Test
    fun completionsFollowExactMatches() {
        val result = engine().lookup("4663")
        assertEquals(listOf("good", "home", "gone", "hood", "hone"), result.take(5).map { it.word })
        assertTrue(result.drop(5).all { it.isCompletion })
        assertEquals(listOf("goods", "homes"), result.drop(5).map { it.word })
    }

    @Test
    fun completionsWhenNoExactMatch() {
        val result = engine().lookup("466")
        assertTrue(result.isNotEmpty() && result.all { it.isCompletion })
        assertEquals("good", result.first().word)
    }

    @Test
    fun learningPromotesWords() {
        val e = engine()
        e.learn("hood")
        e.learn("hood")
        assertEquals("hood", e.lookup("4663").first().word)
        assertEquals("hood", e.lookup("466").first().word)
    }

    @Test
    fun userWordsAreFoundImmediately() {
        val e = engine()
        e.addUserWord("hoof") // 4663 as well
        assertTrue(e.lookup("4663").any { it.word == "hoof" })
    }

    @Test
    fun loadsFromStream() {
        val e = T9Engine()
        e.loadFrom("# comment\ngood 700\nhome 650\n".byteInputStream())
        assertEquals(listOf("good", "home"), e.lookup("4663").map { it.word })
    }

    @Test
    fun sessionCyclesAndCommits() {
        val e = engine()
        val s = T9Session(e)
        "4663".forEach { s.pressDigit(it) }
        assertEquals("good", s.displayText)
        s.next()
        assertEquals("home", s.displayText)
        assertEquals("home", s.commit())
        assertFalse(s.isComposing)
        // home was learned, so it now leads
        "4663".forEach { s.pressDigit(it) }
        assertEquals("home", s.displayText)
    }

    @Test
    fun sessionBackspaceAndFallback() {
        val s = T9Session(engine())
        "999".forEach { s.pressDigit(it) }
        assertEquals("www", s.displayText)
        assertTrue(s.backspace())
        assertTrue(s.backspace())
        assertTrue(s.backspace())
        assertFalse(s.backspace())
    }

    @Test
    fun previousWordPromotesFollowers() {
        val e = engine()
        assertEquals("good", e.lookup("4663").first().word)
        repeat(3) { e.learnPair("i", "gone") }
        assertEquals("gone", e.lookup("4663", "i").first().word)
        assertEquals("good", e.lookup("4663", "you").first().word)
    }

    @Test
    fun previousWordSurfacesCompletions() {
        val e = engine()
        repeat(4) { e.learnPair("i", "homes") }
        // "466" has only completions; the learned follower should lead.
        assertEquals("homes", e.lookup("466", "i").first().word)
    }

    @Test
    fun userDataSurvivesExportAndImport() {
        val a = engine()
        a.learn("hood")
        a.learn("hood")
        a.addUserWord("hoof")
        a.learnPair("i", "gone")
        val dump = a.exportUserData()

        val b = engine()
        b.importUserData(dump.lineSequence())
        assertEquals(a.lookup("4663", "i").map { it.word }, b.lookup("4663", "i").map { it.word })
        assertTrue(b.lookup("4663").any { it.word == "hoof" })
    }

    @Test
    fun sessionRestoreReopensAWord() {
        val s = T9Session(engine())
        s.restore("4663")
        assertEquals("good", s.displayText)
        assertEquals("4663", s.digitString)
    }

    @Test
    fun contractionFixesKeepTheSameKeys() {
        for ((typed, fixed) in Contractions.FIXES) {
            assertEquals(T9Keymap.encode(typed), T9Keymap.encode(fixed), typed)
        }
    }

    @Test
    fun contractionsAreSuggested() {
        val e = engine()
        for ((w, s) in Contractions.WORDS) e.add(w, s)
        assertTrue(e.lookup("43").any { it.word == "i'd" })
        assertTrue(e.lookup("9373").any { it.word == "we're" })
    }
}
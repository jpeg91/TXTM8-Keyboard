// src/main/kotlin/predictive/Contractions.kt
package predictive

/** Built-in contractions, so they are always available whatever the dictionary file contains. */
object Contractions {

    /**
     * Common contractions with rough scores on the dictionary's scale (100 * ln(count)).
     * Words the dictionary already has keep their own score.
     */
    val WORDS: List<Pair<String, Long>> = listOf(
        "don't" to 1550L, "i'm" to 1530L, "it's" to 1525L, "that's" to 1500L, "you're" to 1490L,
        "can't" to 1480L, "didn't" to 1450L, "i'll" to 1440L, "what's" to 1440L, "i've" to 1420L,
        "let's" to 1400L, "he's" to 1400L, "we're" to 1380L, "they're" to 1360L, "there's" to 1360L,
        "won't" to 1340L, "wasn't" to 1340L, "doesn't" to 1330L, "couldn't" to 1330L, "she's" to 1330L,
        "isn't" to 1320L, "i'd" to 1300L, "you've" to 1300L, "you'll" to 1300L, "wouldn't" to 1290L,
        "here's" to 1280L, "haven't" to 1260L, "we'll" to 1260L, "aren't" to 1250L, "you'd" to 1250L,
        "we've" to 1250L, "who's" to 1230L, "where's" to 1230L, "shouldn't" to 1200L, "they'll" to 1190L,
        "they've" to 1180L, "how's" to 1180L, "he'll" to 1150L, "it'll" to 1150L, "hasn't" to 1150L,
        "hadn't" to 1150L, "weren't" to 1150L, "ain't" to 1150L, "he'd" to 1130L, "she'll" to 1120L,
        "they'd" to 1100L, "she'd" to 1100L, "we'd" to 1050L, "that'll" to 1050L, "it'd" to 1000L,
        "would've" to 1050L, "could've" to 1050L, "should've" to 1050L, "might've" to 900L,
        "y'all" to 1000L, "o'clock" to 1000L, "ma'am" to 1000L, "mustn't" to 700L,
    )

    /**
     * Apostrophe-less spellings that are not real words, and the contraction they should become.
     * Used when typing letter by letter (QWERTY and multi-tap). Words that are real on their own
     * (its, were, well, ill, id, wed, lets, wont ...) are deliberately left out.
     */
    val FIXES: Map<String, String> = mapOf(
        "dont" to "don't", "cant" to "can't", "didnt" to "didn't", "doesnt" to "doesn't",
        "isnt" to "isn't", "arent" to "aren't", "wasnt" to "wasn't", "werent" to "weren't",
        "hasnt" to "hasn't", "havent" to "haven't", "hadnt" to "hadn't", "couldnt" to "couldn't",
        "wouldnt" to "wouldn't", "shouldnt" to "shouldn't", "mustnt" to "mustn't", "aint" to "ain't",
        "im" to "i'm", "ive" to "i've", "youre" to "you're", "youve" to "you've", "youll" to "you'll",
        "youd" to "you'd", "theyre" to "they're", "theyve" to "they've", "theyll" to "they'll",
        "theyd" to "they'd", "weve" to "we've", "hes" to "he's", "shes" to "she's", "thats" to "that's",
        "whats" to "what's", "whos" to "who's", "theres" to "there's", "heres" to "here's",
        "wheres" to "where's", "hows" to "how's", "itll" to "it'll", "itd" to "it'd", "thatll" to "that'll",
        "wouldve" to "would've", "couldve" to "could've", "shouldve" to "should've",
        "yall" to "y'all", "oclock" to "o'clock", "maam" to "ma'am",
    )
}

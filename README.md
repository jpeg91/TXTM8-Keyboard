# TXTM8 Keyboard

An Android keyboard for the old and more efficient way of typing on a phone: a number pad with T9-style predictive text.

Press each number once for the letter you want. The keyboard works out the word, learns which words you use, and gets better at putting them first.

You can also switch to multi-tap, where repeated presses cycle through the letters, or use the full QWERTY keyboard when you need it.

The predictive engine starts with a 40,000-word English dictionary, ranked by frequency. It also learns from your typing, including which words tend to follow each other.

There are plenty of themes to choose from, including Nokia 3310, retro and game console-inspired themes, as well as a range of typing, editing and visual features.

Everything runs locally. There is no internet permission, and anything the keyboard learns stays on your phone.

**[Source code on GitHub](https://github.com/jpeg91/TXTM8-Keyboard)**

## What it does

### Predictive typing

Press each number once for each letter. For example, `4663` gives you suggestions for GOOD, HOME, GONE and other words with the same key sequence.

The best guess appears in the text as you type. Other suggestions appear in the strip above the keys. Press `*` to cycle through them, or tap a different suggestion directly.

Press space (`0`) to accept the highlighted word.

### Multi-tap

Hold `#` to switch between predictive and multi-tap modes.

Multi-tap works like older mobile phones: press a number repeatedly to cycle through its letters. For example, `7` gives P, Q, R and S.

The letter is entered when you press another key or the timeout expires. Words typed this way are added to your dictionary.

Hold a number key to enter the digit itself. In number and phone fields, the number pad enters digits directly.

### It learns from you

Words you choose more often gradually move up the suggestions list.

Type a new word using Multi-tap or the included QWERTY keyboard, and it will be added to the predictive dictionary for next time.

It also remembers which words tend to follow each other. After a space, the suggestion strip can offer likely next words based on what you've typed before.

You can remove a suggestion by holding it. Removed words stay out of the list, even after restarting the keyboard. Holding a next-word suggestion removes that word pairing instead.

Everything it learns is saved locally. You can clear learned words at any time from Settings.

### Full QWERTY keyboard

Tap the keyboard icon to switch to a conventional QWERTY layout, with a separate numbers and symbols page.

It includes shift, space, enter and backspace keys, along with clipboard controls and the same cursor and selection gestures as the number pad.

### Emoji and text smileys

The emoji page is available from either symbol page by tapping `;)`.

It contains emoji grouped into sections, followed by old-school text smileys and kaomoji. Each section scrolls sideways, with the whole page scrolling up and down.

### Editing gestures

- Slide along the space key (or `0` on the number pad) to move the cursor.
- With text selected, use the same gesture to adjust either end of the selection.
- Slide left from backspace to select text backwards. Let go to delete the selection.
- Hold the clipboard key and let go to copy selected text.
- Hold the clipboard key and slide to select text from the cursor. Let go to leave the selection in place.
- Tap the clipboard key to paste.
- Backspace straight after a word re-opens it with its suggestions, so you can choose a different one.

Gestures can be switched off or adjusted for speed in Settings.

### Automatic typing features

- **Capitalisation:** Automatically capitalises the start of sentences. Tap `#` to cycle between lower case, a capital for the next letter, and caps lock.
- **Contractions:** Fixes common missing apostrophes when typing letter by letter. `dont` becomes `don't`, `im` becomes `I'm`, and so on.
- **Double space:** Two quick taps on space after a word insert a full stop and a space.
- **Automatic spacing:** Accepting a suggestion adds a space after the word. Typing punctuation straight afterwards takes that space back.
- **Punctuation:** Repeatedly tap `1` to cycle through punctuation marks.

### Themes and settings

There are 32 themes to choose from:

- **P5:** Black and white with a choice of twelve accent colours.
- **Nokia 3310, Game Boy and Game Boy Advance.**
- **Glitter Graphics, Night Sky, Winter, Aurora, Synthwave and Gold on Black:** Pixel-edged keys with twinkling stars.
- **Amber Backlight, Terminal Green, Terminal Amber and Retro Desktop.**
- **Bubbles, Embers, Sakura and Neon Ripples:** Animations that appear wherever you tap.
- **SNES (US and EU), N64, Saturn, PlayStation, PS2, Dreamcast (US, Japan and EU), Xbox and GameCube.**
- **Follow system, Dark and Light.**

Key size can be set to small, medium or large. The typing pulse has an adjustable strength. The star themes have a separate star-density setting, and the tap-animation themes have an effect-amount setting.

Vibration and key click sounds can each be switched on or off. Other settings cover multi-tap timing, learning, next-word suggestions, automatic capitalisation, contractions, double-space full stops and slide gestures.

### Password and number fields

Password fields automatically switch to the plain QWERTY keyboard, with predictive typing and learning disabled.

Number and phone fields use the number pad to enter digits.

---

## Setup

1. Install the APK.
2. Open TXTM8 Keyboard.
3. Press **Enable the keyboard** and enable it in Android's keyboard settings.
4. Return to TXTM8 and press **Switch to TXTM8 Keyboard**.
5. Select TXTM8 Keyboard from the keyboard picker.

You can try the keyboard in the test field on the setup screen.

## Requirements

- Android 8.0 (API 26) or later.
- An English-language dictionary is included.

---

<details>
<summary><b>How it works</b></summary>

### Predictive engine

The predictive engine uses a digit-keyed trie to find words matching the sequence entered on the number pad.

Each word is assigned a base score derived from its frequency in the source word list. More common words start higher in the suggestions.

Exact matches are shown first, followed by longer words that begin with the same key sequence.

The engine keeps a shortlist of likely completions at each point in the trie, rather than searching through the entire dictionary every time a key is pressed.

### Learning

Every time a dictionary word is chosen, its user score increases. This changes its ranking without changing the original dictionary.

The engine also records word pairs. If a word regularly follows another word, it receives an additional ranking bonus when that previous word is present.

Next-word suggestions use these learned pairs, ranked by how often they have occurred.

Words typed in multi-tap or QWERTY mode can be added to the dictionary, so they are available to predictive mode afterwards.

Removed suggestions and learned data are saved in a local file and restored when the engine starts again. Typing a removed word letter by letter adds it back.

### Dictionary

The bundled English dictionary contains 40,000 words.

It is derived from [FrequencyWords](https://github.com/hermitdave/FrequencyWords), using word-frequency data from the OpenSubtitles corpus.

The original frequencies are converted to a logarithmic score:

`score = max(1, round(100 × ln(count)))`

This keeps the scores within a useful range for the engine's own learning weights.

The dictionary is filtered to remove invalid words, fragments and unwanted apostrophe forms. Common contractions are included separately so they remain available.

The dictionary's source and licence information is in `dictionaries/ATTRIBUTION.md`.

The dictionary can be rebuilt using `tools/build_dictionary.py`.

### Key mapping

The number pad follows the traditional telephone layout:

| Key | Letters |
|---|---|
| 2 | ABC |
| 3 | DEF |
| 4 | GHI |
| 5 | JKL |
| 6 | MNO |
| 7 | PQRS |
| 8 | TUV |
| 9 | WXYZ |

Accented characters are normalised to their unaccented equivalents for lookup. Apostrophes are ignored when encoding words, so `don't` and `dont` use the same key sequence.

### Android integration

The keyboard is implemented as an Android Input Method Service.

The predictive engine is kept separate from the Android keyboard interface, allowing it to be tested independently.

The keyboard communicates with the active text field through Android's input connection. It handles composing text, committing words, cursor movement, selection, clipboard actions and editor-specific actions.

Password fields are detected through the editor's input type. Learning and predictive suggestions are disabled for these fields, and the keyboard switches to the plain QWERTY layout.

### Tests

The predictive engine has a Kotlin test suite covering:

- Number-to-letter encoding.
- Exact matches and word completions.
- Frequency-based ranking.
- Learning and user-added words.
- Previous-word suggestions.
- Saving and restoring learned data.
- Removing words and word pairs.
- Multi-tap session behaviour.
- Contraction handling.

</details>

## Building from source

The project uses Kotlin and Android Gradle Plugin.

Requirements:

- JDK 17
- Gradle 8.10.2
- Android SDK 34

Run the tests:

```bash
gradle :test
```

Build the debug APK:

```bash
gradle :app:assembleDebug
```

The APK is generated in:

```text
app/build/outputs/apk/debug/
```

## Privacy

There is no internet permission in the Android manifest.

The dictionary and predictive engine run locally. Learned words, word preferences, word pairings and removed suggestions are stored in the app's private storage.

Nothing is uploaded, and learned data can be cleared from the settings screen.

## Licence and attribution

The bundled dictionary is derived from FrequencyWords and is distributed under CC BY-SA 4.0.

See `dictionaries/ATTRIBUTION.md` for details.

The dictionary-building script and the rest of the source code have their own respective licensing requirements; see the repository for licence information.

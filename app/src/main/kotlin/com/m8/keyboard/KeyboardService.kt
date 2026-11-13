// app/src/main/kotlin/com/m8/keyboard/KeyboardService.kt
package com.m8.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.Toast
import predictive.Contractions
import predictive.PredictiveEngine
import predictive.KeypadMap
import predictive.PredictiveSession
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The keyboard service. Two layouts: the number pad and a plain QWERTY keyboard.
 *
 * Predictive mode: digits go to [PredictiveSession] and the best word is shown as composing text.
 * Multi-tap mode: repeated presses cycle letters (then the digit) with a timeout.
 * Holding a number key types the digit and keeps cycling: hold 7, tap 4 more times for S.
 *
 * Editing gestures: slide along the space key to move the cursor; slide left from backspace to
 * select text and let go to delete it; the paste key inserts the clipboard. With nothing being
 * typed, the suggestion strip offers the words that usually follow the previous word; holding a
 * suggestion removes it.
 */
class KeyboardService :
    InputMethodService(),
    NumberPadView.Listener,
    QwertyKeypadView.Listener,
    EmojiKeyboardView.Listener {

    private enum class CaseMode { LOWER, CAP_NEXT, UPPER }
    private enum class InputMode { PREDICTIVE, MULTI_TAP }

    private lateinit var engine: PredictiveEngine
    private lateinit var session: PredictiveSession
    private lateinit var store: UserDataStore
    private val uiHandler = Handler(Looper.getMainLooper())

    private var settings = KeyboardSettings()
    private var kbTheme = KeyboardTheme.DARK
    private var padView: NumberPadView? = null
    private var qwertyView: QwertyKeypadView? = null
    private var emojiView: EmojiKeyboardView? = null
    private var shownView: View? = null

    private var emojiOpen = false       // the emoji page is showing (over either layout)
    private var fullKeyboard = false    // QWERTY layout showing
    private var userFull = false        // the user's own choice of layout
    private var caseMode = CaseMode.LOWER
    private var userMode = InputMode.PREDICTIVE
    private var forceMultiTap = false   // e.g. password fields
    private var digitsOnly = false      // number / phone fields
    private var learningEnabled = true

    // multi-tap state
    private var mtKey: Char? = null
    private var mtIndex = 0
    private val mtTimeout = Runnable { commitMultiTap() }

    // double-tap space -> ". "
    private var spaceTapAt = 0L

    // the space added after a tapped suggestion: punctuation typed next takes it back
    private var autoSpace = false
    private var autoSpaceAt = 0L
    private var mtDroppedSpace = false // the pending multi-tap punctuation took the automatic space back

    // next-word suggestions on show (dictionary forms), and backspace slide-to-select progress
    private var nextWords: List<String> = emptyList()
    private var slideSelected = 0

    // saving learned data
    private var lastSaved = 0
    private val saveTask = Runnable { saveNow() }
    private var clearToken = 0L

    private val mode: InputMode
        get() = if (forceMultiTap) InputMode.MULTI_TAP else userMode

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate() {
        super.onCreate()
        store = UserDataStore(this)
        settings = KeyboardSettings.load(this)
        kbTheme = KeyboardTheme.resolve(this, settings.theme)
        clearToken = getSharedPreferences(KeyboardSettings.PREFS, MODE_PRIVATE)
            .getLong(KeyboardSettings.KEY_CLEAR_TOKEN, 0L)
        loadEngine()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(mtTimeout)
        uiHandler.removeCallbacks(saveTask)
        saveNow()
        super.onDestroy()
    }

    private fun loadEngine() {
        val e = PredictiveEngine()
        try {
            assets.open("dictionaries/en.txt").use { e.loadFrom(it) }
        } catch (ex: Exception) {
            e.build() // no dictionary packaged: start empty, user words still work
        }
        for ((w, s) in Contractions.WORDS) e.add(w, s) // only adds the ones the dictionary lacks
        store.load(e)
        engine = e
        session = PredictiveSession(e)
        session.learning = settings.learnWords
        lastSaved = e.changeCount
    }

    override fun onCreateInputView(): View {
        val v = currentView()
        shownView = v
        refreshUi()
        return v
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        resetState()
        reloadSettings()
        session.learning = settings.learnWords
        emojiOpen = false

        val type = info.inputType
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        digitsOnly = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
        val isPassword = cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        forceMultiTap = isPassword
        learningEnabled = !isPassword
        fullKeyboard = (userFull || isPassword) && !digitsOnly
        caseMode = if (settings.autoCap && cursorWantsCaps(type)) CaseMode.CAP_NEXT else CaseMode.LOWER

        if (shownView != null) showView()
        padView?.showSymbols(false)
        refreshUi()
    }

    override fun onFinishInput() {
        commitWord()
        saveNow()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val busy = session.isComposing || mtKey != null
        if (busy && candidatesEnd >= 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            // The cursor moved away from our word: drop our state.
            resetState()
            currentInputConnection?.finishComposingText()
            refreshUi()
        } else if (!busy) {
            refreshUi() // the cursor moved or the text changed: the next-word suggestions may differ
        }
    }

    // ---------------------------------------------------------------- views & settings

    private fun currentView(): View {
        return if (emojiOpen) {
            emojiView ?: EmojiKeyboardView(this, this, settings, kbTheme).also { emojiView = it }
        } else if (fullKeyboard) {
            qwertyView ?: QwertyKeypadView(this, this, settings, kbTheme).also { qwertyView = it }
        } else {
            padView ?: NumberPadView(this, this, settings, kbTheme).also { padView = it }
        }
    }

    private fun showView() {
        val v = currentView()
        if (shownView !== v) {
            setInputView(v)
            shownView = v
        }
    }

    private fun reloadSettings() {
        val s = KeyboardSettings.load(this)
        val th = KeyboardTheme.resolve(this, s.theme)
        if (s != settings || th !== kbTheme) {
            settings = s
            kbTheme = th
            padView = null
            qwertyView = null
            emojiView = null
        }
        val token = getSharedPreferences(KeyboardSettings.PREFS, MODE_PRIVATE)
            .getLong(KeyboardSettings.KEY_CLEAR_TOKEN, 0L)
        if (token != clearToken) {
            // "Clear learned words" was pressed in the settings screen.
            clearToken = token
            uiHandler.removeCallbacks(saveTask)
            loadEngine()
        }
    }

    private fun openEmoji() {
        commitWord()
        spaceTapAt = 0L
        autoSpace = false
        emojiOpen = true
        showView()
        refreshUi()
    }

    private fun closeEmoji() {
        emojiOpen = false
        showView()
        refreshUi()
    }

    private fun switchLayout(full: Boolean) {
        commitWord()
        fullKeyboard = full
        userFull = full
        showView()
        refreshUi()
    }

    // ---------------------------------------------------------------- number pad events

    override fun onKeypadKey(key: Char) {
        if (key != '#') pulse()
        val hadAutoSpace = takeAutoSpace()
        if (digitsOnly) {
            currentInputConnection?.commitText(key.toString(), 1)
            return
        }
        if (key != '0') spaceTapAt = 0L
        when (key) {
            '1' -> {
                if (mtKey != '1') {
                    commitWord()
                    // . , ? ! right after a tapped suggestion sit against the word, not after its space
                    if (hadAutoSpace) mtDroppedSpace = removeAutoSpace()
                }
                multiTapPress('1')
            }
            in '2'..'9' -> onLetterKey(key)
            '0' -> onSpace()
            '*' -> onStar()
            '#' -> cycleCase()
        }
        refreshUi()
    }

    override fun onKeypadLongKey(key: Char) {
        spaceTapAt = 0L
        autoSpace = false
        if (key.isDigit()) pulse()
        if (digitsOnly) {
            currentInputConnection?.commitText(key.toString(), 1)
            return
        }
        when {
            key == '#' -> {
                commitWord()
                userMode = if (userMode == InputMode.PREDICTIVE) InputMode.MULTI_TAP else InputMode.PREDICTIVE
            }
            key == '*' -> {
                commitWord()
                padView?.showSymbols(true)
            }
            key == '0' -> {
                commitWord()
                currentInputConnection?.commitText("0", 1)
            }
            key.isDigit() -> startDigitCycle(key)
        }
        refreshUi()
    }

    override fun onKeypadBackspace() = doBackspace()

    override fun onKeypadBackspaceSlide(steps: Int) = slideSelect(steps)

    override fun onKeypadBackspaceSlideEnd(delete: Boolean) = endSlideSelect(delete)

    override fun onKeypadEnter() = doEnter()

    override fun onKeypadCursorMove(steps: Int, startDirection: Int) = moveCursor(steps, startDirection)

    override fun onKeypadPaste() = pasteClipboard()

    override fun onKeypadCopy() = copySelection()

    override fun onKeypadSettings() = openSettings()

    override fun onKeypadCandidateLong(index: Int) = removeSuggestion(index)

    /** Tapping a suggestion types that word and a space after it. */
    override fun onKeypadCandidate(index: Int) {
        spaceTapAt = 0L
        autoSpace = false
        if (!session.isComposing) {
            acceptNextWord(index)
            return
        }
        pulse()
        session.select(index)
        commitWord()
        insertSpace()
        markAutoSpace()
        updateAutoCap()
        refreshUi()
    }

    override fun onKeypadSymbol(symbol: String) {
        spaceTapAt = 0L
        val hadAutoSpace = takeAutoSpace()
        pulse()
        commitWord()
        if (hadAutoSpace && symbol.firstOrNull()?.let { it in AUTO_SPACE_PUNCTUATION } == true) removeAutoSpace()
        currentInputConnection?.commitText(symbol, 1)
        refreshUi()
    }

    override fun onKeypadQwerty() = switchLayout(true)

    override fun onKeypadEmoji() = openEmoji()

    // ---------------------------------------------------------------- QWERTY events

    override fun onQwertyText(text: String, isLetter: Boolean) {
        val ic = currentInputConnection ?: return
        spaceTapAt = 0L
        val hadAutoSpace = takeAutoSpace()
        pulse()
        if (isLetter) {
            ic.commitText(caseAdjust(text), 1)
            if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        } else {
            if (hadAutoSpace && text.firstOrNull()?.let { it in AUTO_SPACE_PUNCTUATION } == true) removeAutoSpace()
            ic.commitText(text, 1)
        }
        refreshUi()
    }

    override fun onQwertySpace() {
        autoSpace = false
        pulse()
        fixContraction()
        learnTrailingWord()
        insertSpace()
        updateAutoCap()
        refreshUi()
    }

    override fun onQwertyBackspace() = doBackspace()

    override fun onQwertyBackspaceSlide(steps: Int) = slideSelect(steps)

    override fun onQwertyBackspaceSlideEnd(delete: Boolean) = endSlideSelect(delete)

    override fun onQwertyCursorMove(steps: Int, startDirection: Int) = moveCursor(steps, startDirection)

    override fun onQwertyPaste() = pasteClipboard()

    override fun onQwertyCopy() = copySelection()

    override fun onQwertySettings() = openSettings()

    override fun onQwertyEnter() = doEnter()

    override fun onQwertyShift() {
        cycleCase()
        refreshUi()
    }

    override fun onQwertyPad() = switchLayout(false)

    override fun onQwertyEmoji() = openEmoji()

    // ---------------------------------------------------------------- emoji page events

    override fun onEmojiText(text: String) {
        val ic = currentInputConnection ?: return
        spaceTapAt = 0L
        autoSpace = false
        pulse()
        commitWord()
        ic.commitText(text, 1)
        refreshUi()
    }

    override fun onEmojiBack() = closeEmoji()

    override fun onEmojiSpace() = onQwertySpace()

    override fun onEmojiBackspace() = doBackspace()

    override fun onEmojiBackspaceSlide(steps: Int) = slideSelect(steps)

    override fun onEmojiBackspaceSlideEnd(delete: Boolean) = endSlideSelect(delete)

    override fun onEmojiEnter() = doEnter()

    // ---------------------------------------------------------------- key actions

    private fun onLetterKey(key: Char) {
        if (mode == InputMode.MULTI_TAP || mtKey == key) {
            // mtKey == key: continuing a hold-to-type-digit cycle, even in predictive mode
            multiTapPress(key)
        } else {
            commitMultiTap() // a pending punctuation mark or digit, if any
            if (!session.isComposing) session.previousWord = previousWordBefore(currentInputConnection)
            session.pressDigit(key)
            showComposing()
        }
    }

    private fun onSpace() {
        commitWord()
        if (mode == InputMode.MULTI_TAP) {
            fixContraction()
            learnTrailingWord()
        }
        insertSpace()
        updateAutoCap()
    }

    /** * = next candidate while composing; otherwise open the symbol picker. */
    private fun onStar() {
        if (mode == InputMode.PREDICTIVE && session.isComposing) {
            session.next()
            showComposing()
        } else {
            commitWord()
            padView?.showSymbols(true)
        }
    }

    /** # (and the QWERTY shift key) cycle abc -> Abc -> ABC. */
    private fun cycleCase() {
        caseMode = when (caseMode) {
            CaseMode.LOWER -> CaseMode.CAP_NEXT
            CaseMode.CAP_NEXT -> CaseMode.UPPER
            CaseMode.UPPER -> CaseMode.LOWER
        }
        if (session.isComposing) showComposing()
    }

    /** Hold a number key: type the digit and carry on cycling (hold 7, tap 4 more times -> S). */
    private fun startDigitCycle(key: Char) {
        commitWord()
        val symbols = symbolsFor(key)
        mtKey = key
        mtIndex = symbols.lastIndex // the digit is the last item in the cycle
        currentInputConnection?.setComposingText(symbols[mtIndex].toString(), 1)
        uiHandler.removeCallbacks(mtTimeout)
        uiHandler.postDelayed(mtTimeout, settings.multiTapMs.toLong())
    }

    private fun doBackspace() {
        val ic = currentInputConnection ?: return
        spaceTapAt = 0L
        autoSpace = false
        when {
            mtKey != null -> {
                uiHandler.removeCallbacks(mtTimeout)
                // cancelling punctuation that took the automatic space back gives the space back
                ic.setComposingText(if (mtDroppedSpace) " " else "", 1)
                if (mtDroppedSpace) ic.finishComposingText()
                mtDroppedSpace = false
                mtKey = null
                mtIndex = 0
            }
            mode == InputMode.PREDICTIVE && session.isComposing -> {
                session.backspace()
                showComposing()
            }
            tryReopenWord(ic) -> Unit
            else -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        refreshUi()
    }

    private fun doEnter() {
        spaceTapAt = 0L
        autoSpace = false
        commitWord()
        if (fullKeyboard || mode == InputMode.MULTI_TAP) {
            fixContraction()
            learnTrailingWord()
        }
        val info = currentInputEditorInfo
        val ic = currentInputConnection
        if (info != null && ic != null) {
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (!noEnterAction &&
                action != EditorInfo.IME_ACTION_NONE &&
                action != EditorInfo.IME_ACTION_UNSPECIFIED
            ) {
                ic.performEditorAction(action)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
        }
        updateAutoCap()
        refreshUi()
    }

    /**
     * Slide along space: move the cursor [steps] characters (negative = left). With text selected
     * it moves one end of the selection instead: a slide that started leftwards controls the
     * left end (left grows the selection, right shrinks it), and one that started rightwards
     * controls the right end (right grows it, left shrinks it).
     */
    private fun moveCursor(steps: Int, startDirection: Int) {
        if (steps == 0) return
        val ic = currentInputConnection ?: return
        autoSpace = false
        // With text selected, sliding grows the selection (left or right) instead of moving the cursor.
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            if (!moveSelectionEdge(ic, steps, startDirection)) {
                sendShiftArrow(ic, if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT, abs(steps))
            }
            return
        }
        commitWord() // finish any word in progress; the cursor is about to leave it
        spaceTapAt = 0L
        val code = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        ic.beginBatchEdit()
        repeat(abs(steps)) { sendDownUpKeyEvents(code) }
        ic.endBatchEdit()
    }

    /**
     * Moves one end of the selection by [steps] characters (negative = left, positive = right):
     * the left end when [edge] is negative, the right end otherwise. The selection always keeps at
     * least one character, so the two ends never cross. Returns false if the editor will not tell
     * us where its text is.
     */
    private fun moveSelectionEdge(ic: InputConnection, steps: Int, edge: Int): Boolean {
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return false
        val length = extracted.text?.length ?: return false
        var start = min(extracted.selectionStart, extracted.selectionEnd)
        var end = max(extracted.selectionStart, extracted.selectionEnd)
        if (start == end) return false
        if (edge < 0) start = (start + steps).coerceIn(0, end - 1) else end = (end + steps).coerceIn(start + 1, length)
        ic.setSelection(extracted.startOffset + start, extracted.startOffset + end)
        return true
    }

    /** Hold paste: copies the selected text. */
    private fun copySelection() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString().orEmpty()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select some text to copy it", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("", selected))
        // Android 13 and later shows its own "copied" confirmation.
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    /** The cog on the symbol pages: opens the keyboard's settings. */
    private fun openSettings() {
        commitWord()
        startActivity(Intent(this, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        requestHideSelf(0)
    }

    /** Remembers that the space just typed was added automatically (after a tapped suggestion). */
    private fun markAutoSpace() {
        autoSpace = true
        autoSpaceAt = SystemClock.uptimeMillis()
    }

    /** True if the last thing typed was an automatic space, a moment ago. Either way, forgets it. */
    private fun takeAutoSpace(): Boolean {
        val fresh = autoSpace && SystemClock.uptimeMillis() - autoSpaceAt <= AUTO_SPACE_MS
        autoSpace = false
        return fresh
    }

    /** Takes back the automatic space, if the cursor is still right after it. */
    private fun removeAutoSpace(): Boolean {
        val ic = currentInputConnection ?: return false
        if (ic.getTextBeforeCursor(1, 0)?.toString() != " ") return false
        ic.deleteSurroundingText(1, 0)
        return true
    }

    /**
     * Slide left from backspace: [steps] > 0 selects that many more characters to the left,
     * [steps] < 0 gives characters back. The selection is deleted when the finger lifts.
     */
    private fun slideSelect(steps: Int) {
        val ic = currentInputConnection ?: return
        if (steps > 0) {
            if (slideSelected == 0) commitWord() // finish any word in progress first
            spaceTapAt = 0L
            autoSpace = false
            sendShiftArrow(ic, KeyEvent.KEYCODE_DPAD_LEFT, steps)
            slideSelected += steps
        } else if (steps < 0) {
            repeat(-steps) {
                if (slideSelected > 0) {
                    // Never step right unless something is selected, so the selection cannot flip over.
                    if (ic.getSelectedText(0).isNullOrEmpty()) {
                        slideSelected = 0
                    } else {
                        sendShiftArrow(ic, KeyEvent.KEYCODE_DPAD_RIGHT, 1)
                        slideSelected--
                    }
                }
            }
        }
    }

    private fun endSlideSelect(delete: Boolean) {
        val ic = currentInputConnection
        val moved = slideSelected
        slideSelected = 0
        if (delete && ic != null && moved > 0 && !ic.getSelectedText(0).isNullOrEmpty()) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) // deletes the selection
            updateAutoCap()
        }
        refreshUi()
    }

    /** Shift + arrow key, [count] times: extends (or shrinks) the selection in the editor. */
    private fun sendShiftArrow(ic: InputConnection, code: Int, count: Int) {
        val now = SystemClock.uptimeMillis()
        val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        ic.beginBatchEdit()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, shift))
        repeat(count) {
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, shift))
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, shift))
        }
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0))
        ic.endBatchEdit()
    }

    /** The paste key: types whatever text is on the clipboard. */
    private fun pasteClipboard() {
        val ic = currentInputConnection ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = clipboard.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
        if (text.isEmpty()) return
        spaceTapAt = 0L
        autoSpace = false
        pulse()
        commitWord()
        ic.commitText(text, 1)
        if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        updateAutoCap()
        refreshUi()
    }

    /** Tapped a next-word suggestion: type it with a space after it, and learn the pair. */
    private fun acceptNextWord(index: Int) {
        val word = nextWords.getOrNull(index) ?: return
        val ic = currentInputConnection ?: return
        pulse()
        val prev = previousWordBefore(ic)
        ic.commitText(displayWord(word), 1)
        if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        if (learningEnabled && settings.learnWords) {
            engine.learn(word)
            if (prev != null) engine.learnPair(prev, word)
            scheduleSave()
        }
        insertSpace()
        markAutoSpace()
        updateAutoCap()
        refreshUi()
    }

    /**
     * Held a suggestion: forget it. While typing a word, that removes the word from the
     * suggestions for good; on a next-word suggestion it only forgets that word pair.
     */
    private fun removeSuggestion(index: Int) {
        if (session.isComposing) {
            val word = session.candidateList.getOrNull(index)?.word ?: return
            if (!engine.removeWord(word)) return
            session.restore(session.digitString) // look the keys up again, without the word
            showComposing()
            Toast.makeText(this, "Removed \"$word\"", Toast.LENGTH_SHORT).show()
        } else {
            val word = nextWords.getOrNull(index) ?: return
            val prev = previousWordBefore(currentInputConnection) ?: return
            if (!engine.forgetPair(prev, word)) return
            Toast.makeText(this, "Forgot \"$prev $word\"", Toast.LENGTH_SHORT).show()
        }
        scheduleSave()
        refreshUi()
    }

    /**
     * DEL right after a dictionary word re-opens it as the composing word, so the candidate
     * strip shows the alternatives. Pressing DEL again then removes keys as usual.
     */
    private fun tryReopenWord(ic: InputConnection): Boolean {
        if (!settings.reopenWord || fullKeyboard || emojiOpen || mode != InputMode.PREDICTIVE) return false
        if (!ic.getSelectedText(0).isNullOrEmpty()) return false
        val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: return false
        val word = before.takeLastWhile { it.isLetter() || it == '\'' }
        if (word.length < 2) return false
        val keys = KeypadMap.encode(word) ?: return false
        val candidates = engine.lookup(keys)
        if (candidates.none { !it.isCompletion && it.word.equals(word, ignoreCase = true) }) return false

        ic.deleteSurroundingText(word.length, 0)
        caseMode = when {
            word.length > 1 && word.all { it.isUpperCase() || it == '\'' } -> CaseMode.UPPER
            word[0].isUpperCase() -> CaseMode.CAP_NEXT
            else -> CaseMode.LOWER
        }
        session.previousWord = previousWordBefore(ic)
        session.restore(keys)
        val idx = session.candidateList.indexOfFirst { it.word.equals(word, ignoreCase = true) }
        if (idx >= 0) session.select(idx)
        showComposing()
        return true
    }

    // ---------------------------------------------------------------- composing / committing

    private fun showComposing() {
        val ic = currentInputConnection ?: return
        if (session.isComposing) {
            ic.setComposingText(displayWord(session.displayText), 1)
        } else {
            ic.setComposingText("", 1)
        }
    }

    /** Commits whatever is in progress: a predictive word and/or a pending multi-tap character. */
    private fun commitWord() {
        commitMultiTap()
        if (!session.isComposing) return
        val shown = displayWord(session.displayText)
        session.commit() // teaches the engine the dictionary form and the word pair
        currentInputConnection?.commitText(shown, 1)
        if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        scheduleSave()
    }

    private fun multiTapPress(key: Char) {
        val symbols = symbolsFor(key)
        if (mtKey == key) {
            mtIndex = (mtIndex + 1) % symbols.length
        } else {
            commitMultiTap()
            mtKey = key
            mtIndex = 0
        }
        val symbol = caseAdjust(symbols[mtIndex].toString())
        // Punctuation that took the automatic space back stays against the word; cycling on to
        // anything else (a bracket, a dash...) puts the space back in front of it.
        val text = if (mtDroppedSpace && symbol.firstOrNull()?.let { it !in AUTO_SPACE_PUNCTUATION } == true) " $symbol" else symbol
        currentInputConnection?.setComposingText(text, 1)
        uiHandler.removeCallbacks(mtTimeout)
        uiHandler.postDelayed(mtTimeout, settings.multiTapMs.toLong())
    }

    private fun commitMultiTap() {
        uiHandler.removeCallbacks(mtTimeout)
        val key = mtKey ?: return
        mtDroppedSpace = false
        currentInputConnection?.finishComposingText()
        mtKey = null
        mtIndex = 0
        if (key != '1' && caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        refreshUi()
    }

    /**
     * Inserts a space. A second space tapped quickly after the first one (right after a word)
     * turns the pair into ". ", in both layouts.
     */
    private fun insertSpace() {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        if (learningEnabled && settings.doubleSpace && spaceTapAt != 0L && now - spaceTapAt <= DOUBLE_SPACE_MS) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length == 2 && before[1] == ' ' && (before[0].isLetterOrDigit() || before[0] in ")]\"'")) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                spaceTapAt = 0L
                return
            }
        }
        ic.commitText(" ", 1)
        spaceTapAt = now
    }

    /**
     * Typing letter by letter (QWERTY / multi-tap): "dont" becomes "don't", "im" becomes "I'm",
     * and so on, when space or enter finishes the word. Capitalisation is kept.
     */
    private fun fixContraction() {
        if (!learningEnabled || !settings.fixContractions) return // not in password fields
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: return
        val raw = before.takeLastWhile { it.isLetter() }
        if (raw.length < 2) return
        val fixed = Contractions.FIXES[raw.lowercase()] ?: return
        var out = when {
            raw.all { it.isUpperCase() } -> fixed.uppercase()
            raw[0].isUpperCase() -> fixed.replaceFirstChar { it.uppercaseChar() }
            else -> fixed
        }
        if (fixed.startsWith("i'")) out = out.replaceFirstChar { it.uppercaseChar() }
        ic.deleteSurroundingText(raw.length, 0)
        ic.commitText(out, 1)
    }

    /** Words typed letter by letter (multi-tap or QWERTY) are added to the user dictionary. */
    private fun learnTrailingWord() {
        if (!learningEnabled || !settings.learnWords) return
        val before = currentInputConnection?.getTextBeforeCursor(60, 0)?.toString() ?: return
        val raw = before.takeLastWhile { it.isLetter() || it == '\'' }
        val word = raw.trim('\'').lowercase()
        if (word.length < 2) return
        engine.addUserWord(word)
        val prev = PREV_WORD.find(before.dropLast(raw.length))?.groupValues?.get(1)?.lowercase()
        if (prev != null) engine.learnPair(prev, word)
        scheduleSave()
    }

    private fun findNextWords(): List<String> {
        if (!settings.nextWord || digitsOnly || mtKey != null) return emptyList()
        val prev = previousWordBefore(currentInputConnection) ?: return emptyList()
        return engine.nextWords(prev)
    }

    private fun previousWordBefore(ic: InputConnection?): String? {
        val before = ic?.getTextBeforeCursor(40, 0)?.toString() ?: return null
        return PREV_WORD.find(before)?.groupValues?.get(1)?.lowercase()
    }

    private fun resetState() {
        uiHandler.removeCallbacks(mtTimeout)
        mtKey = null
        mtIndex = 0
        session.cancel()
    }

    private fun scheduleSave() {
        if (engine.changeCount == lastSaved) return
        uiHandler.removeCallbacks(saveTask)
        uiHandler.postDelayed(saveTask, SAVE_DELAY_MS)
    }

    private fun saveNow() {
        if (!::engine.isInitialized || engine.changeCount == lastSaved) return
        store.save(engine)
        lastSaved = engine.changeCount
    }

    // ---------------------------------------------------------------- case & text helpers

    private fun symbolsFor(key: Char): String =
        if (key == '1') ".,?!'-@1" else KeypadMap.keyLetters.getValue(key) + key

    private fun caseAdjust(text: String): String = when (caseMode) {
        CaseMode.LOWER -> text
        CaseMode.CAP_NEXT -> text.replaceFirstChar { it.uppercaseChar() }
        CaseMode.UPPER -> text.uppercase()
    }

    private fun displayWord(w: String): String {
        val base = if (w == "i" || w.startsWith("i'")) w.replaceFirstChar { it.uppercaseChar() } else w
        return caseAdjust(base)
    }

    private fun cursorWantsCaps(inputType: Int): Boolean {
        if ((inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        return (currentInputConnection?.getCursorCapsMode(inputType) ?: 0) != 0
    }

    private fun updateAutoCap() {
        if (!settings.autoCap || caseMode != CaseMode.LOWER) return
        val type = currentInputEditorInfo?.inputType ?: return
        if (cursorWantsCaps(type)) caseMode = CaseMode.CAP_NEXT
    }

    /** Flash the keyboard background in the accent colour (if the setting is on). */
    private fun pulse() {
        padView?.pulse()
        qwertyView?.pulse()
        emojiView?.pulse()
    }

    private fun refreshUi() {
        val caseLabel = when (caseMode) {
            CaseMode.LOWER -> "abc"
            CaseMode.CAP_NEXT -> "Abc"
            CaseMode.UPPER -> "ABC"
        }
        padView?.let { k ->
            // Letters on the keys show the case the next letter will have. While a predictive
            // word is being typed the case is already fixed, so a one-shot capital is not shown.
            k.setUpperCase(
                caseMode == CaseMode.UPPER ||
                    (caseMode == CaseMode.CAP_NEXT && !(mode == InputMode.PREDICTIVE && session.isComposing)),
            )
            if (mode == InputMode.MULTI_TAP) {
                nextWords = emptyList()
                k.setCandidates(emptyList(), -1)
            } else if (!session.isComposing) {
                // Nothing being typed: offer the words that usually come next.
                nextWords = findNextWords()
                k.setCandidates(nextWords.map { displayWord(it) }, -1)
            } else {
                nextWords = emptyList()
                val words = session.candidateList.map { displayWord(it.word) }
                k.setCandidates(
                    if (words.isEmpty()) listOf(displayWord(session.displayText)) else words,
                    session.selectedIndex,
                )
            }
            k.setModeLabel("$caseLabel \u00B7 " + if (mode == InputMode.PREDICTIVE) "predict" else "multi")
        }
        qwertyView?.setCase(caseLabel, caseMode != CaseMode.LOWER)
    }

    private companion object {
        val PREV_WORD = Regex("([A-Za-z']+) $")
        const val SAVE_DELAY_MS = 4000L
        const val DOUBLE_SPACE_MS = 500L

        /** How long after a tapped suggestion's automatic space punctuation may still take it back. */
        const val AUTO_SPACE_MS = 10_000L

        /** Punctuation that belongs right against the word before it. */
        const val AUTO_SPACE_PUNCTUATION = ".,!?;:"
    }
}

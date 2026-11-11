// app/src/main/kotlin/com/m8/keyboard/KeyboardService.kt
package com.m8.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import predictive.Contractions
import predictive.PredictiveEngine
import predictive.KeypadMap
import predictive.PredictiveSession

/**
 * The keyboard service. Two layouts: the number pad and a plain QWERTY keyboard.
 *
 * Predictive mode: digits go to [PredictiveSession] and the best word is shown as composing text.
 * Multi-tap mode: repeated presses cycle letters (then the digit) with a timeout.
 * Holding a number key types the digit and keeps cycling: hold 7, tap 4 more times for S.
 */
class KeyboardService : InputMethodService(), NumberPadView.Listener, QwertyKeypadView.Listener {

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
    private var shownView: View? = null

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
        caseMode = if (cursorWantsCaps(type)) CaseMode.CAP_NEXT else CaseMode.LOWER

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
        }
    }

    // ---------------------------------------------------------------- views & settings

    private fun currentView(): View {
        return if (fullKeyboard) {
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

    private fun switchLayout(full: Boolean) {
        commitWord()
        fullKeyboard = full
        userFull = full
        showView()
        refreshUi()
    }

    // ---------------------------------------------------------------- number pad events

    override fun onKeypadKey(key: Char) {
        if (digitsOnly) {
            currentInputConnection?.commitText(key.toString(), 1)
            return
        }
        if (key != '0') spaceTapAt = 0L
        when (key) {
            '1' -> {
                if (mtKey != '1') commitWord()
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

    override fun onKeypadEnter() = doEnter()

    override fun onKeypadCandidate(index: Int) {
        if (!session.isComposing) return
        spaceTapAt = 0L
        session.select(index)
        showComposing()
        refreshUi()
    }

    override fun onKeypadSymbol(symbol: String) {
        spaceTapAt = 0L
        commitWord()
        currentInputConnection?.commitText(symbol, 1)
        refreshUi()
    }

    override fun onKeypadQwerty() = switchLayout(true)

    // ---------------------------------------------------------------- QWERTY events

    override fun onQwertyText(text: String, isLetter: Boolean) {
        val ic = currentInputConnection ?: return
        spaceTapAt = 0L
        if (isLetter) {
            ic.commitText(caseAdjust(text), 1)
            if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
        } else {
            ic.commitText(text, 1)
        }
        refreshUi()
    }

    override fun onQwertySpace() {
        fixContraction()
        learnTrailingWord()
        insertSpace()
        updateAutoCap()
        refreshUi()
    }

    override fun onQwertyBackspace() = doBackspace()

    override fun onQwertyEnter() = doEnter()

    override fun onQwertyShift() {
        cycleCase()
        refreshUi()
    }

    override fun onQwertyPad() = switchLayout(false)

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
        when {
            mtKey != null -> {
                uiHandler.removeCallbacks(mtTimeout)
                ic.setComposingText("", 1)
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
     * DEL right after a dictionary word re-opens it as the composing word, so the candidate
     * strip shows the alternatives. Pressing DEL again then removes keys as usual.
     */
    private fun tryReopenWord(ic: InputConnection): Boolean {
        if (!settings.reopenWord || fullKeyboard || mode != InputMode.PREDICTIVE) return false
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
        currentInputConnection?.setComposingText(caseAdjust(symbols[mtIndex].toString()), 1)
        uiHandler.removeCallbacks(mtTimeout)
        uiHandler.postDelayed(mtTimeout, settings.multiTapMs.toLong())
    }

    private fun commitMultiTap() {
        uiHandler.removeCallbacks(mtTimeout)
        val key = mtKey ?: return
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
        if (learningEnabled && spaceTapAt != 0L && now - spaceTapAt <= DOUBLE_SPACE_MS) {
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
        if (!learningEnabled) return // not in password fields
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
        if (!learningEnabled) return
        val before = currentInputConnection?.getTextBeforeCursor(60, 0)?.toString() ?: return
        val raw = before.takeLastWhile { it.isLetter() || it == '\'' }
        val word = raw.trim('\'').lowercase()
        if (word.length < 2) return
        engine.addUserWord(word)
        val prev = PREV_WORD.find(before.dropLast(raw.length))?.groupValues?.get(1)?.lowercase()
        if (prev != null) engine.learnPair(prev, word)
        scheduleSave()
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
        if (caseMode != CaseMode.LOWER) return
        val type = currentInputEditorInfo?.inputType ?: return
        if (cursorWantsCaps(type)) caseMode = CaseMode.CAP_NEXT
    }

    private fun refreshUi() {
        val caseLabel = when (caseMode) {
            CaseMode.LOWER -> "abc"
            CaseMode.CAP_NEXT -> "Abc"
            CaseMode.UPPER -> "ABC"
        }
        padView?.let { k ->
            if (mode == InputMode.MULTI_TAP || !session.isComposing) {
                k.setCandidates(emptyList(), -1)
            } else {
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
    }
}

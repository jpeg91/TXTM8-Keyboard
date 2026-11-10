// app/src/main/kotlin/com/m8/keyboard/T9InputMethodService.kt
package com.m8.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import t9.T9Engine
import t9.T9Keymap
import t9.T9Session

/**
 * The keyboard service. Predictive mode: digits go to [T9Session] and the best word is
 * shown as composing text. Multi-tap mode: repeated presses cycle letters (800 ms timeout).
 */
class T9InputMethodService : InputMethodService(), T9KeypadView.Listener {

    private enum class CaseMode { LOWER, CAP_NEXT, UPPER }
    private enum class InputMode { PREDICTIVE, MULTI_TAP }

    private lateinit var engine: T9Engine
    private lateinit var session: T9Session
    private var keypad: T9KeypadView? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    private var caseMode = CaseMode.LOWER
    private var userMode = InputMode.PREDICTIVE
    private var forceMultiTap = false   // e.g. password fields
    private var digitsOnly = false      // number / phone fields
    private var learningEnabled = true

    // multi-tap state
    private var mtKey: Char? = null
    private var mtIndex = 0
    private val mtTimeout = Runnable { commitMultiTap() }

    private val mode: InputMode
        get() = if (forceMultiTap) InputMode.MULTI_TAP else userMode

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate() {
        super.onCreate()
        engine = T9Engine()
        try {
            assets.open("dictionaries/en.txt").use { engine.loadFrom(it) }
        } catch (e: Exception) {
            engine.build() // no dictionary packaged: start empty, user words still work
        }
        session = T9Session(engine)
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(mtTimeout)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val view = T9KeypadView(this, this)
        keypad = view
        refreshUi()
        return view
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        resetState()
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
        caseMode = if (cursorWantsCaps(type)) CaseMode.CAP_NEXT else CaseMode.LOWER
        refreshUi()
    }

    override fun onFinishInput() {
        commitWord()
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

    // ---------------------------------------------------------------- keypad events

    override fun onKeypadKey(key: Char) {
        if (digitsOnly) {
            currentInputConnection?.commitText(key.toString(), 1)
            return
        }
        when (key) {
            '1' -> {
                if (mtKey != '1') commitWord()
                multiTapPress('1')
            }
            in '2'..'9' -> onLetterKey(key)
            '0' -> onSpace()
            '*' -> onStar()
            '#' -> onHash()
        }
        refreshUi()
    }

    override fun onKeypadLongKey(key: Char) {
        if (key == '#') {
            commitWord()
            userMode = if (userMode == InputMode.PREDICTIVE) InputMode.MULTI_TAP else InputMode.PREDICTIVE
        } else if (key.isDigit()) {
            commitWord()
            currentInputConnection?.commitText(key.toString(), 1)
        }
        refreshUi()
    }

    override fun onKeypadBackspace() {
        val ic = currentInputConnection ?: return
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
            else -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        refreshUi()
    }

    override fun onKeypadEnter() {
        commitWord()
        if (mode == InputMode.MULTI_TAP) learnTrailingWord()
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

    override fun onKeypadCandidate(index: Int) {
        if (!session.isComposing) return
        session.select(index)
        showComposing()
        refreshUi()
    }

    // ---------------------------------------------------------------- key actions

    private fun onLetterKey(key: Char) {
        if (mode == InputMode.MULTI_TAP) {
            multiTapPress(key)
        } else {
            commitMultiTap() // a pending punctuation mark, if any
            session.pressDigit(key)
            showComposing()
        }
    }

    private fun onSpace() {
        commitWord()
        if (mode == InputMode.MULTI_TAP) learnTrailingWord()
        currentInputConnection?.commitText(" ", 1)
        updateAutoCap()
    }

    /** * = next candidate while composing; otherwise a plain asterisk (symbol picker comes later). */
    private fun onStar() {
        if (mode == InputMode.PREDICTIVE && session.isComposing) {
            session.next()
            showComposing()
        } else {
            commitWord()
            currentInputConnection?.commitText("*", 1)
        }
    }

    /** # = cycle abc -> Abc -> ABC. */
    private fun onHash() {
        caseMode = when (caseMode) {
            CaseMode.LOWER -> CaseMode.CAP_NEXT
            CaseMode.CAP_NEXT -> CaseMode.UPPER
            CaseMode.UPPER -> CaseMode.LOWER
        }
        if (session.isComposing) showComposing()
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
        session.commit() // teaches the engine the dictionary form
        currentInputConnection?.commitText(shown, 1)
        if (caseMode == CaseMode.CAP_NEXT) caseMode = CaseMode.LOWER
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
        uiHandler.postDelayed(mtTimeout, MULTI_TAP_TIMEOUT_MS)
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

    /** Words spelled in multi-tap mode are added to the user dictionary when finished. */
    private fun learnTrailingWord() {
        if (!learningEnabled) return
        val before = currentInputConnection?.getTextBeforeCursor(40, 0)?.toString() ?: return
        val word = before.takeLastWhile { it.isLetter() || it == '\'' }.trim('\'').lowercase()
        if (word.length >= 2) engine.addUserWord(word)
    }

    private fun resetState() {
        uiHandler.removeCallbacks(mtTimeout)
        mtKey = null
        mtIndex = 0
        session.cancel()
    }

    // ---------------------------------------------------------------- case & text helpers

    private fun symbolsFor(key: Char): String =
        if (key == '1') ".,?!'-@1" else T9Keymap.keyLetters.getValue(key) + key

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
        val k = keypad ?: return
        if (mode == InputMode.MULTI_TAP || !session.isComposing) {
            k.setCandidates(emptyList(), -1)
        } else {
            val words = session.candidateList.map { displayWord(it.word) }
            k.setCandidates(
                if (words.isEmpty()) listOf(displayWord(session.displayText)) else words,
                session.selectedIndex,
            )
        }
        val caseLabel = when (caseMode) {
            CaseMode.LOWER -> "abc"
            CaseMode.CAP_NEXT -> "Abc"
            CaseMode.UPPER -> "ABC"
        }
        val modeLabel = if (mode == InputMode.PREDICTIVE) "T9" else "multi"
        k.setModeLabel("$caseLabel · $modeLabel")
    }

    private companion object {
        const val MULTI_TAP_TIMEOUT_MS = 800L
    }
}

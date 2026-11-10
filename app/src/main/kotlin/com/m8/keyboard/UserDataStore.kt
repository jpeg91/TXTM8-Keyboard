// app/src/main/kotlin/com/m8/keyboard/UserDataStore.kt
package com.m8.keyboard

import android.content.Context
import java.io.File
import t9.T9Engine

/** Saves and restores what the engine has learned (boosted words, added words, word pairs). */
class UserDataStore(context: Context) {
    private val file = File(context.filesDir, "user_data.tsv")
    private val tmp = File(context.filesDir, "user_data.tsv.tmp")

    fun load(engine: T9Engine) {
        if (!file.exists()) return
        try {
            file.bufferedReader(Charsets.UTF_8).useLines { engine.importUserData(it) }
        } catch (e: Exception) {
            // A damaged file just means starting fresh.
        }
    }

    fun save(engine: T9Engine) {
        try {
            tmp.writeText(engine.exportUserData(), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            // Best effort: losing one save is not worth crashing the keyboard.
        }
    }

    fun clear() {
        file.delete()
        tmp.delete()
    }
}

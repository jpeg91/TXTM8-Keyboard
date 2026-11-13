// app/src/main/kotlin/com/m8/keyboard/EmojiData.kt
package com.m8.keyboard

import android.graphics.Paint

/** What the emoji page offers: emoji on the top row, old-school text smileys on the bottom row. */
object EmojiData {

    /**
     * The emoji, grouped loosely (faces, hands, hearts, people, animals, nature, food, things,
     * places, symbols). Anything this phone's font cannot draw is left out, so there are no blank boxes.
     */
    val emojis: List<String> by lazy {
        val paint = Paint()
        EMOJI.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && paint.hasGlyph(it) }.distinct()
    }

    val textFaces: List<String> = listOf(
        ":)", ";)", ":(", ":D", ":P", ";P", ":O", ":/", ":|", ":*", ":'(", "XD", "xD", "=)", "=D", "8)", "B)",
        ":-)", ";-)", ":-(", ":-D", ":-P", ":-O", ":-/", ":-|", ":-*", ":-3", ":-X", ":-#", "8-)",
        ">:(", ">:)", ">;)", "O:)", "3:)", "%)", ":S", ":3", ":]", ":[", "(:", "D:", "c:", ":^)",
        "<3", "</3", "^_^", "^^", "-_-", "o_O", "O_o", "T_T", ";_;", ">_<", "*_*", "@_@", "=^.^=", "orz",
        "\\o/", "\\m/", "(^_^)", "(>_<)", "<(^_^)>", "(*^_^*)", "(-_-)zzz",
        "\u00AF\\_(\u30C4)_/\u00AF", "(\u256F\u00B0\u25A1\u00B0)\u256F\uFE35 \u253B\u2501\u253B", "\u252C\u2500\u252C\u30CE(\u00BA_\u00BAO\u30CE)",
        "( \u0361\u00B0 \u035C\u0296 \u0361\u00B0)", "\u0295\u2022\u1D25\u2022\u0294", "(\u00AC\u203F\u00AC)", "(\u3065\uFF61\u25D5\u203F\u203F\u25D5\uFF61)\u3065",
        "(\u2022\u203F\u2022)", "(\u2299_\u2299)", "(\u2022_\u2022)", "( \u2022_\u2022)>\u2310\u25A0-\u25A0", "(\u2310\u25A0_\u25A0)",
    )

    private const val EMOJI = """
        😀 😃 😄 😁 😆 😅 😂 🤣 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🤩 🥳
        😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫
        🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩
        👻 💀 ☠️ 👽 👾 🤖 🎃 😺 😸 😹 😻 😼 😽 🙀 😿 😾
        👍 👎 👊 ✊ 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✌️ 🤞 🤟 🤘 👌 🤏 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤙 💪 🖕 ✍️ 🤳 💅
        ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 💋 💯 💢 💥 💫 💦 💨 💤
        👀 👅 👄 🧠 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵 🙋 🤦 🤷 🙆 🙅 💁 🙇 💃 🕺 👯 🏃 🚶 🧘 👑 🎩 🎓 👓 🕶️ 👔 👗 👟 👠 🎒
        🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌
        🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦀 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐘 🦒 🦓 🐪 🐕 🐈 🐓 🦃 🕊️ 🐇 🦔 🐾
        🌸 🌹 🌺 🌻 🌷 🌲 🌴 🌵 🍀 🍁 🍄 🌍 🌙 ⭐ 🌟 ✨ ⚡ 🔥 🌈 ☀️ ⛅ ☁️ 🌧️ ⛈️ ❄️ ☃️ 🌊 💧
        🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍒 🍑 🍍 🥭 🥑 🍅 🥕 🌽 🌶️ 🥔 🍞 🥐 🧀 🍳 🥞 🥓 🍔 🍟 🍕 🌭 🌮 🌯 🍝 🍜 🍣 🍤
        🍦 🍩 🍪 🎂 🍰 🍫 🍬 🍭 ☕ 🍵 🍺 🍻 🥂 🍷 🍸 🍹 🥤
        ⚽ 🏀 🏈 ⚾ 🎾 🏐 🎱 🏓 🎯 🎮 🕹️ 🎲 🎸 🎹 🎤 🎧 🎬 🎨 🏆 🥇 🥈 🥉 🎉 🎊 🎁 🎈
        📱 💻 📷 📺 🔋 💡 📚 ✏️ 📌 🔒 🔑 💰 💎 💣 🔔 📞 ✉️ 🚗 🚕 🚌 🚲 ✈️ 🚀 🚁 ⛵ 🚂 🏠 🏖️ 🗽 🎡
        ✅ ❌ ❓ ❗ ‼️ ⚠️ ➕ ➖ ✔️ ➡️ ⬅️ ⬆️ ⬇️ 🔴 🟠 🟡 🟢 🔵 🟣
    """
}

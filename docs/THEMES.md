<!-- docs/THEMES.md -->
# Theme system: guide for developers and coding agents

How themes work in TXTM8-Keyboard and how to add one (or a new kind of theme part) so it plugs into the code correctly, including as a source for the "Custom mix" theme. Read this before touching `KeyboardTheme.kt`, `ThemeParts.kt`, `ThemeComposer.kt`, `ThemeLayers.kt` or the theme code in `KeyDrawables.kt`, `ThemeEffects.kt` and `SetupActivity.kt`.

## Design principle: each theme is as good as it can be on its own

Design every theme as one complete, self-contained look and make it the best it can be, with colours, key style, background, animation and font chosen to suit each other. **Do not compromise a theme's look to make its parts interchangeable.** Whether a mix of parts from different themes looks good is the user's choice in the Customize screen, not a requirement on any theme. The rules below are about how the code is structured, so a theme's parts *can* be taken separately; they say nothing about how a theme should look.

## 1. The model in one minute

- `KeyboardTheme` is the **resolved** look. Views and drawables only ever read its fields; they never ask which theme it is.
- A theme is built from five **parts**. Every preset is a complete theme, and the "Custom mix" theme takes each part from a different preset.
- Presets are **static singletons** (`val NIGHT_SKY = KeyboardTheme(...)` in the companion object). P5 is prebuilt once per accent colour. The keyboard decides whether the theme changed by **identity** (`th !== kbTheme` in `KeyboardService.reloadSettings`), so a theme object must be the same object every time it is resolved.
- `KeyboardTheme.resolve(context, id, p5Accent, custom)` maps a saved id (`KeyboardSettings.THEME_*`) to a theme. For `THEME_CUSTOM` it resolves each source preset and calls `ThemeComposer.compose`, which builds each combination once and caches it (keyed on the identity of the source themes).
- Saved state: `KeyboardSettings.theme` (the id), `p5Accent`, and `custom: CustomParts` (the source id for each part, or `CustomParts.NONE` for the plain version of a part).

## 2. The five parts and the fields each owns

`ThemeComposer.build` is the authority; this table mirrors it. A field not listed under a part comes from the **Colours** part.

| Part | Fields |
|------|--------|
| **Colours** | `background`, `strip`, `key`, `keyPressed`, `text`, `subText`, `accent`, `accentText`, `actionKey`, `actionKeyPressed`, `actionText`, `actionKeyColours`, `actionTextColours`, `keyCycle`, `keyLabel`, `keySubLabel`, `stripText`, `border`, `shadow`, `deep`, `pixelBorder`, `rainbowLetters` |
| **Keys** | `keyStyle`, `squareKeys`, `keyRing`, `keyGloss`, `pixelColours`, `candidateStyle` |
| **Background** | `backdrop`, `stripStyle`, `backgroundBottom` (a gradient belongs to the background, not the palette) |
| **Animation** | `stars` + `starColours`, `tapEffect` + `effectColours`, `ambient` + `ambientColours` (drawn behind the keys), `overlay` + `overlayColours` (an animation drawn over the keys; give its colours low alpha so lettering stays readable), `cursorColour` |
| **Font** | `typeface`, `shoutLabels`, `candidateFont` |

Animations carry their own colours, so they look the same whichever palette they are mixed with.

## 3. Adding a preset theme

A new preset touches exactly three files.

1. **`KeyboardSettings.kt`**: add `const val THEME_YOUR_THEME = <next id>`. The last id used is `THEME_KERRANG = 93`, so the next is 94. Ids are stored in preferences: **never reuse or renumber one**. A theme that is removed keeps its constant, marked retired, and `migrateThemes` moves anyone on it to a replacement (`THEME_RETIRED_OASIS = 81` is one).
2. **`KeyboardTheme.kt`**: add `val YOUR_THEME = KeyboardTheme(...)` in the companion object, next to related themes, using **named arguments only**, then add `KeyboardSettings.THEME_YOUR_THEME -> YOUR_THEME` to `resolve`.
3. **`SetupActivity.kt`**: add `"Display name" to KeyboardSettings.THEME_YOUR_THEME` to the right `ThemeGroup` in `themeGroups` (titles: Essentials, Phones & gadgets, Game consoles, Video games, Music & TV, Film & TV, Retro computers, Web & media players, Twinkling stars, Tap animations). The Custom mix part lists, the "Try it" stepper and the section that opens for the current theme are all built from this list, so nothing else needs registering. Part lists show a theme only if it has something to offer that part, decided by `mixNote`; no per-theme code is needed.

Minimal example (a dark theme with square keys and twinkling stars):

```kotlin
val EXAMPLE = KeyboardTheme(
    background = 0xFF101820.toInt(),
    strip = 0xFF182430.toInt(),
    key = 0xFF22303F.toInt(),
    keyPressed = 0xFF3B5169.toInt(),
    text = 0xFFE6F0FA.toInt(),
    accent = 0xFF4FC3F7.toInt(),
    accentText = 0xFF00131F.toInt(),
    border = 0xFF4FC3F7.toInt(),
    squareKeys = true,
    stars = true,
    starColours = intArrayOf(0xFFE6F0FA.toInt(), 0xFFE6F0FA.toInt(), 0xFF4FC3F7.toInt()), // a repeated colour is more common
)
```

Set only what the theme needs; every other field has a default.

## 4. Rules for a theme that fits the system

**Keep the parts separate in code.**
- Every behaviour is chosen by an enum or a plain field on `KeyboardTheme`. **Never add a per-theme boolean** (`isSomething`) or test a theme id in a view or drawable: that is what the old `isGlitter`/`isGameBoy` flags were, and they tied parts together so they could not be used separately. If a look needs a new behaviour, add a part value (section 5).
- Never read `settings.theme` to decide how something looks. Read the resolved `KeyboardTheme`.
- The code must not crash or draw nothing for any combination of part values. How a combination *looks* is not the theme author's concern.

**Colours.**
- Choose the colours for the theme's own look. Nothing needs to be neutral, safe or contrast-checked against other themes' parts.
- Key styles use these colours from the palette: `GLOSS` border and `keyRing`; `LCD` border and `shadow` (as its edge); `MEMPHIS` border and `shadow`; `CHAMFER` border; `SLANTED` border and `shadow`; `NOKIA` `shadow`; `PIXEL` `shadow` and `pixelBorder` (0 uses `shadow`); `DEFAULT` outlines the key when `border != 0`; `BEVEL`, `PILL` and `RETRO` use only the key colours. The `STRIPES` backdrop uses `accent` and `deep`.
- The newer key styles (drawn in `KeyShapes.kt`) read: `BRACKET`, `RETICLE`, `SCANLINE`, `FRAME`, `DISH`, `PLATE`, `PETAL`, `ETCHED`, `MOULDED` and `BUBBLE` the `border`; `NEON` the `border` as its glow (`accent` when 0); `GLITCH` the `border` plus `accent` and the key sub-label colour for its two ghost outlines; `UNDERGLOW` the `accent` as its glow, with `border` as a rim; `BACKLIT` the key lettering colour as its halo (a lightened key colour on action keys), with `border` as a rim; `DOME` and `SLAB` the key colours, with an optional `border`. The game styles read: `RIVETED`, `WINDOW`, `PHOSPHOR` (`deep` as its bloom), `DECO` (`border` outer line, `accent` inner line), `CHECKER` (`border` and `shadow` as the two check colours), `TERRAIN` (`shadow` as grass, `border` as outline) and `BLOCK` the `border`; `STENCIL` uses `accent` for its spray (the key text colour on action keys); `PANEL` uses only the key colours. New ambients: `FIRE`, `PIPBOY`, `SUNSET`, `OVERWORLD`, `GREEN_HILL`. New tap effects: `POINTER`, `PLUMBOB`, `SPRAY`, `BLAST`, `RINGS`, `COINS`. New strip: `StripStyle.WINDOW`. The music and TV styles read: `SCRATCHED` (`border` for the scratches, `shadow` for the rust), `STITCH` (`border` as the thread), `CHROME` (`accent` as the LED), `TICKET` (`border` as the stripe), `CLAY` (`border` as the seam), `MENU` (`border` as the rules), `SPEECH` (`border` as the outline), `GOTHIC` (`border`). New ambients: `SPIRAL`, `TRACKS`, `CROSS`, `PEPPERMINT`, `COLOUR_CYCLE`, `CATHEDRAL`, `FEATHERS`, `DOGS`, `CLAYMATION`, and `HALFTONE` and `TURF` (both still); `TRACKS`, `CROSS`, `FEATHERS` and `DOGS` are meant for `overlay`. New tap effects: `STARBURST`, `TRACK` and `CLAY` (and `SPRAY` now throws one colour per tap); their colour lists are described in `MusicEffects.kt`. Each is drawn from the key colour it is given (so action keys and pressed keys work), and `keyBackground` wraps them so the wide end keys of an indented QWERTY row stay centred.
- A **preset** must set the colours its own key style and backdrop need, because presets are never derived. Only when a mix takes keys or a backdrop from a different theme does `ThemeComposer` fill a missing `border`, `shadow` or `deep` (outline from `text`, shadow from a darkened `key`, `deep` from a darkened `accent`) and a gradient's bottom colour, so the result is drawn rather than blank. That is a fallback for mixes, not a goal for presets. If you add a key style or backdrop that needs a colour, add it to the composer's `OUTLINED` / `SHADOWED` sets or give it a similar rule.
- What each animation colour list (`starColours`, `effectColours`, `ambientColours`, `cursorColour`) means depends on the animation: read the animation's KDoc and drawing code, and copy the list shape from an existing preset that uses it. Pick the colours that look best in your theme.

**Animations are independent slots** (`ThemeLayers`).
- Behind the keys: the background (plus the `ambient` animation) and the typing pulse. Over the keys: the `overlay` animation, `stars`, `tapEffect` and the cursor. Any combination can be on at once; with more than one over-the-keys slot, `EffectStack` draws them together.
- The typing pulse is drawn in `pulseColour` (a Colours-part field; 0 means the `accent`).
- The typing pulse is on for every theme and follows the pulse settings. Settings controls appear per slot: `stars` shows Star density, `tapEffect` shows Effect amount, and the pulse controls are always shown.
- `ambient` follows the Effect amount setting only when the theme also has a `tapEffect` (`ambientFollowsEffectAmount`). Otherwise it is always on.

**Identity and caching.**
- Define every theme once, in the companion object, as a `val`. Never construct a `KeyboardTheme` inside `resolve` or a view. A theme with a parameter (P5's accent) is prebuilt for every parameter value and looked up.
- `ThemeComposer` already caches mixes by source identity. Source themes must therefore be stable singletons, which is the same rule.

## 5. Adding a new part value

Add the enum entry, then find every `when` over that enum (search for its name; the compiler rejects non-exhaustive `when`s on enums) and handle the new value.

| New value | Where |
|-----------|-------|
| `KeyStyle` entry (`ThemeEffects.kt`) | a drawable (extend `KeyShapeDrawable` in `KeyShapes.kt`) and the `when (keyStyle)` in `KeyboardTheme.keyBackground` (`KeyDrawables.kt`); a mapping in `candidateShape` (`KeyboardTheme.kt`; `SQUARE_STYLES` there lists the styles with square corners), plus a new `CandidateStyle` and its branch in `candidateBackground` if it needs its own highlight; the `when` in `SetupActivity.mixNote`; the composer's `OUTLINED`/`SHADOWED` sets if it uses `border`/`shadow` |
| `Backdrop` entry (`ThemeParts.kt`) | `flatBackgroundDrawable` (`KeyDrawables.kt`); `mixNote`; a colour rule in `ThemeComposer.build` if it needs a colour the palette may lack |
| `StripStyle` entry | `stripDrawable` (`KeyDrawables.kt`); `mixNote` |
| `CandidateStyle` entry | `candidateBackground` (`KeyDrawables.kt`) and, if it should be the default for some key style, `candidateShape` |
| `CandidateFont` entry | where `NumberPadView` builds the candidate views |
| `AmbientKind` entry | its frame period and its draw branch in `ThemeEffects.kt` (both are `when (kind)`) |
| `TapEffectKind` entry | the `when (kind)` sites in `TapEffectDrawable` (`KeyDrawables.kt`) |
| A new field on `KeyboardTheme` | give it a default, decide which part owns it, copy it in `ThemeComposer.build` (with that part's plain value when the part is `NONE`), and add it to the table in section 2 |

`SetupActivity.mixNote` decides which presets a part list offers and what note follows the name; extend it when a new value should be discoverable. When a new field is added, the constructor check is simple: every constructor parameter of `KeyboardTheme` must appear in `ThemeComposer.build`'s `KeyboardTheme(...)` call, or a mix silently drops it.

## 6. Conventions

- Every code file starts with a line-one filename comment (for example `// app/src/main/kotlin/com/m8/keyboard/ThemeComposer.kt`), above the `package` line. Keep it on every edit.
- Never write the trademarked name of the predictive text feature in code, comments, file names, labels or UI. Say "predictive".
- Colours are `0xFFRRGGBB.toInt()`. KDoc every new field and enum entry, in the style of the existing ones.
- Do not edit `README.md` for theme changes unless asked.

## 7. Checking a new theme or part value

1. It builds (CI) with no new warnings about non-exhaustive `when`s.
2. As a plain theme: keys, candidate strip (selected and unselected word), action keys, the settings screens (which use the theme as their own look), and every keyboard (number pad, QWERTY, emoji).
3. As a Custom mix source for each part it offers: choosing it must work (no crash, nothing blank or missing). Do not tune the theme for how it looks next to other themes' parts.
4. Selecting the theme, leaving, and returning does not rebuild the views (same object from `resolve`; see section 1).
5. The right settings controls show for its animations (section 4), and the theme appears in the right picker group and in the Custom mix part lists it should.

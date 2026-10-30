# ASCENT

**[English](README.md)** · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Русский](README.ru.md) · [Svenska](README.sv.md)

An Android app for people starting out in high-altitude mountaineering and ice climbing. The curriculum is organised by altitude: every lesson you pass moves you a little higher.

Two course tracks switch freely. **Alpine climbing** runs six units and 68 lessons, from a 5,000 m introduction to 8,000 m — 3,200 m to 8,848 m end to end. **Trekking** runs four units and 21 lessons, from day hikes to long trails — 200 m to 5,630 m. Each track keeps its own progress, badges and review queue.

Current version v0.21.1 (versionCode 130). The language work closed with v0.19.24 — all nine translated languages match English key for key (10,554 / 10,554). v0.20 is about the badges: all forty achievements render as real-time 3D relics instead of flat images, and their badge icons are rendered from the same models — the old AI illustrations are gone. The unlock dialog uses the same renderer and bursts confetti from both screen edges. Models ship inside the APK, so the package grew from 21 MB to about 41 MB. v0.20.3 fixes one long-standing annoyance: scroll down a list, open a secondary screen, come back, and the list used to jump to the top — it now stays where you left it. v0.20.6–v0.20.7 raise badge rendering to full quality by default: a finer bloom that no longer switches itself off during the first slow frames, a two-step downgrade, and 4x MSAA with a capability check and a silent fallback.

v0.20.19 – v0.21.0 restyled the whole interface in clay: soft double shadows drawn offscreen and cached, flat colour chips, category colours in the handbook, and hero blocks that take their depth from a contact shadow instead of a gradient.

## Install

The easiest route is the prebuilt debug APK from [Releases](https://github.com/Solar-CV/AscentApp/releases). Pushing a `v*` tag makes GitHub Actions build and attach a fresh APK.

- Package: `com.ascent.climb`
- Minimum: Android 8.0 (API 26), **arm64-v8a only** — 32-bit-only phones cannot install this build
- Size: about 41 MB (38 MB of uncompressed badge models, ~11 MB after packaging)
- Permissions: network and network state — used to check for question-bank updates and to send feedback. Everything works offline; the bundled bank is always there

Transfer the APK to your phone, allow installs from unknown sources, tap install. No login on first launch.

## What's inside

### The path

The home screen is a route from base camp upwards, split into altitude units (six for climbing, four for trekking). Swipe horizontally to page through them, two units per page. Lessons unlock linearly — pass one and the next opens. Nodes show stars, the current lesson carries a "start" hint, and the last lesson of a unit is marked in gold.

Two pills at the top switch course tracks; tapping one opens a bottom sheet. Switching returns to the first page and the choice is remembered.

Progress is not a stored number. It is recomputed from the lessons you have completed, so if the altitude curve is adjusted later, old saves recalculate instead of getting stuck.

### Question bank

1,533 formal questions plus 154 auto-inserted review copies, 1,687 display items in total. Volume rises with altitude: about 15 questions per lesson in the 5,000 m introduction, 30 in the 7,000 m technical units, and 8 across trekking.

Eight question types: single choice, multiple choice, ordering, matching, fill-in, calculation, hotspot (tap a point on an image), and scenario (one stem answered in several steps).

Finishing a lesson drops you straight into redoing the ones you got wrong. Questions you then answer correctly gain credit; the rest go to the review centre. Review uses spaced repetition at 0, 1, 3, 7, 16 and 35 days. Safety-critical questions are weighted, and require confirming you understand before you can move on.

### Handbook

35 long-form articles, 4 to 8 sections each, sorted into gear, technique, medicine, health, weather, meteorology, decisions, judgement, safety, risk and avalanche rescue. The handbook is now filtered by track: snow-and-ice pieces appear only under climbing, while altitude medicine, hypothermia, navigation, camping and leadership are shared by both. Sources are public medical and technical material, three commercial guide documents, and edited transcripts of seven Chinese mountaineering books. Per product agreement, the Chinese books are not cited by title; the guide documents are.

### Training, review, profile

Training gives a readiness percentage and the target peak's altitude. Review puts today's due questions and safety-critical misses first. Profile holds the header, current altitude, ability overview, badge entry, account area and a feedback entry. The gear icon opens settings, the avatar opens the profile page.

The profile shows unlocked badges only. All 40 live on the badge wall, locked ones as grey previews, hidden achievements as a "?".

Badges are real 3D objects — metal frames, enamel insets, relief. Tap any unlocked badge on the profile page or on the wall and it opens full screen: drag to rotate, pinch to zoom up to about 5.5×, two fingers to pan across the relief, double-tap to reset, and it sways gently on its own when you stop. Icons are rendered from the models themselves, so the wall and the 3D view show the same object; all forty now have a model, and the still-art fallback stays for any badge added later. Unlocking an achievement anywhere shows the same 3D badge, on a light pedestal, with ribbons firing in from both edges.

### Settings

Appearance follows the system or can be pinned to light or dark, persisted, with the status bar matching. Language follows the system or can be set manually; interface and content both switch, no restart. The bank section shows the current version, checks for updates and restores the bundled bank. Developer mode hides behind the version number in About — tap it seven times.

### Feedback

Profile → Feedback and suggestions, sitting between the account card and the safety notice. Pick a type — bug, idea, content correction — write the message, optionally leave an address for a reply, and optionally attach diagnostics. Sending posts straight to the maintainer through Formspree; if the network is down the screen offers to send the same text by email instead, and copies it to the clipboard if no mail app exists.

Diagnostics are off by default and cover the app version, Android version, device model, interface language, bank version, current track, progress counts and the last uncaught exception. No lesson text, no answer history, no account, no clipboard.

## Ten languages

Interface strings and course content are available in:

简体中文 (source), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

All ten languages are complete. Every language has its own entry layer, lesson briefs, handbook body and question bank, and each key set matches the English one entry for entry — **10,554 / 10,554**, with nothing missing, nothing duplicated and no orphan keys.

| Layer | Items | All 9 translated languages |
|---|---|---|
| Entry (lesson, unit, handbook titles, peaks) | 117 | 100% |
| Lesson key points | 443 | 100% |
| Handbook body | 378 | 100% |
| Question bank (stem / options / explanation) | 9,422 | 100% |

Terminology follows each language's own mountaineering usage rather than literal translation: Swedish stegjärn / isyxa / standplats, German Steigeisen / Pickel, Russian кошки / ледоруб, Italian corda fissa / bloccante / sosta. Medical doses, avalanche levels, slope angles and temperatures are carried over unchanged — only the number format is adapted, and the three conventions are kept apart on purpose:

- English, Chinese and Japanese: `6.5 °C`, `5,000 m`
- German, Spanish and Italian: `6,5 °C`, `1.000 m` (decimal comma, thousands point)
- French and Swedish: `6,5 °C`, `5 000 m` (decimal comma, thousands space)

The learner-facing voice also differs by language: German uses the impersonal *man*-form, Spanish addresses the reader as *tú*, French as *vous*, Swedish and Italian as *du* / *tu*, while lesson briefs stay impersonal in all of them.

If an item ever is missing in the selected language, the fallback order is: selected language → English → Chinese source. That path is now only a safety net.

## Curriculum

**Alpine climbing**

| Unit | Altitude | Lessons | Questions | Per lesson |
|---|---|---|---|---|
| 5000M · Introduction | 5,000 m | 16 | 249 | 16 |
| 5000M · Progression | 5,300 m | 12 | 221 | 18 |
| 6000M | 6,000 m | 14 | 316 | 22 |
| 7000M · C3 technical camp | 7,000 m | 6 | 185 | 31 |
| 7000M · C4 high camp | 7,500 m | 10 | 279 | 28 |
| 8000M | 8,000 m | 10 | 248 | 24 |

**Trekking**

| Unit | Altitude | Lessons | Questions | Per lesson |
|---|---|---|---|---|
| Day hikes | 1,200 m | 5 | 40 | 8 |
| Multi-day and camping | 2,500 m | 8 | 79 | 10 |
| High-altitude trekking | 3,800 m | 5 | 44 | 8 |
| Long trails and emergency | 4,500 m | 3 | 26 | 8 |

38 target peaks and routes in total: 30 for climbing, from Aotaina at 5,211 m up to the fourteen 8,000 m peaks; 8 for trekking, from the Huihang Trail to the Three Passes route at Everest.

## Where the content comes from

Two main streams feed the questions and the handbook.

Public sources include the Wilderness Medical Society's 2024 update of the clinical practice guidelines for acute altitude illness, the CDC Yellow Book, technical material from the American Alpine Institute and the American Alpine Club, REI Expert Advice, UIAA and CE equipment standards, and avalanche data from avalanche.org.

The three SEVEN SUMMIT TREKS guide documents — altitude illness, equipment, general expedition practice — are the second stream.

Later, proofed transcripts of seven Chinese mountaineering books were edited into the handbook. Knowledge and cases are rewritten in our own words, with page references; the scanned source text is not kept in the repository.

| Source | Used for |
|---|---|
| WMS / CDC / StatPearls | Altitude medicine questions and handbook |
| AAI / AAC / REI / BMC | Snow, ice and movement technique |
| UIAA / CE standards | Equipment questions |
| avalanche.org | Avalanche judgement |
| SEVEN SUMMIT TREKS documents | Handbook h17–h22 and related questions |
| 7 Chinese mountaineering books | Handbook h23–h31 |

## Safety boundary

The app covers theory and cognitive preparation only. Practical skills — snow and ice, rope work, ice climbing, rescue — must be taught and assessed on site by a certified instructor.

Nothing here is medical advice. Decisions about medication and first aid belong with a doctor.

No summit is promised and no acclimatisation outcome is guaranteed.

Lessons marked as needing an instructor say so explicitly once the theory is done.

## Building it yourself

The whole toolchain can live in your home directory — no Homebrew, no Android Studio. What this machine uses:

- `~/android-toolchain/jdk`: Temurin OpenJDK 17
- `~/android-toolchain/sdk`: Android SDK with platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9`: Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

The repo also ships the Gradle 8.9 wrapper, so a machine without Gradle works too:

```bash
cd AscentApp
./gradlew assembleDebug
```

Output lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
AscentApp/
├── import/                      Question authoring (Markdown, from print books)
├── knowledge/                   Print-source knowledge base
├── tools/
│   ├── contentcheck/            Content checks + bank export (plain JVM)
│   ├── i18ncheck.py             Per-language coverage across all four layers
│   ├── qbank/build.mjs          Markdown → question bank compiler
│   └── poc3d/                   Model sync, icon cut-out, emissive baking
└── app/src/main/
    ├── res/values*/             Interface strings for 10 languages
    ├── assets/
    │   ├── badges/*.glb         40 badge models
    │   └── env/studio_ibl.ktx   Procedurally generated IBL environment
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt       Main lesson content
        │   ├── NewLessons.kt    Newer curriculum lessons
        │   ├── i18n/Pack*.kt    Content translations (entry / brief / handbook / quiz)
        │   ├── Diagnostics.kt   Crash log + environment snapshot for the feedback form
        │   ├── FeedbackSender.kt  Formspree POST (HttpURLConnection, no extra deps)
        │   ├── BundledBank.kt   Lesson pool assembly and review insertion
        │   └── bank/            Bank config, JSON decoding, network, repository
        └── ui/                  Screens and components (Badge3D.kt = Filament scene, framing, lights)
```

## Changing content

To add questions to an existing lesson, edit `data/Expansion*.kt`. For a new lesson, write `Lesson(...)` in `data/NewLessons.kt` and add its id to the right unit in `Curriculum.kt`. Ordering and unit membership live only in `Curriculum.kt`.

Then run the checks:

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

The first script syncs the app's content into a plain JVM project, runs the real assembly logic, and checks question counts, difficulty progression, out-of-range answers, empty questions, unlock logic and achievement definitions, then exports `bank/bank.json` and `bank/manifest.json`. The second reports coverage per language across the four layers and diffs the keys. Neither needs an emulator.

After touching the bank, bump `BUNDLED_VERSION` in `data/bank/BankConfig.kt`, otherwise clients will think nothing has changed.

## Online bank updates

The bank has three layers: local cache, bundled, and an online delta. Lookup order is cache → bundled; the online copy only overwrites the cache when it is newer.

So a plane, a valley with no signal, a dead server, malformed JSON, a sha256 mismatch or an older version all leave things as they are. Updates are checked silently after launch, at most every six hours, and only once a URL is configured.

To publish a new bank: change content → bump `BUNDLED_VERSION` → run `contentcheck` → upload `bank/bank.json` and `bank/manifest.json` to a server or CDN → the app picks it up next launch, no release needed.

Older app versions are not broken by a newer bank. `minAppVersion` in `manifest.json` blocks clients that are too old, unknown fields are ignored, and a malformed question is skipped rather than fatal.

## Recent changes

**v0.21.1: three fixes**

- The handbook was empty in every language except the two Chinese ones. The "All" filter stored its localised label as the filter state, so after switching language the stored value matched neither the new label nor any category, and all 35 articles were filtered out
- The bottom bar's labels were clipped in French and Spanish on narrow screens: the selected pill was almost a circle, and a 13-character word overflowed it. The indicator is now a rounded rectangle, and the label size is fitted to the widest tab so all five stay the same
- The bar's own corner radius no longer clips the first and last selected items — the outer radius now stays within the inner inset plus the indicator's radius

**v0.20.19 – v0.21.0: clay surfaces**

- Cards are clay now: each one carries four layers — outer shadow, edge light, a short top highlight and an inner shade — with the shadow generated by `BlurMaskFilter` offscreen and cached per size and radius. `Modifier.blur()` needs API 31 and this project runs on 26; `Modifier.shadow()` cannot do a coloured two-layer shadow
- Five screens restyled: profile home, review, training and the handbook, with My page following. The highlight was pulled back into a 20 dp lip, the outline moved from 1 physical pixel to dp, and the shadow split into volume plus contact — until then the cards read as semi-transparent, because the highlight was washing out their edges
- Handbook categories each carry a colour (steel blue gear, teal ropes, indigo snow and ice, red medicine, amber judgement, green planning, magenta leadership); the chips are flat fills with no gradient, and the detail page cover takes the category colour
- Several achievements unlocking at once no longer ghost onto each other: the 3D view is rebuilt per achievement id, and the backdrop stays opaque between dialogs

**v0.20.18: a re-exported Everest diorama**

- The Climbing Grand Slam model was rebuilt around its details: prayer flags grew to 0.42 × 0.26 mm (they were invisible on a phone), their cords are lifted onto twin cairn stones and every flag is checked against the snow so none sinks in, and small parts sample the terrain mesh directly instead of ray-casting the whole scene
- The icon comes from the new hero render; pose and framing are unchanged

**v0.20.17: no more accidental spin**

- Lifting one finger after a pinch used to be read as a one-finger drag — with the tracking point a whole hand-width away, so the badge jumped and carried the jump into its momentum. The gesture is now an explicit state machine: once a two-finger gesture starts, the remaining finger is ignored until you let go, and a pinch leaves no momentum
- Nothing auto-sways while you are zoomed in, and returning to the front face is now a smooth sweep instead of a jump

**v0.20.16: zoom into the details**

- Pinch used to top out at 1.25× magnification, which is why peak names and altitudes stayed unreadable on a phone. It now goes to 5.5×
- With two fingers held at a constant distance, dragging pans the view so you can reach the corners; single-finger drag still rotates. Double-tap resets zoom and pan, and a short gesture hint sits under the badge title

**v0.20.15: all forty badges are 3D**

- The last six land: Eight K Grad (a mortarboard), Final Exam (a summit medal with a flag), Five Zones (a running-sole plate), Fourteen Peaks (a 2.5:1 panorama of all fourteen 8,000ers), Two Tracks (a two-rail tray) and Full Route (a Mount Everest terrain block — the only piece in the set that is not a medal)
- Every badge icon is now the model's own render, so the hand-drawn backups are gone from the wall
- Full Route needs its own pose: it stands on its base, so the 90° flip used for flat medals is skipped and the plaque is turned to face the camera; its render sits on grey rather than black, so the icon is knocked out against that grey
- Models 34 → 40; package 39.4 → 40.6 MB

**v0.20.14: badges open anywhere**

- Badges are no longer reachable only from the developer test area: tap any unlocked badge on My profile or on the badge wall and it opens full screen. The view is laid over the app rather than replacing the page, so going back returns you where you were — list position included — and the bottom bar stays out of the way
- Trail Finish was still showing the shared placeholder icon even though its model had shipped; the mapping now points at the rendered icon
- Two Tracks had a photograph of a person as its icon — a leftover placeholder. It now uses a drawn plate (navy enamel, brass rim, two dashed routes meeting at a peak), and the photo is gone from the package

**v0.20.13: six more badge models**

- The library went from 28 to 34: 6,000 m of learning, comeback, seven-day and three-week footprints, twenty runs, and a D-shaped carabiner for safety first; self_arrest re-exported
- The icon knockout now also clears background *enclosed* by the badge shape — the carabiner's centre hole and a punched card's perforations used to come out as solid black discs. Each enclosed region is judged on its own: a uniform colour matching the backdrop counts as a hole, gradients stay. Package 38.5 → 39.4 MB

**v0.20.11: badge stage lighting and backdrop**

- The badge view used two lights, so the metal only ever had one curved highlight and the relief sat flat. It now has five directional lights — key, top, front fill, rim and a weak bounce from below — with the total illumination kept where it was, so the metal gains depth without washing out
- A second, darker backdrop (a synthwave sunset blurred down to colour fields) suits dark mode. The pill at the top right switches between light and dark, defaults to the system setting, and remembers your choice; only this screen is affected

**v0.20.8 – v0.20.10: launch screen**

- The launch screen is now clay: a white puck with tight, crisp-edged shading, the single-peak mark, and one random quotation from a mountaineer — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe or Bonatti — in place of the old tagline. It fades out after about 1.7 s, and is all but skipped when reduced motion is on. Nothing else in the app changed In v0.20.9 the system splash shows no logo at all any more — the launch theme is just the same background colour, so the clay screen starts on a blank white (or near-black) field. Each line is quoted in its original language

**v0.20.6 – v0.20.7: badge rendering quality**

- Bloom runs at high quality by default — 256 px, four levels, MEDIUM. The old auto-downgrade judged from the very first frame, so a few hundred milliseconds of shader compilation was enough to switch bloom off for the rest of the session. It now waits 2.5 s, tolerates 50 ms frames (20 fps) across three consecutive windows, and steps full → low → off instead of cutting it outright. The debug string shows which stage is active
- 4x MSAA is on where the driver supports it, checked before the swap chain is built; a failed attempt falls back to no MSAA and is remembered, so a badge never fails to draw because of an antialiasing flag

**v0.20.4 – v0.20.5: small fixes**

- Developer mode is now invisible until unlocked — the whole settings section, including "off · tap the version number 7 times", is gone for ordinary users. The 7-tap counter still works, it just no longer counts down out loud
- Scroll position survives inside the achievement test area too: opening a badge's 3D stage and coming back, and stepping from settings into the test area and back
- The unlock popup no longer closes on a stray tap: the first tap on the backdrop only arms it and fades in "tap again to close"; the second tap dismisses. Accept and the back gesture still close it in one step

**v0.20.0 – v0.20.3: 3D badges**

- Thirty-four achievements render as real-time 3D (Filament 1.75, glTF, PBR metal and enamel). Drag to rotate, pinch to zoom, gentle sway when idle; the automatic motion stays on the front face because the back of a medal is a plain plate
- All forty have models as of v0.20.15; the tilted-card fallback stays for any badge added before its model exists
- Unlock and preview dialogs go through the same renderer, with a light pedestal behind the model (a dark model on a dark card disappears) and a 56-ribbon burst from both edges. Reduce-motion disables the ribbons
- Display page sits on a heavily blurred landscape, generated procedurally (27 KB) — sky, four ridges, mist, no photo licensing to worry about
- Drag directions inverted per feedback; the whole scene brightened, then pulled back slightly so emissive parts keep their contrast
- Lights authored inside the models now survive: `KHR_lights_punctual` entities are added to the scene explicitly (glTF asset entities do not include them) and their intensity is scaled to the scene exposure — 54 cd next to a 66,000 lux key light is invisible otherwise. `KHR_materials_emissive_strength` is baked into `emissiveFactor` at build time, and bloom is on, auto-disabled when frame time stays above 28 ms
- The model library grew from 17 to 34 in this stretch: streak, perfect, review, retreat, trail-finish, altitude, comeback, weekly footprints, runs and a carabiner joined, plus several re-exports; package 21 → 39 MB
- **Badge icons now come from the models**: each badge's front render is knocked out of its black backdrop into a transparent 256 px icon, so the wall, the unlock dialog and the 3D view all show the same object. The AI-generated illustrations for those badges were dropped
- Spotlight gain simplified to 5× (the model's own spot light aims into the medal body, so no gain makes it visible from the front — that needs a fix in the Blender source), bloom dialled down to 0.05
- v0.20.3: list scroll position now survives a trip into a secondary screen (settings → language, handbook → article, profile → settings, review → lesson), and returning from a lesson no longer resets the path pager to page one

**v0.19.25 – v0.19.30: app work**

- Achievements realigned with the 89-lesson bank. `full_route` now means all 68 climbing lessons — it used to say 66 and judged against whichever track was open, so finishing trekking alone earned "theoretical grand slam". `five_zones` became six-zone, matching the six climbing units. Five trekking achievements added, 35 → **40**
- Achievement progress is computed across both tracks instead of the selected one, so switching tracks no longer moves the goalposts
- Track icons redrawn as solid silhouettes — a snow-capped peak and a hiking boot — replacing the `▲` / `≡` text glyphs in the sheet; the top pills carry the same two at 14 dp
- Splash logo scaled down 40 %
- Feedback screen under Profile, posting to Formspree with an email and clipboard fallback; crash handler keeps the last five uncaught exceptions in app-private storage
- Profile and home route progress counted the completion total against the current track's lesson count, showing "68 / 21" after switching tracks; fixed

v0.19 is the content expansion; the bank structure and scoring logic are unchanged.

- Lessons 47 → 89: climbing now 68 (6000 / 7000 / 8000 m units, all fourteen 8000ers), plus 21 new trekking lessons
- Questions 711 → 1,533 (9,422 keys), handbook 31 → 35 articles (avalanche burial and resuscitation, the avalanche trio, fixed ropes, river crossing), bank v15 → v19
- **Nine languages now complete across all four content layers**: English, Traditional Chinese, Japanese, Russian, German, Spanish, French, Swedish and Italian. Their key sets match English entry for entry (10,554 / 10,554), so nothing falls back at runtime
- German uses the impersonal man-form, Spanish addresses the reader as tú, French as vous, Swedish and Italian as du / tu; each follows local number conventions — German, Spanish and Italian write 1.000 m, French and Swedish 5 000 m, all of them use a decimal comma
- Leftover orphaned keys from earlier content edits (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) were removed from all nine languages, leaving every key set clean and identical
- Traditional Chinese runs through OpenCC s2twp plus a terminology table, validated by regression against the existing translations (zero differences); Japanese is a real translation, with one register for the question bank and another for the handbook
- `tools/i18n/` provides three commands (`status.py` / `prep.py` / `apply.py`) that work across all nine languages; Traditional Chinese also has `tw.py` and `tw_entry.py`
- The bank is split across files: a single Kotlin file of roughly 2,500 entries crashes the compiler, so each of the nine translated languages is split into three parts (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

v0.16 through v0.18 were all multilingual work; the bank structure and scoring logic did not change.

- Interface strings split into 10 `res/values*` sets; language can be set manually or follow the system, taking effect immediately
- New `data/i18n/` content layer with four packs per language: entry, key points, handbook body, question bank. 4,727 items per language
- All four layers complete in the nine translated languages (zh-TW, en, ja, de, fr, es, it, ru, sv); the eight beyond English were verified key by key against the English source, zero missing
- Terminology aligned with each language's existing usage; doses, avalanche levels, slope angles and temperatures kept as-is
- Added `tools/i18ncheck.py` so one command shows coverage across all four layers — no more "translated, but one lesson was missed"
- Fixed a batch of copy issues along the way: a comma-before-dash punctuation error (140 in Swedish, 5 each in German and Spanish), two fill-in answers with a stray quote mark, and three French decimals

Earlier:

- v0.15.1: in-app copy clean-up, removed literal `**` markers, rewrote a batch of explanations, login page no longer promises unimplemented sync
- v0.15.0: nine more handbook articles from Chinese books (h23–h31), new "outdoor leadership" category, 22 → 31 articles
- v0.14.0: six SEVEN SUMMIT TREKS guide articles
- v0.13.x: settings, profile page, badge wall; back gesture on secondary screens

## Licence

PolyForm Noncommercial License 1.0.0 — noncommercial use only. Commercial use requires separate written permission; see `LICENSE` and `NOTICE`.

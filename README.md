# ASCENT

**English** · [简体中文](README.zh-CN.md)

An Android app for people starting out in high-altitude mountaineering and ice climbing. The curriculum is organised by altitude: every lesson you pass moves you a little higher.

Two course tracks switch freely. **Alpine climbing** runs six units and 68 lessons, from a 5,000 m introduction to 8,000 m — 3,200 m to 8,848 m end to end. **Trekking** runs four units and 21 lessons, from day hikes to long trails — 200 m to 5,630 m. Each track keeps its own progress, badges and review queue.

Current version v0.19.30 (versionCode 105). The language work closed with v0.19.24 — Italian finished the set, so all nine translated languages match English key for key (10,554 / 10,554). Everything since has been app work: achievements realigned with the 89-lesson bank, the track icons redrawn, the splash mark scaled down, and a feedback screen that posts straight to the maintainer's inbox.

## Install

The easiest route is the prebuilt debug APK from [Releases](https://github.com/Solar-CV/AscentApp/releases). Pushing a `v*` tag makes GitHub Actions build and attach a fresh APK.

- Package: `com.ascent.climb`
- Minimum: Android 8.0 (API 26)
- Size: about 21 MB
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
│   └── qbank/build.mjs          Markdown → question bank compiler
└── app/src/main/
    ├── res/values*/             Interface strings for 10 languages
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt       Main lesson content
        │   ├── NewLessons.kt    Newer curriculum lessons
        │   ├── i18n/Pack*.kt    Content translations (entry / brief / handbook / quiz)
        │   ├── Diagnostics.kt   Crash log + environment snapshot for the feedback form
        │   ├── FeedbackSender.kt  Formspree POST (HttpURLConnection, no extra deps)
        │   ├── BundledBank.kt   Lesson pool assembly and review insertion
        │   └── bank/            Bank config, JSON decoding, network, repository
        └── ui/                  Screens and components
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

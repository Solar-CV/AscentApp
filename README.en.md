# ASCENT

[简体中文](README.md) · **English**

An Android app for people starting out in high-altitude mountaineering and ice climbing. The curriculum is organised by altitude: every lesson you pass moves you a little higher. Five units run from a 5,000 m introduction to 8,000 m, and after 47 lessons you have climbed from 3,200 m to 8,848 m.

Current version v0.18.35 (versionCode 71). This release closes out the multilingual work: the interface and the content are both available in 10 languages, and all four content layers are fully translated in the nine non-Chinese languages.

## Install

The easiest route is the prebuilt debug APK from [Releases](https://github.com/Solar-CV/AscentApp/releases). Pushing a `v*` tag makes GitHub Actions build and attach a fresh APK.

- Package: `com.ascent.climb`
- Minimum: Android 8.0 (API 26)
- Size: about 19 MB
- Permissions: network and network state, used only to check for question-bank updates. Everything works offline; the bundled bank is always there

Transfer the APK to your phone, allow installs from unknown sources, tap install. No login on first launch.

## What's inside

### The path

The home screen is a route from base camp upwards, split into five altitude units. Lessons unlock linearly — pass one and the next opens. Nodes show stars, the current lesson carries a "start" hint, and the last lesson of a unit is marked in gold.

Progress is not a stored number. It is recomputed from the lessons you have completed, so if the altitude curve is adjusted later, old saves recalculate instead of getting stuck.

### Question bank

629 formal questions plus 82 auto-inserted review copies, 711 display items in total. Volume rises with altitude: about 11 questions per lesson in the 5,000 m introduction, about 19 in the 8,000 m unit.

Eight question types: single choice, multiple choice, ordering, matching, fill-in, calculation, hotspot (tap a point on an image), and scenario (one stem answered in several steps).

Finishing a lesson drops you straight into redoing the ones you got wrong. Questions you then answer correctly gain credit; the rest go to the review centre. Review uses spaced repetition at 0, 1, 3, 7, 16 and 35 days. Safety-critical questions are weighted, and require confirming you understand before you can move on.

### Handbook

31 long-form articles, 4 to 8 sections each, sorted into gear, technique, medicine, health, weather, meteorology, decisions, judgement, safety and risk. Sources are public medical and technical material, three commercial guide documents, and edited transcripts of seven Chinese mountaineering books. Per product agreement, the Chinese books are not cited by title; the guide documents are.

### Training, review, profile

Training gives a readiness percentage and the target peak's altitude. Review puts today's due questions and safety-critical misses first. Profile holds the header, current altitude, ability overview, badge entry and account area. The gear icon opens settings, the avatar opens the profile page.

The profile shows unlocked badges only. All 35 live on the badge wall, locked ones as grey previews, hidden achievements as a "?".

### Settings

Appearance follows the system or can be pinned to light or dark, persisted, with the status bar matching. Language follows the system or can be set manually; interface and content both switch, no restart. The bank section shows the current version, checks for updates and restores the bundled bank. Developer mode hides behind the version number in About — tap it seven times.

## Ten languages

Interface strings and course content are available in:

简体中文 (source), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

All four layers are complete in the nine translated languages:

| Layer | Items | Coverage |
|---|---|---|
| Navigation and notes | 232 | 100% |
| Lesson key points | 215 | 100% |
| Handbook body | 326 | 100% |
| Question bank (stem / options / explanation) | 3,954 | 100% |

Terminology follows each language's own mountaineering usage rather than literal translation: Swedish uses stegjärn / isyxa / standplats, German Steigeisen / Pickel, Russian кошки / ледоруб. Medical doses, avalanche levels, slope angles and temperatures are carried over unchanged, with only the thousands separator and decimal mark adapted.

If an item is missing in the selected language, it falls back to English rather than Chinese — at least that stays readable technical text.

## Curriculum

| Unit | Altitude | Lessons | Questions | Per lesson |
|---|---|---|---|---|
| 5000M · Introduction | 5,000 m | 12 | 135 | 11 |
| 5000M · Progression | 5,000 m | 9 | 112 | 12 |
| 6000M | 6,000 m | 10 | 143 | 14 |
| 7000M | 7,000 m | 8 | 163 | 20 |
| 8000M | 8,000 m | 8 | 158 | 19 |

Thirty target peaks are listed, from Aotaina at 5,211 m up to the fourteen 8,000 m peaks.

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

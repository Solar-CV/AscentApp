# ASCENT

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · **[Deutsch](README.de.md)** · [Español](README.es.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Русский](README.ru.md) · [Svenska](README.sv.md)

Eine Android-App für Einsteiger in Hochtouren und Eisklettern. Der Lehrplan ist nach Höhe geordnet: mit jeder bestandenen Lektion geht es ein Stück höher.

Zwei Kurslinien lassen sich frei umschalten. **Hochtour** umfasst sechs Einheiten und 68 Lektionen, von der Einführung auf 5.000 m bis 8.000 m — durchgehend 3.200 m bis 8.848 m. **Trekking** umfasst vier Einheiten und 21 Lektionen, vom Tagesausflug bis zur Fernwanderung — 200 m bis 5.630 m. Jede Linie führt Fortschritt, Abzeichen und Wiederholungswarteschlange getrennt.

Aktuelle Version v0.20.13 (versionCode 120). Die Spracharbeit endete mit v0.19.24 — alle neun übersetzten Sprachen stimmen Eintrag für Eintrag mit dem Englischen überein (10.554 / 10.554). v0.20 dreht sich um die Abzeichen: 34 Erfolge werden jetzt als Echtzeit-3D-Objekte gerendert statt als flache Bilder, und ihre Abzeichen-Icons stammen aus denselben Modellen — die alten KI-Illustrationen sind weg. Der Freischaltdialog nutzt denselben Renderer und schießt Konfetti von beiden Bildschirmrändern ein. Die Modelle liegen im APK, deshalb wuchs das Paket von 21 MB auf etwa 39 MB. v0.20.3 behebt ein altes Ärgernis: Liste nach unten scrollen, einen Unterschirm öffnen, zurückkommen — die Liste sprang an den Anfang. Jetzt bleibt sie, wo sie war. v0.20.6–v0.20.7 heben die Abzeichen-Darstellung standardmäßig auf höchste Qualität: feinerer Bloom, der nicht mehr wegen der langsamen ersten Frames abschaltet, eine zweistufige Herabstufung und 4x MSAA mit Fähigkeitsprüfung und stillem Rückfall.

## Installation

Am einfachsten über das fertige Debug-APK aus den [Releases](https://github.com/Solar-CV/AscentApp/releases). Ein `v*`-Tag lässt GitHub Actions bauen und das APK anhängen.

- Paket: `com.ascent.climb`
- Minimum: Android 8.0 (API 26), **nur arm64-v8a** — reine 32-Bit-Geräte können diese Version nicht installieren
- Größe: etwa 39 MB (32 MB unkomprimierte Abzeichenmodelle, rund 9 MB nach dem Paketieren)
- Berechtigungen: Netzwerk und Netzwerkstatus — für die Prüfung auf Fragenpool-Updates und für Feedback. Alles funktioniert offline, der mitgelieferte Pool ist immer da

APK aufs Telefon übertragen, Installation aus unbekannten Quellen erlauben, tippen. Beim ersten Start ist keine Anmeldung nötig.

## Was drin ist

### Die Route

Der Startbildschirm ist eine Route vom Basislager nach oben, geteilt in Höheneinheiten (sechs beim Hochtouren, vier beim Trekking). Horizontal wischen blättert durch sie, zwei Einheiten pro Seite. Lektionen schalten linear frei — eine bestanden, die nächste offen. Knoten zeigen Sterne, die aktuelle Lektion trägt einen „Start"-Hinweis, die letzte Lektion einer Einheit ist gold markiert.

Zwei Pillen oben schalten die Kurslinie um; ein Tipp öffnet ein Bottom-Sheet. Das Umschalten springt auf die erste Seite zurück, die Wahl bleibt gespeichert.

Fortschritt ist keine gespeicherte Zahl. Er wird aus den abgeschlossenen Lektionen neu berechnet — wird die Höhenkurve später angepasst, rechnen sich alte Spielstände neu, statt hängenzubleiben.

### Fragenpool

1.533 reguläre Fragen plus 154 automatisch eingefügte Wiederholungskopien, zusammen 1.687 Anzeigeposten. Die Menge steigt mit der Höhe: rund 15 Fragen pro Lektion in der Einführung auf 5.000 m, 30 in den technischen Einheiten auf 7.000 m, 8 im Trekking.

Acht Fragetypen: Einfachwahl, Mehrfachwahl, Reihenfolge, Zuordnung, Lückentext, Rechnung, Hotspot (einen Punkt im Bild antippen) und Szenario (ein Stamm, in mehreren Schritten beantwortet).

Nach einer Lektion geht es direkt in die Wiederholung der falsch beantworteten Fragen. Richtig beantwortete bringen Punkte, der Rest wandert ins Wiederholungszentrum. Die Wiederholung arbeitet mit verteilten Intervallen von 0, 1, 3, 7, 16 und 35 Tagen. Sicherheitskritische Fragen sind gewichtet und verlangen eine Bestätigung des Verständnisses, bevor es weitergeht.

### Handbuch

35 lange Artikel mit je 4 bis 8 Abschnitten, sortiert nach Ausrüstung, Technik, Medizin, Gesundheit, Wetter, Meteorologie, Entscheidung, Beurteilung, Sicherheit, Risiko und Lawinenrettung. Das Handbuch ist nach Linie gefiltert: Schnee-und-Eis-Stücke erscheinen nur beim Hochtouren, Höhenmedizin, Unterkühlung, Navigation, Lager und Führung teilen beide Linien. Quellen sind öffentliches medizinisches und technisches Material, drei kommerzielle Führer-Dokumente und bearbeitete Transkripte von sieben chinesischen Bergbüchern. Laut Produktvereinbarung werden die chinesischen Bücher nicht mit Titel zitiert, die Führer-Dokumente schon.

### Training, Wiederholung, Profil

Training liefert einen Bereitschafts-Prozentsatz und die Höhe des Zielgipfels. Die Wiederholung stellt die heute fälligen Fragen und sicherheitskritische Fehler nach vorn. Das Profil enthält Kopfbereich, aktuelle Höhe, Fähigkeitsübersicht, Abzeichen-Einstieg, Kontobereich und einen Feedback-Einstieg. Das Zahnrad öffnet die Einstellungen, der Avatar die Profilseite.

Das Profil zeigt nur freigeschaltete Abzeichen. Alle 40 stehen an der Abzeichenwand, gesperrte als graue Vorschau, versteckte Erfolge als „?".

Die meisten gibt es zusätzlich als echtes 3D-Modell — Metallrahmen, Emaille-Einlagen, Relief. Im Erfolgs-Testbereich antippen und ein Abzeichen dreht sich: ziehen zum Drehen, mit zwei Fingern zoomen, und es wiegt sich von selbst leicht, sobald man loslässt. Die Icons stammen aus den Modellen selbst, Wand und 3D-Ansicht zeigen also dasselbe Objekt; die alten KI-Illustrationen sind ausgemustert. Die übrigen zwölf ohne Modell behalten ihre handgezeichnete Grafik und erscheinen als perspektivisch gekippte Karte. Wird ein Erfolg irgendwo in der App freigeschaltet, zeigt der Dialog dasselbe 3D-Abzeichen, auf einem hellen Sockel, während von beiden Bildschirmrändern Bänder hereinfliegen.

### Einstellungen

Das Erscheinungsbild folgt dem System oder lässt sich auf Hell oder Dunkel festlegen, wird gespeichert, die Statusleiste zieht mit. Die Sprache folgt dem System oder wird manuell gesetzt; Oberfläche und Inhalte wechseln gemeinsam, kein Neustart. Der Pool-Bereich zeigt die aktuelle Version, prüft auf Updates und stellt den mitgelieferten Pool wieder her. Der Entwicklermodus liegt hinter der Versionsnummer unter „Über" — siebenmal tippen.

### Feedback

Profil → Feedback und Vorschläge, zwischen Kontokarte und Sicherheitshinweis. Art wählen — Fehler, Idee, Inhaltskorrektur —, Nachricht schreiben, optional eine Adresse für die Antwort hinterlassen und optional Diagnose dazulegen. Das Senden geht über Formspree direkt an den Maintainer; ohne Netz bietet der Bildschirm an, denselben Text per E-Mail zu schicken, und kopiert ihn in die Zwischenablage, wenn keine Mail-App vorhanden ist.

Die Diagnose ist standardmäßig aus und umfasst App-Version, Android-Version, Gerätemodell, Sprache, Pool-Version, aktuelle Linie, Fortschrittszahlen und die letzte unbehandelte Ausnahme. Kein Lektionstext, kein Antwortverlauf, kein Konto, keine Zwischenablage.

## Zehn Sprachen

Oberflächentexte und Kursinhalte gibt es in:

简体中文 (Quelle), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

Alle zehn Sprachen sind vollständig. Jede hat ihre eigene Einstiegsebene, Lektionskerntexte, Handbuchtexte und einen Fragenpool, und jeder Schlüsselsatz stimmt Eintrag für Eintrag mit dem Englischen überein — **10.554 / 10.554**, nichts fehlt, nichts doppelt, keine verwaisten Schlüssel.

| Ebene | Einträge | Alle 9 übersetzten Sprachen |
|---|---|---|
| Einstieg (Lektionen, Einheiten, Handbuchtitel, Gipfel) | 117 | 100 % |
| Lektionskerntexte | 443 | 100 % |
| Handbuchtexte | 378 | 100 % |
| Fragenpool (Stamm, Optionen, Erklärung) | 9.422 | 100 % |

Die Terminologie folgt dem Sprachgebrauch der jeweiligen Bergsteigerszene statt wörtlicher Übersetzung: schwedisch stegjärn / isyxa / standplats, deutsch Steigeisen / Pickel, russisch кошки / ледоруб, italienisch corda fissa / bloccante / sosta. Medizinische Dosierungen, Lawinenstufen, Hangneigungen und Temperaturen bleiben unverändert — angepasst wird nur das Zahlenformat, und die drei Konventionen bleiben bewusst getrennt:

- Englisch, Chinesisch, Japanisch: `6.5 °C`, `5,000 m`
- Deutsch, Spanisch, Italienisch: `6,5 °C`, `1.000 m` (Dezimalkomma, Tausenderpunkt)
- Französisch, Schwedisch: `6,5 °C`, `5 000 m` (Dezimalkomma, Tausenderleerzeichen)

Auch die Ansprache unterscheidet sich: Deutsch nutzt die unpersönliche man-Form, Spanisch duzt mit *tú*, Französisch siezt mit *vous*, Schwedisch und Italienisch duzen mit *du* / *tu* — die Lektionskerntexte bleiben in allen Sprachen unpersönlich.

Fehlt ein Eintrag in der gewählten Sprache, lautet die Reihenfolge: gewählte Sprache → Englisch → chinesische Quelle. Dieser Pfad ist inzwischen nur noch ein Sicherheitsnetz.

## Lehrplan

**Hochtour**

| Einheit | Höhe | Lektionen | Fragen | Pro Lektion |
|---|---|---|---|---|
| 5000M · Einführung | 5.000 m | 16 | 249 | 16 |
| 5000M · Aufbau | 5.300 m | 12 | 221 | 18 |
| 6000M | 6.000 m | 14 | 316 | 22 |
| 7000M · C3 Techniklager | 7.000 m | 6 | 185 | 31 |
| 7000M · C4 Hochlager | 7.500 m | 10 | 279 | 28 |
| 8000M | 8.000 m | 10 | 248 | 24 |

**Trekking**

| Einheit | Höhe | Lektionen | Fragen | Pro Lektion |
|---|---|---|---|---|
| Tageswanderungen | 1.200 m | 5 | 40 | 8 |
| Mehrtagestouren und Lager | 2.500 m | 8 | 79 | 10 |
| Höhentrekking | 3.800 m | 5 | 44 | 8 |
| Fernwege und Notfälle | 4.500 m | 3 | 26 | 8 |

Insgesamt 38 Zielgipfel und Routen: 30 fürs Hochtouren, von der Aotaina auf 5.211 m bis zu den vierzehn Achttausendern; 8 fürs Trekking, vom Huihang-Pfad bis zur Three-Passes-Route am Everest.

## Woher die Inhalte kommen

Zwei Hauptströme speisen Fragen und Handbuch.

Zu den öffentlichen Quellen gehören die 2024er Aktualisierung der klinischen Leitlinien für akute Höhenkrankheit der Wilderness Medical Society, das CDC Yellow Book, technisches Material des American Alpine Institute und des American Alpine Club, REI Expert Advice, die Ausrüstungsnormen von UIAA und CE sowie Lawinendaten von avalanche.org.

Die drei Führer-Dokumente von SEVEN SUMMIT TREKS — Höhenkrankheit, Ausrüstung, allgemeine Expeditionspraxis — sind der zweite Strom.

Später wurden geprüfte Transkripte von sieben chinesischen Bergbüchern zu Handbuchtexten verarbeitet. Wissen und Fälle sind in eigenen Worten wiedergegeben, mit Seitenangaben; der gescannte Quelltext liegt nicht im Repository.

| Quelle | Verwendet für |
|---|---|
| WMS / CDC / StatPearls | Höhenmedizinische Fragen und Handbuch |
| AAI / AAC / REI / BMC | Schnee-, Eis- und Bewegungstechnik |
| UIAA / CE-Normen | Ausrüstungsfragen |
| avalanche.org | Lawinenbeurteilung |
| SEVEN SUMMIT TREKS | Handbuch h17–h22 und zugehörige Fragen |
| 7 chinesische Bergbücher | Handbuch h23–h31 |

## Sicherheitsgrenze

Die App deckt nur Theorie und kognitive Vorbereitung ab. Praktische Fertigkeiten — Schnee und Eis, Seiltechnik, Eisklettern, Rettung — müssen von einer zertifizierten Ausbilderin oder einem zertifizierten Ausbilder vor Ort vermittelt und geprüft werden.

Nichts hier ist medizinischer Rat. Entscheidungen zu Medikamenten und Erster Hilfe gehören in ärztliche Hand.

Kein Gipfel wird versprochen, kein Akklimatisierungserfolg garantiert.

Lektionen, die eine Begleitung erfordern, sagen das ausdrücklich, sobald die Theorie durch ist.

## Selbst bauen

Die ganze Toolchain kann im Home-Verzeichnis liegen — kein Homebrew, kein Android Studio. Was diese Maschine nutzt:

- `~/android-toolchain/jdk`: Temurin OpenJDK 17
- `~/android-toolchain/sdk`: Android SDK mit platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9`: Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

Das Repository bringt außerdem den Gradle-8.9-Wrapper mit, ein Rechner ohne Gradle funktioniert also auch:

```bash
cd AscentApp
./gradlew assembleDebug
```

Das Ergebnis liegt in `app/build/outputs/apk/debug/app-debug.apk`.

## Projektaufbau

```
AscentApp/
├── import/                      Fragenerstellung (Markdown, aus gedruckten Büchern)
├── knowledge/                   Wissensbasis der Druckquellen
├── tools/
│   ├── contentcheck/            Inhaltsprüfungen + Pool-Export (reines JVM)
│   ├── i18ncheck.py             Abdeckung pro Sprache über alle vier Ebenen
│   ├── qbank/build.mjs          Markdown → Fragenpool-Compiler
│   └── poc3d/                   Modellabgleich, Icon-Freistellung, Emissive-Backen
└── app/src/main/
    ├── res/values*/             Oberflächentexte für 10 Sprachen
    ├── assets/
    │   ├── badges/*.glb         34 Abzeichenmodelle
    │   └── env/studio_ibl.ktx   Prozedural erzeugte IBL-Umgebung
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt      Inhalte der Hauptlektionen
        │   ├── NewLessons.kt    Spätere Lehrplanlektionen
        │   ├── i18n/Pack*.kt    Inhaltsübersetzungen (Einstieg / Kern / Handbuch / Pool)
        │   ├── Diagnostics.kt   Absturzprotokoll + Umgebungsaufnahme fürs Feedback
        │   ├── FeedbackSender.kt  Formspree-POST (HttpURLConnection, keine Extras)
        │   ├── BundledBank.kt   Zusammenbau des Lektionspools und Einfügen der Wiederholungen
        │   └── bank/            Pool-Konfiguration, JSON-Dekodierung, Netzwerk, Repository
        └── ui/                  Bildschirme und Bausteine (Badge3D.kt = Filament-Szene, Bildausschnitt, Licht)
```

## Inhalte ändern

Für neue Fragen in einer bestehenden Lektion `data/Expansion*.kt` bearbeiten. Für eine neue Lektion `Lesson(...)` in `data/NewLessons.kt` schreiben und die id in der passenden Einheit in `Curriculum.kt` ergänzen. Reihenfolge und Einheitenzugehörigkeit liegen nur in `Curriculum.kt`.

Danach die Prüfungen laufen lassen:

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

Das erste Skript synchronisiert die Inhalte der App in ein reines JVM-Projekt, führt die echte Zusammenbaulogik aus und prüft Fragenanzahl, Schwierigkeitsverlauf, Antworten außerhalb des Bereichs, leere Fragen, Freischaltlogik und Erfolgsdefinitionen; anschließend exportiert es `bank/bank.json` und `bank/manifest.json`. Das zweite meldet die Abdeckung je Sprache über die vier Ebenen und vergleicht die Schlüssel. Beide brauchen keinen Emulator.

Nach Änderungen am Pool `BUNDLED_VERSION` in `data/bank/BankConfig.kt` erhöhen, sonst glaubt der Client, es habe sich nichts geändert.

## Online-Updates des Pools

Der Pool hat drei Ebenen: lokaler Cache, mitgelieferte Fassung, Online-Delta. Die Reihenfolge ist Cache → mitgeliefert; die Online-Fassung überschreibt den Cache nur, wenn sie neuer ist.

Im Flugzeug, im Tal ohne Signal, bei totem Server, fehlerhaftem JSON, falscher sha256 oder einer älteren Version bleibt also alles, wie es ist. Updates werden nach dem Start still geprüft, höchstens alle sechs Stunden und nur wenn eine URL konfiguriert ist.

Neuen Pool veröffentlichen: Inhalte ändern → `BUNDLED_VERSION` erhöhen → `contentcheck` laufen lassen → `bank/bank.json` und `bank/manifest.json` auf einen Server oder CDN legen → die App übernimmt ihn beim nächsten Start, ohne Release.

Ältere App-Versionen gehen an einem neueren Pool nicht kaputt. `minAppVersion` in `manifest.json` blockt zu alte Clients, unbekannte Felder werden ignoriert, eine fehlerhafte Frage wird übersprungen statt tödlich zu sein.

## Letzte Änderungen

**v0.20.13: sechs weitere Abzeichenmodelle**

- Die Bibliothek wächst von 28 auf 34: 6.000 m Lernen, Comeback, Sieben-Tage- und Drei-Wochen-Spuren, zwanzig Läufe und ein D-förmiger Karabiner für „Sicherheit zuerst“; self_arrest neu exportiert
- Die Icon-Freistellung entfernt jetzt auch Hintergrund, der von der Abzeichenform *umschlossen* ist — das Mittelloch des Karabiners und die Lochung einer Stanzkarte waren bisher massiv schwarze Scheiben. Jede eingeschlossene Fläche wird einzeln geprüft: gleichmäßige Farbe wie der Hintergrund gilt als Loch, Verläufe bleiben. Paket 38.5 → 39.4 MB

**v0.20.11: Beleuchtung und Hintergrund der 3D-Badge-Ansicht**

- Die Badge-Ansicht hatte zwei Lichter, daher gab es nur einen einzigen Glanzbogen und das Relief blieb flach. Jetzt sind es fünf gerichtete Lichter — Key, Top, Front-Fill, Rim und ein schwacher Bounce von unten — bei unveränderter Gesamtbeleuchtung: mehr Tiefe, ohne auszubleichen
- Ein zweiter, dunklerer Hintergrund (ein Synthwave-Sonnenuntergang, stark verwischt zu reinen Farbflächen) passt zum dunklen Modus. Die Pille oben rechts schaltet zwischen hell und dunkel, folgt standardmäßig dem System und merkt sich die Wahl; nur dieser Bildschirm ist betroffen

**v0.20.8 – v0.20.10: Startbildschirm**

- Der Startbildschirm ist jetzt aus Clay: eine weiße Scheibe mit straffer, scharfkantiger Schattierung, das Gipfelzeichen und statt des alten Slogans ein zufälliges Zitat eines Bergsteigers — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe oder Bonatti. Nach rund 1,7 s blendet er aus, bei reduzierter Bewegung wird er fast übersprungen. Sonst hat sich in der App nichts geändert Ab v0.20.9 zeigt der System-Splash kein Logo mehr — das Start-Theme ist nur noch die gleiche Hintergrundfarbe, sodass der Clay-Screen auf einem leeren weißen (oder fast schwarzen) Feld beginnt. Jedes Zitat steht in der Originalsprache

**v0.20.6 – v0.20.7: Darstellungsqualität der Abzeichen**

- Bloom läuft standardmäßig in höchster Qualität — 256 px, vier Stufen, MEDIUM. Die alte automatische Herabstufung urteilte schon ab dem ersten Frame, sodass wenige hundert Millisekunden Shader-Kompilierung den Bloom für die restliche Sitzung abschalteten. Jetzt wird 2,5 s gewartet, die Schwelle liegt bei 50 ms (20 fps) über drei aufeinanderfolgende Fenster, und die Stufen laufen voll → reduziert → aus
- 4x MSAA ist aktiv, wo der Treiber es unterstützt; die Fähigkeit wird vor dem Erzeugen der Swap Chain geprüft. Scheitert der Versuch, wird ohne MSAA weitergezeichnet und der Fehlschlag gemerkt — ein Antialiasing-Flag darf nie dazu führen, dass ein Abzeichen nicht erscheint

**v0.20.4 – v0.20.5: kleine Korrekturen**

- Der Entwicklermodus ist bis zur Freischaltung unsichtbar — der ganze Abschnitt in den Einstellungen, inklusive „Aus · Versionsnummer siebenmal tippen", erscheint für normale Nutzer nicht mehr. Der Zähler für die sieben Tipps funktioniert weiter, zählt aber nicht mehr laut mit
- Auch im Erfolgs-Testbereich bleibt die Scrollposition erhalten: in die 3D-Ansicht eines Abzeichens und zurück, und von den Einstellungen in den Testbereich und zurück
- Der Freischaltdialog schließt nicht mehr bei einem Streiftipp: der erste Tipp auf den Hintergrund spannt ihn nur vor und blendet „Erneut tippen zum Schließen" ein; der zweite schließt. „Annehmen" und die Zurück-Geste schließen weiterhin sofort

**v0.20.0 – v0.20.3: 3D-Abzeichen**

- 34 Erfolge werden in Echtzeit 3D gerendert (Filament 1.75, glTF, PBR-Metall und Emaille). Ziehen dreht, zwei Finger zoomen, in Ruhe wiegt sich das Objekt leicht; die automatische Bewegung bleibt an der Vorderseite, weil die Rückseite einer Medaille eine glatte Platte ist
- Die übrigen sechs behalten ihre stehende Grafik, perspektivisch gekippt, mit runden Ecken und Schatten — eine Sammelkarte, kein flaches Bild. Sobald ein Modell vorliegt, wechseln sie automatisch auf 3D
- Freischalt- und Vorschau-Dialoge laufen über denselben Renderer, mit hellem Sockel hinter dem Modell (ein dunkles Modell auf dunkler Karte verschwindet) und 56 Bändern aus beiden Richtungen. „Bewegung reduzieren" schaltet die Bänder ab
- Die Anzeigeseite sitzt auf einer stark unscharfen Landschaft, prozedural erzeugt (27 KB) — Himmel, vier Grate, Nebel, keine Fotolizenz nötig
- Ziehrichtungen nach Feedback umgedreht; die Szene insgesamt aufgehellt und dann leicht zurückgenommen, damit emittierende Teile Kontrast behalten
- In den Modellen angelegte Lichter überleben jetzt: `KHR_lights_punctual`-Entitäten werden ausdrücklich in die Szene gehängt (glTF-Asset-Entitäten enthalten sie nicht) und ihre Intensität wird an die Belichtung der Szene skaliert — 54 cd neben einem Schlüssellicht mit 66.000 Lux sind sonst unsichtbar. `KHR_materials_emissive_strength` wird beim Bauen in `emissiveFactor` gebacken, Bloom ist an und schaltet sich ab, wenn die Frame-Zeit über 28 ms bleibt
- Die Modellbibliothek wuchs in diesem Abschnitt von 17 auf 34: Serien, Perfekt, Wiederholung, Rückzug, Trekking-Abschluss und drei Neuausgaben kamen dazu; Paket 21 → 39 MB
- **Abzeichen-Icons kommen jetzt aus den Modellen**: vom Frontrender wird der schwarze Hintergrund zur transparenten 256-px-Grafik freigestellt, sodass Wand, Freischaltdialog und 3D-Ansicht dasselbe Objekt zeigen. Die KI-Illustrationen dieser Abzeichen wurden verworfen
- Spotlight-Verstärkung auf 5× vereinfacht (das eigene Spotlight des Modells zielt in den Medaillenkörper, aus keiner Verstärkung wird es von vorn sichtbar — das gehört in die Blender-Quelle), Bloom auf 0,05 gedreht
- v0.20.3: Die Scrollposition einer Liste überlebt den Ausflug in einen Unterschirm (Einstellungen → Sprache, Handbuch → Artikel, Profil → Einstellungen, Wiederholung → Lektion), und die Rückkehr aus einer Lektion setzt den Routen-Pager nicht mehr auf Seite eins

**v0.19.25 – v0.19.30: Arbeit an der App**

- Erfolge an den Pool mit 89 Lektionen angeglichen. `full_route` bedeutet jetzt alle 68 Hochtouren-Lektionen — vorher stand dort 66, und es wurde gegen die gerade offene Linie geprüft, sodass Trekking allein den „theoretischen Grand Slam" einbrachte. `five_zones` wurde zu „sechs Zonen" passend zu den sechs Hochtouren-Einheiten. Fünf Trekking-Erfolge ergänzt, 35 → **40**
- Der Erfolgsfortschritt wird über beide Linien gerechnet statt über die gewählte, das Ziel verschiebt sich beim Umschalten also nicht mehr
- Linien-Icons als massive Silhouetten neu gezeichnet — Schneegipfel und Bergstiefel — und damit die `▲` / `≡`-Zeichen im Sheet ersetzt; die oberen Pillen tragen dieselben zwei in 14 dp
- Splash-Logo um 40 % verkleinert
- Feedback-Bildschirm im Profil, der an Formspree sendet, mit E-Mail- und Zwischenablage-Rückweg; der Crash-Handler behält die letzten fünf unbehandelten Ausnahmen im app-privaten Speicher
- Profil und Startseite zählten den Gesamtfortschritt gegen die Lektionszahl der aktuellen Linie und zeigten nach dem Umschalten „68 / 21"; behoben

v0.19 ist die Inhaltserweiterung; Pool-Struktur und Bewertungslogik sind unverändert.

- Lektionen 47 → 89: Hochtouren jetzt 68 (Einheiten 6000 / 7000 / 8000 m, alle vierzehn Achttausender), dazu 21 neue Trekking-Lektionen
- Fragen 711 → 1.533 (9.422 Schlüssel), Handbuch 31 → 35 Artikel (Lawinenverschüttung und Reanimation, das Lawinen-Trio, Fixseile, Flussquerung), Pool v15 → v19
- **Neun Sprachen jetzt vollständig über alle vier Inhaltsebenen**: Englisch, Traditionelles Chinesisch, Japanisch, Russisch, Deutsch, Spanisch, Französisch, Schwedisch, Italienisch. Ihre Schlüsselsätze stimmen mit dem Englischen überein (10.554 / 10.554), zur Laufzeit fällt nichts zurück
- Deutsch nutzt die unpersönliche man-Form, Spanisch duzt mit tú, Französisch siezt mit vous, Schwedisch und Italienisch duzen mit du / tu; jede Sprache folgt ihren Zahlenkonventionen — Deutsch, Spanisch und Italienisch schreiben 1.000 m, Französisch und Schwedisch 5 000 m, alle mit Dezimalkomma
- Verwaiste Schlüssel aus früheren Inhaltsänderungen (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) wurden aus allen neun Sprachen entfernt, sodass jeder Schlüsselsatz sauber und identisch ist
- Traditionelles Chinesisch läuft über OpenCC s2twp plus Terminologietabelle, per Regression gegen die bestehenden Übersetzungen geprüft (null Abweichungen); Japanisch ist eine echte Übersetzung, mit einem Register für den Fragenpool und einem für das Handbuch
- `tools/i18n/` bietet drei Kommandos (`status.py` / `prep.py` / `apply.py`) für alle neun Sprachen; für Traditionelles Chinesisch zusätzlich `tw.py` und `tw_entry.py`
- Der Pool ist auf Dateien verteilt: eine einzelne Kotlin-Datei mit rund 2.500 Einträgen bringt den Compiler zum Absturz, deshalb ist jede der neun Übersetzungssprachen in drei Teile geteilt (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

v0.16 bis v0.18 waren reine Mehrsprachigkeitsarbeit; Pool-Struktur und Bewertungslogik blieben unverändert.

- Oberflächentexte in 10 `res/values*`-Sätze aufgeteilt; die Sprache lässt sich manuell setzen oder dem System folgen, wirksam sofort
- Neue Inhaltsebene `data/i18n/` mit vier Paketen pro Sprache: Einstieg, Kernpunkte, Handbuchtext, Fragenpool. 4.727 Einträge pro Sprache
- Alle vier Ebenen in den neun Übersetzungssprachen (zh-TW, en, ja, de, fr, es, it, ru, sv) fertig; die acht außer Englisch Schlüssel für Schlüssel gegen die englische Quelle geprüft, null fehlend
- Terminologie an den jeweiligen Sprachgebrauch angeglichen; Dosierungen, Lawinenstufen, Hangneigungen und Temperaturen unverändert übernommen
- `tools/i18ncheck.py` ergänzt, damit ein Kommando die Abdeckung über alle vier Ebenen zeigt — kein „übersetzt, aber eine Lektion fehlt" mehr
- Unterwegs eine Reihe von Textfehlern behoben: ein Komma-vor-Gedankenstrich-Fehler (140 im Schwedischen, je 5 im Deutschen und Spanischen), zwei Lückentextantworten mit überzähligem Anführungszeichen und drei französische Dezimalzahlen

Früher:

- v0.15.1: Textbereinigung in der App, wörtliche `**`-Marker entfernt, eine Reihe Erklärungen neu geschrieben, die Anmeldeseite verspricht keine nicht implementierte Synchronisierung mehr
- v0.15.0: neun weitere Handbuchartikel aus chinesischen Büchern (h23–h31), neue Kategorie „Führung im Gelände", 22 → 31 Artikel
- v0.14.0: sechs Führer-Artikel von SEVEN SUMMIT TREKS
- v0.13.x: Einstellungen, Profilseite, Abzeichenwand; Zurück-Geste auf Unterschirmen

## Lizenz

PolyForm Noncommercial License 1.0.0 — nur nichtkommerzielle Nutzung. Kommerzielle Nutzung braucht eine separate schriftliche Erlaubnis; siehe `LICENSE` und `NOTICE`.

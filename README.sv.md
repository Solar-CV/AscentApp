# ASCENT

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Русский](README.ru.md) · **[Svenska](README.sv.md)**

En Android-app för dig som börjar med höghöjdsalpinism och isklättring. Kursen är ordnad efter höjd: varje avklarad lektion flyttar dig en bit högre upp.

Två kursspår byts fritt. **Högfjällsbestigning** har sex enheter och 68 lektioner, från introduktionen på 5 000 m till 8 000 m — 3 200 m till 8 848 m hela vägen. **Vandring** har fyra enheter och 21 lektioner, från dagsutflykt till långled — 200 m till 5 630 m. Varje spår håller egen progress, egna emblem och egen repetitionskö.

Aktuell version v0.21.0 (versionCode 129). Språkarbetet avslutades med v0.19.24 — alla nio översatta språk matchar engelskan post för post (10 554 / 10 554). v0.20 handlar om emblemen: alla fyrtio prestationer renderas nu som 3D-objekt i realtid i stället för platta bilder, och deras ikoner kommer från samma modeller — de gamla AI-illustrationerna är borta. Upplåsningsdialogen använder samma renderare och skjuter konfetti från båda skärmkanterna. Modellerna ligger i APK:n, så paketet växte från 21 MB till cirka 41 MB. v0.20.3 rättar till en gammal irritation: du scrollade ned i en lista, öppnade en underskärm, kom tillbaka — och listan hoppade till början. Nu stannar den där du lämnade den. v0.20.6–v0.20.7 lyfter emblemens rendering till högsta kvalitet som standard: finare bloom som inte längre stängs av på grund av långsamma första bildrutor, nedväxling i två steg och 4x MSAA med kapacitetskontroll och tyst återgång.

v0.20.19 – v0.21.0 klädde om hela gränssnittet i lera: mjuka dubbelskuggor som ritas utanför skärmen och cachas, platta färgrutor, kategorifärger i handboken och stora färgytor vars djup kommer från en kontakt­skugga i stället för en gradient.

## Installation

Enklaste vägen är det färdigbyggda debug-APK:t från [Releases](https://github.com/Solar-CV/AscentApp/releases). En push av en `v*`-tagg låter GitHub Actions bygga och bifoga ett nytt APK.

- Paket: `com.ascent.climb`
- Minsta krav: Android 8.0 (API 26), **endast arm64-v8a** — telefoner med bara 32 bitar kan inte installera den här versionen
- Storlek: cirka 41 MB (38 MB okomprimerade emblemmodeller, ungefär 11 MB efter paketering)
- Behörigheter: nätverk och nätverksstatus — för att söka efter uppdateringar av frågebanken och skicka feedback. Allt fungerar offline; den medföljande banken finns alltid där

Flytta över APK:n till telefonen, tillåt installation från okända källor och tryck på installera. Ingen inloggning vid första starten.

## Vad som finns inuti

### Vägen

Startskärmen är en rutt från baslägret och uppåt, delad i höjdenheter (sex för högfjäll, fyra för vandring). Svep i sidled för att bläddra, två enheter per sida. Lektionerna låses upp linjärt: en klar, nästa öppnas. Noderna visar stjärnor, aktuell lektion har en "börja"-hint och sista lektionen i en enhet är markerad i guld.

Två piller högst upp byter kursspår; ett tryck öppnar ett nedre ark. Bytet går tillbaka till första sidan och valet sparas.

Progress är inte ett sparat tal. Det räknas om från de lektioner du klarat, så om höjdkurvan justeras senare räknas gamla sparanden om i stället för att fastna.

### Frågebank

1 533 ordinarie frågor plus 154 automatiskt insatta repetitionskopior, 1 687 poster totalt. Mängden ökar med höjden: omkring 15 frågor per lektion i introduktionen på 5 000 m, 30 i de tekniska enheterna på 7 000 m och 8 i vandringen.

Åtta frågetyper: enkelval, flerval, ordningsföljd, matchning, lucktext, beräkning, hotspot (tryck på en punkt i bilden) och scenario (en frågestam som besvaras i flera steg).

När en lektion är klar går du direkt in och gör om dem du missade. Rättade frågor ger poäng, resten går till repetitionscentret. Repetitionen använder utspridda intervall på 0, 1, 3, 7, 16 och 35 dagar. Säkerhetskritiska frågor är viktade och kräver att du bekräftar att du förstått innan du går vidare.

### Handbok

35 långa artiklar med 4 till 8 avsnitt, sorterade på utrustning, teknik, medicin, hälsa, väder, meteorologi, beslut, bedömning, säkerhet, risk och lavinräddning. Handboken filtreras på spår: snö- och isartiklarna visas bara under högfjäll, medan höjdmedicin, nedkylning, navigation, läger och ledarskap delas av båda. Källorna är offentligt medicinskt och tekniskt material, tre kommersiella guidehandlingar och bearbetade utskrifter av sju kinesiska bergsböcker. Enligt produktöverenskommelsen anges de kinesiska böckerna inte med titel; guidehandlingarna anges.

### Träning, repetition, profil

Träningen ger en beredskapsprocent och måltoppens höjd. Repetitionen sätter dagens förfallna frågor och säkerhetskritiska misstag först. Profilen rymmer huvud, aktuell höjd, förmågeöversikt, emblemets ingång, kontoområde och en feedback-ingång. Kugghjulet öppnar inställningarna, avataren profilsidan.

Profilen visar bara upplåsta emblem. Alla 40 finns på emblemväggen, låsta som grå förhandsvisning, dolda prestationer som ett "?".

Emblemen är riktiga 3D-objekt — metallramar, emalj, relief. Tryck på ett upplåst emblem i profilen eller på väggen så öppnas det i fullskärm: dra för att rotera, nyp för att zooma upp till cirka 5,5×, två fingrar flyttar vyn över reliefen, dubbeltryck återställer, och när du släpper gungar det sakta av sig självt. Ikonerna kommer från modellerna själva, så väggen och 3D-vyn visar samma föremål. Alla fyrtio har nu en modell; den tecknade bilden finns kvar som reserv för emblem som läggs till senare. När en prestation låses upp någonstans visar dialogen samma 3D-emblem, på en ljus piedestal, medan band far in från båda skärmkanterna.

### Inställningar

Utseendet följer systemet eller låses till ljust eller mörkt, sparas, och statusfältet hänger med. Språket följer systemet eller väljs manuellt; gränssnitt och innehåll byts samtidigt, ingen omstart. Bankavsnittet visar aktuell version, söker uppdateringar och återställer den medföljande banken. Utvecklarläget ligger bakom versionsnumret under "Om" — sju tryck.

### Feedback

Profil → Feedback och förslag, mellan kontokortet och säkerhetsnotisen. Välj typ — fel, idé, innehållsrättelse — skriv meddelandet, lämna valfritt en adress för svar och bifoga valfritt diagnostik. Skickandet går direkt till underhållaren via Formspree; utan nät erbjuder skärmen att skicka samma text med e-post och kopierar den till urklipp om ingen mejlapp finns.

Diagnostiken är avstängd som standard och omfattar appversion, Android-version, enhetsmodell, språk, bankversion, aktuellt spår, progressionssiffror och det senaste ohanterade undantaget. Ingen lektionstext, ingen svarshistorik, inget konto, inget urklipp.

## Tio språk

Gränssnittstexter och kursinnehåll finns på:

简体中文 (källa), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

Alla tio språk är kompletta. Varje språk har sitt eget ingångslager, lektionssammanfattningar, handbokstext och frågebank, och varje nyckeluppsättning matchar engelskan post för post — **10 554 / 10 554**, inget saknas, inget dubbelt, inga föräldralösa nycklar.

| Lager | Poster | Alla 9 översatta språk |
|---|---|---|
| Ingång (lektioner, enheter, handboksrubriker, toppar) | 117 | 100 % |
| Lektionernas huvudpunkter | 443 | 100 % |
| Handbokstext | 378 | 100 % |
| Frågebank (fråga, alternativ, förklaring) | 9 422 | 100 % |

Terminologin följer varje språks eget bergsbruk i stället för ordagrann översättning: svenska stegjärn / isyxa / standplats, tyska Steigeisen / Pickel, ryska кошки / ледоруб, italienska corda fissa / bloccante / sosta. Medicinska doser, lavinnivåer, sluttningar och temperaturer förs över oförändrade — bara sifferformatet anpassas, och de tre konventionerna hålls medvetet åtskilda:

- Engelska, kinesiska, japanska: `6.5 °C`, `5,000 m`
- Tyska, spanska, italienska: `6,5 °C`, `1.000 m` (decimalkomma, tusentalspunkt)
- Franska, svenska: `6,5 °C`, `5 000 m` (decimalkomma, tusentalsmellanslag)

Även tilltalet skiljer sig: tyskan använder opersonlig *man*-form, spanskan tilltalar med *tú*, franskan med *vous*, svenskan och italienskan med *du* / *tu*, medan lektionssammanfattningarna är opersonliga på alla språk.

Om en post saknas på det valda språket är ordningen: valt språk → engelska → kinesiska originalet. Den vägen är numera bara ett skyddsnät.

## Kursplan

**Högfjällsbestigning**

| Enhet | Höjd | Lektioner | Frågor | Per lektion |
|---|---|---|---|---|
| 5000M · Introduktion | 5 000 m | 16 | 249 | 16 |
| 5000M · Vidare | 5 300 m | 12 | 221 | 18 |
| 6000M | 6 000 m | 14 | 316 | 22 |
| 7000M · Tekniskt läger C3 | 7 000 m | 6 | 185 | 31 |
| 7000M · Högläger C4 | 7 500 m | 10 | 279 | 28 |
| 8000M | 8 000 m | 10 | 248 | 24 |

**Vandring**

| Enhet | Höjd | Lektioner | Frågor | Per lektion |
|---|---|---|---|---|
| Dagsvandringar | 1 200 m | 5 | 40 | 8 |
| Flerdygn och läger | 2 500 m | 8 | 79 | 10 |
| Höghöjdsvandring | 3 800 m | 5 | 44 | 8 |
| Långleder och nödlägen | 4 500 m | 3 | 26 | 8 |

38 måltoppar och rutter totalt: 30 för högfjäll, från Aotaina på 5 211 m upp till de fjorton 8 000-meterstopparna, och 8 för vandring, från Huihangleden till Three Passes-rutten vid Everest.

## Var innehållet kommer från

Två huvudflöden förser frågorna och handboken.

Bland de offentliga källorna finns Wilderness Medical Societys uppdatering 2024 av de kliniska riktlinjerna för akut höjdsjuka, CDC Yellow Book, tekniskt material från American Alpine Institute och American Alpine Club, REI Expert Advice, UIAA:s och CE:s utrustningsstandarder samt lavindata från avalanche.org.

De tre guidehandlingarna från SEVEN SUMMIT TREKS — höjdsjuka, utrustning, allmän expedition — är det andra flödet.

Senare bearbetades korrekturlästa utskrifter av sju kinesiska bergsböcker in i handboken. Kunskap och fall är omskrivna med egna ord, med sidhänvisningar; den skannade källtexten förvaras inte i repot.

| Källa | Används till |
|---|---|
| WMS / CDC / StatPearls | Höjdmedicinska frågor och handbok |
| AAI / AAC / REI / BMC | Snö-, is- och rörelseteknik |
| UIAA / CE-standarder | Utrustningsfrågor |
| avalanche.org | Lavinbedömning |
| SEVEN SUMMIT TREKS handlingar | Handbok h17–h22 och relaterade frågor |
| 7 kinesiska bergsböcker | Handbok h23–h31 |

## Säkerhetsgräns

Appen täcker bara teori och kognitiv förberedelse. Praktiska färdigheter — snö och is, reparbete, isklättring, räddning — måste läras ut och bedömas på plats av en certifierad instruktör.

Inget här är medicinsk rådgivning. Beslut om läkemedel och första hjälpen hör till en läkare.

Ingen topp utlovas och inget acklimatiseringsresultat garanteras.

Lektioner som kräver instruktör säger det uttryckligen när teorin är klar.

## Bygga själv

Hela verktygskedjan kan ligga i hemkatalogen — ingen Homebrew, ingen Android Studio. Det här använder maskinen:

- `~/android-toolchain/jdk`: Temurin OpenJDK 17
- `~/android-toolchain/sdk`: Android SDK med platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9`: Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

Repot innehåller också Gradle 8.9-wrappern, så en maskin utan Gradle fungerar också:

```bash
cd AscentApp
./gradlew assembleDebug
```

Resultatet hamnar i `app/build/outputs/apk/debug/app-debug.apk`.

## Projektstruktur

```
AscentApp/
├── import/                      Frågeförfattande (Markdown, från tryckta böcker)
├── knowledge/                   Kunskapsbas för de tryckta källorna
├── tools/
│   ├── contentcheck/            Innehållskontroller + bankexport (ren JVM)
│   ├── i18ncheck.py             Täckning per språk över alla fyra lager
│   ├── qbank/build.mjs          Markdown → frågebank-kompilator
│   └── poc3d/                   Modellsynk, ikonurklippning, emissive-bakning
└── app/src/main/
    ├── res/values*/             Gränssnittstexter för 10 språk
    ├── assets/
    │   ├── badges/*.glb         40 emblemmodeller
    │   └── env/studio_ibl.ktx   Procedurellt genererad IBL-miljö
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt      Innehåll för huvudlektionerna
        │   ├── NewLessons.kt    Lektioner från den nya kursplanen
        │   ├── i18n/Pack*.kt    Innehållsöversättningar (ingång / punkter / handbok / bank)
        │   ├── Diagnostics.kt   Kraschlogg + miljöögonblick för feedbackformuläret
        │   ├── FeedbackSender.kt  Formspree-POST (HttpURLConnection, inga beroenden)
        │   ├── BundledBank.kt   Sammansättning av lektionspoolen och insättning av repetitioner
        │   └── bank/            Bankkonfiguration, JSON-avkodning, nätverk, repository
        └── ui/                  Skärmar och komponenter (Badge3D.kt = Filament-scen, beskärning, ljus)
```

## Ändra innehåll

För att lägga till frågor i en befintlig lektion, redigera `data/Expansion*.kt`. För en ny lektion, skriv `Lesson(...)` i `data/NewLessons.kt` och lägg till dess id i rätt enhet i `Curriculum.kt`. Ordning och enhetstillhörighet finns bara i `Curriculum.kt`.

Kör sedan kontrollerna:

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

Det första skriptet synkar appens innehåll till ett rent JVM-projekt, kör den riktiga sammansättningslogiken och kontrollerar frågeantal, svårighetsutveckling, svar utanför intervallet, tomma frågor, upplåsningslogik och prestationsdefinitioner, och exporterar därefter `bank/bank.json` och `bank/manifest.json`. Det andra rapporterar täckning per språk över de fyra lagren och jämför nycklarna. Ingen av dem behöver en emulator.

Efter ändringar i banken, höj `BUNDLED_VERSION` i `data/bank/BankConfig.kt`, annars tror klienterna att inget har hänt.

## Onlineuppdatering av banken

Banken har tre lager: lokal cache, medföljande och onlinedelta. Läsordningen är cache → medföljande; onlinekopian skriver bara över cachen när den är nyare.

Så ett flygplan, en dal utan täckning, en död server, trasig JSON, en sha256 som inte stämmer eller en äldre version lämnar allt som det är. Uppdateringar kontrolleras tyst efter start, högst var sjätte timme och bara när en adress är angiven.

Publicera en ny bank: ändra innehållet → höj `BUNDLED_VERSION` → kör `contentcheck` → lägg `bank/bank.json` och `bank/manifest.json` på en server eller CDN → appen tar den vid nästa start, utan ny version.

Äldre appversioner går inte sönder av en nyare bank. `minAppVersion` i `manifest.json` stoppar för gamla klienter, okända fält ignoreras och en trasig fråga hoppas över i stället för att vara fatal.

## Senaste ändringar

**v0.20.19 – v0.21.0: lerytor**

- Korten är av lera: fyra lager per kort — yttre skugga, kantljus, en kort högdager upptill och en inre avskuggning. Skuggan ritas med `BlurMaskFilter` utanför skärmen och cachas per storlek och radie. `Modifier.blur()` kräver API 31 och projektet går från 26; `Modifier.shadow()` klarar inte en färgad tvålagersskugga
- Fem skärmar gjordes om: profil, repetition, träning och handbok, därefter sidan ”Mitt”. Högdagern drogs tillbaka till en 20 dp läpp, konturen gick från 1 fysisk pixel till dp och skuggan delades i volym plus kontakt — innan dess såg korten halvtransparenta ut eftersom högdagern åt upp kanterna
- Varje handbokskategori har egen färg (utrustning stålblå, rep turkos, snö och is indigo, medicin röd, bedömning bärnsten, planering grön, ledarskap magenta); rutorna är platt fyllda utan gradient och detaljsidans omslag tar kategorifärgen
- Flera upplåsta prestationer i följd lägger sig inte längre över varandra: 3D-vyn byggs om per prestations-id och bakgrunden är ogenomskinlig mellan dialogerna

**v0.20.18: nytt Everest-diorama**

- Modellen för ”Climbing Grand Slam” är ombyggd kring detaljerna: böneflaggorna är nu 0,42 × 0,26 mm (tidigare osynliga i mobilen), deras snören vilar på dubbla stenrösen och varje flagga kontrolleras mot snön, och små delar samplar terrängnätet direkt i stället för att stråla mot hela scenen
- Ikonen kommer från den nya hero-renderingen; pose och utsnitt oförändrade

**v0.20.17: inget oavsiktligt snurr**

- När ett finger lyftes efter nypningen lästes det som en-fingerdragning, med referenspunkten en handbredd bort: emblemet hoppade och hoppet följde med i trögheten. Gesten är nu en tillståndsmaskin: när tvåfingersgesten börjat ignoreras det kvarvarande fingret, och en nypning lämnar ingen tröghet
- Inget svajar automatiskt när du är inzoomad, och återgången till framsidan är en mjuk vridning i stället för ett hopp

**v0.20.16: zooma in på detaljerna**

- Nypningen gav högst 1,25× förstoring, så toppnamn och höjdtal förblev oläsbara på mobilen. Nu går det till 5,5×
- Med två fingrar på konstant avstånd panoreras vyn, så du når hörnen; ett finger roterar fortfarande. Dubbeltryck återställer zoom och pan, och en kort gestguide finns under emblemtiteln

**v0.20.15: alla fyrtio emblem är i 3D**

- De sista sex anländer: Eight K Grad (studentmössa), Final Exam (toppmässing med flagga), Five Zones (platta formad som en löparsula), Fourteen Peaks (2,5:1-panorama över alla fjorton åttatusenmeterstoppar), Two Tracks (bricka med två räls) och Full Route (ett terrängblock av Everest — den enda pjäsen som inte är en medalj)
- Varje emblems ikon är nu modellens egen rendering, så de tecknade reservbilderna lämnar väggen
- Full Route behöver egen pose: den står på sin sockel, så 90°-vridningen för platta medaljer hoppas över och plaketten vrids mot kameran; renderingen ligger på grått i stället för svart, så urklippningen görs mot det grå
- Modeller 34 → 40; paketet 39,4 → 40,6 MB

**v0.20.14: emblemen öppnas överallt**

- Du behöver inte längre gå via utvecklartestytan: tryck på ett upplåst emblem i ”Min profil” eller på emblemväggen så öppnas det i fullskärm. Vyn läggs ovanpå appen i stället för att ersätta sidan, så du kommer tillbaka till samma skärm — inklusive listans position — och den nedre listen döljs
- ”Trail Finish” visade fortfarande den delade platshållaren trots att modellen redan fanns; mappningen pekar nu på den renderade ikonen
- ”Two Tracks” hade ett fotografi av en person som ikon — en kvarglömd platshållare. Nu är det en ritad platta (marinblå emalj, mässingsram, två streckade leder mot toppen), och fotot finns inte längre i paketet

**v0.20.13: sex emblemmodeller till**

- Biblioteket växer från 28 till 34: 6 000 m lärande, comeback, sju dagars och tre veckors spår, tjugo rundor och en D-formad karbinhake för ”säkerhet först”; self_arrest omexporterad
- Ikonurklippningen tar nu även bort bakgrund som *omsluts* av emblemets form — karbinhakens mitthål och en stansad kortets hål blev tidigare solida svarta skivor. Varje sluten yta bedöms för sig: jämn färg som matchar bakgrunden räknas som hål, gradienter behålls. Paketet 38.5 → 39.4 MB

**v0.20.11: ljus och bakgrund i 3D-emblemvyn**

- Emblemvyn hade två ljus, så metallen fick bara en enda böjd glans och reliefen låg platt. Nu är det fem riktade ljus — huvud, topp, frontfyllnad, kant och ett svagt studs underifrån — med samma totala belysning: mer djup utan att blekas ur
- En andra, mörkare bakgrund (en synthwave-solnedgång kraftigt suddad till enbart färgfält) passar mörkt läge. Pillret uppe till höger växlar mellan ljust och mörkt, följer systeminställningen som standard och minns valet; bara den här skärmen påverkas

**v0.20.8 – v0.20.10: startskärm**

- Startskärmen är nu i lera: en vit skiva med åtstramad skuggning och skarpa kanter, toppmärket och i stället för den gamla raden ett slumpvis citat av en bergsbestigare — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe eller Bonatti. Den tonar ut efter cirka 1,7 s och hoppas nästan över med reducerad rörelse. Inget annat i appen har ändrats Från v0.20.9 visar systemets startskärm ingen logotyp alls — starttemat är bara samma bakgrundsfärg, så ler-skärmen börjar på ett tomt vitt (eller nästan svart) fält. Varje citat står på originalspråket

**v0.20.6 – v0.20.7: renderingskvalitet för emblemen**

- Bloom körs som standard i högsta kvalitet — 256 px, fyra nivåer, MEDIUM. Den gamla automatiska nedväxlingen dömde från allra första bildrutan, så några hundra millisekunder av shaderkompilering räckte för att stänga av den resten av passet. Nu väntar den 2,5 s, tillåter bildrutor på 50 ms (20 fps) under tre fönster i rad och trappar ned i två steg: full → låg → av
- 4x MSAA är på där drivrutinen tillåter det, med kapaciteten kontrollerad innan swap chain skapas; misslyckas försöket ritas det utan MSAA och misslyckandet kommer ihåg — en antialiasinställning ska aldrig göra att ett emblem inte syns

**v0.20.4 – v0.20.5: små rättelser**

- Utvecklarläget är osynligt tills det låses upp — hela avsnittet i inställningarna, inklusive "Av · tryck 7 gånger på versionsnumret", ritas inte längre för vanliga användare. Räknaren för sju tryck fungerar fortfarande, men räknar inte längre högt
- Scrolläget överlever också inne i testytan för prestationer: gå in i ett emblems 3D-vy och tillbaka, och från inställningarna in i testytan och tillbaka
- Upplåsningsdialogen stängs inte längre av ett snedtryck: första trycket på bakgrunden spänner bara upp den och tonar in "Tryck igen för att stänga"; det andra stänger. "Ta emot" och bakåtgesten stänger fortfarande direkt

**v0.20.0 – v0.20.3: 3D-emblem**

- 34 prestationer renderas i 3D i realtid (Filament 1.75, glTF, metall och emalj i PBR). Dra för att rotera, nyp för att zooma, det gungar sakta när du släpper; den automatiska rörelsen stannar på framsidan eftersom baksidan av en medalj är en slät platta
- Sedan v0.20.15 har alla fyrtio en modell; det lutande kortet är reserv för framtida emblem
- Upplåsnings- och förhandsvisningsdialoger går genom samma renderare, med en ljus piedestal bakom modellen (en mörk modell på ett mörkt kort försvinner) och 56 band från båda kanterna. "Minska rörelse" stänger av banden
- Visningssidan vilar på ett kraftigt suddigt landskap, procedurellt genererat (27 kB): himmel, fyra åsar, dimma, inga fotolicenser att tänka på
- Dragriktningarna inverterade efter feedback; hela scenen ljusades upp och drogs sedan tillbaka lite så att emittersande delar behåller kontrasten
- Ljus som skapats inne i modellerna överlever nu: `KHR_lights_punctual`-entiteter läggs uttryckligen till i scenen (glTF-tillgångens entiteter innehåller dem inte) och deras intensitet skalas till scenens exponering — 54 cd bredvid ett huvudljus på 66 000 lux syns annars inte. `KHR_materials_emissive_strength` bakas in i `emissiveFactor` vid bygget, och bloom är på och stängs av automatiskt när bildtiden ligger över 28 ms
- Modellbiblioteket växte från 17 till 34 under den här perioden: serier, fullpoäng, repetition, reträtt, vandringsmål och tre omexporteringar tillkom; paketet 21 → 39 MB
- **Emblemikonerna kommer nu från modellerna**: frontrenderingen skärs ut ur sin svarta bakgrund till en transparent ikon på 256 px, så väggen, upplåsningsdialogen och 3D-vyn visar samma föremål. AI-illustrationerna för dem togs bort
- Spotlight-förstärkningen förenklad till 5× (modellens egen spotlight riktar in i medaljkroppen, ingen förstärkning gör den synlig framifrån — det måste rättas i Blender-källan), bloom nedskruvad till 0,05
- v0.20.3: en listas scrolläge överlever en tur till en underskärm (inställningar → språk, handbok → artikel, profil → inställningar, repetition → lektion), och att gå tillbaka från en lektion nollställer inte längre ruttens sidbläddrare till första sidan

**v0.19.25 – v0.19.30: arbete på appen**

- Prestationerna anpassade till banken med 89 lektioner. `full_route` betyder nu alla 68 högfjällslektioner — tidigare stod det 66 och bedömningen gjordes mot det spår som var öppet, så att bara klara vandringen gav "teoretisk grand slam". `five_zones` blev "sex zoner", i linje med de sex högfjällsenheterna. Fem vandringsprestationer tillagda, 35 → **40**
- Prestationsprogressen räknas över båda spåren i stället för det valda, så att byta spår inte längre flyttar målet
- Spårikonerna omritade som solida silhuetter — snötäckt topp och vandringskänga — i stället för `▲` / `≡` i arket; pillerna högst upp bär samma två i 14 dp
- Splash-logotypen nedskalad 40 %
- Feedbackskärm i profilen som skickar till Formspree med e-post- och urklippsreserv; krashhanteraren behåller de fem senaste ohanterade undantagen i appens privata lagring
- Profilen och startsidan räknade det totala antalet avklarade lektioner mot det aktuella spårets antal och visade "68 / 21" efter byte; rättat

v0.19 är innehållsutbyggnaden; bankens struktur och poänglogiken är oförändrade.

- Lektioner 47 → 89: högfjäll nu 68 (enheterna 6000 / 7000 / 8000 m, alla fjorton 8 000-meterstoppar), plus 21 nya vandringslektioner
- Frågor 711 → 1 533 (9 422 nycklar), handbok 31 → 35 artiklar (lavinbegravning och återupplivning, lavintrion, fasta rep, flodvad), bank v15 → v19
- **Nio språk nu kompletta över alla fyra innehållslager**: engelska, traditionell kinesiska, japanska, ryska, tyska, spanska, franska, svenska och italienska. Deras nyckeluppsättningar matchar engelskan (10 554 / 10 554), inget faller tillbaka i drift
- Tyskan använder opersonlig man-form, spanskan tilltalar med tú, franskan med vous, svenskan och italienskan med du / tu; varje språk följer sina sifferkonventioner — tyska, spanska och italienska skriver 1.000 m, franska och svenska 5 000 m, alla med decimalkomma
- Kvarblivna nycklar från tidigare innehållsändringar (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) togs bort från alla nio språk, så att varje uppsättning är ren och identisk
- Traditionell kinesiska går genom OpenCC s2twp plus en terminologitabell, validerad med regression mot befintliga översättningar (noll avvikelser); japanskan är en riktig översättning, med ett register för frågebanken och ett för handboken
- `tools/i18n/` erbjuder tre kommandon (`status.py` / `prep.py` / `apply.py`) för alla nio språk; traditionell kinesiska har dessutom `tw.py` och `tw_entry.py`
- Banken är delad över filer: en enda Kotlin-fil på cirka 2 500 poster får kompilatorn att falla, så vart och ett av de nio översatta språken är delat i tre (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

Från v0.16 till v0.18 var allt flerspråksarbete; bankens struktur och poänglogiken ändrades inte.

- Gränssnittstexterna delade i 10 `res/values*`-uppsättningar; språket kan ställas in manuellt eller följa systemet och slår igenom direkt
- Nytt innehållslager `data/i18n/` med fyra paket per språk: ingång, huvudpunkter, handbokstext, frågebank. 4 727 poster per språk
- Alla fyra lager klara i de nio översatta språken (zh-TW, en, ja, de, fr, es, it, ru, sv); de åtta utöver engelska verifierade nyckel för nyckel mot källan, noll saknade
- Terminologin anpassad till varje språks bruk; doser, lavinnivåer, sluttningar och temperaturer lämnade som de är
- `tools/i18ncheck.py` tillagd så att ett kommando visar täckningen över alla fyra lager — slut på "översatt, men en lektion missades"
- En del texter rättade på vägen: ett komma-före-tankstreck-fel (140 på svenska, 5 vardera på tyska och spanska), två lucktextsvar med överflödigt citattecken och tre franska decimaler

Tidigare:

- v0.15.1: textstädning i appen, bokstavliga `**`-markörer borttagna, en rad förklaringar omskrivna, inloggningssidan lovar inte längre någon synkronisering som inte finns
- v0.15.0: nio artiklar till ur kinesiska böcker (h23–h31), ny kategori "ledarskap ute", 22 → 31 artiklar
- v0.14.0: sex guideartiklar från SEVEN SUMMIT TREKS
- v0.13.x: inställningar, profilsida, emblemvägg; bakåtgest på underskärmar

## Licens

PolyForm Noncommercial License 1.0.0 — endast icke-kommersiell användning. Kommersiell användning kräver separat skriftligt tillstånd; se `LICENSE` och `NOTICE`.

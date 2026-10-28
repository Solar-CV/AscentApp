# ASCENT

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Français](README.fr.md) · **[Italiano](README.it.md)** · [Русский](README.ru.md) · [Svenska](README.sv.md)

Un'app Android per chi comincia con l'alpinismo d'alta quota e l'arrampicata su ghiaccio. Il programma è ordinato per quota: ogni lezione superata ti porta un po' più in alto.

Due percorsi si cambiano liberamente. **Alta montagna** ha sei unità e 68 lezioni, dall'introduzione a 5.000 m fino a 8.000 m — da 3.200 m a 8.848 m da un capo all'altro. **Escursionismo** ha quattro unità e 21 lezioni, dalla camminata di un giorno ai percorsi lunghi — da 200 m a 5.630 m. Ogni percorso tiene per sé progressi, badge e coda di ripasso.

Versione attuale v0.20.14 (versionCode 121). Il lavoro sulle lingue si è chiuso con la v0.19.24 — le nove lingue tradotte corrispondono all'inglese voce per voce (10.554 / 10.554). La v0.20 riguarda i badge: 34 obiettivi sono ora resi in 3D in tempo reale invece che come immagini piatte, e le loro icone vengono dagli stessi modelli — le vecchie illustrazioni generate dall'IA non ci sono più. La finestra di sblocco usa lo stesso motore e lancia coriandoli dai due bordi dello schermo. I modelli viaggiano dentro l'APK, quindi il pacchetto è passato da 21 MB a circa 39 MB. La v0.20.3 sistema un fastidio vecchio: scorri una lista, apri una schermata secondaria, torni, e la lista saltava in cima — ora resta dove l'hai lasciata. Le v0.20.6–v0.20.7 portano il rendering dei badge alla qualità massima per impostazione predefinita: bloom più fine che non si spegne più per i primi fotogrammi lenti, declassamento in due passi e MSAA 4x con verifica della capacità e ritorno silenzioso.

## Installazione

La via più semplice è l'APK debug già compilato nelle [Releases](https://github.com/Solar-CV/AscentApp/releases). Un tag `v*` fa compilare a GitHub Actions un APK nuovo e lo allega.

- Pacchetto: `com.ascent.climb`
- Minimo: Android 8.0 (API 26), **solo arm64-v8a** — i telefoni solo a 32 bit non possono installarlo
- Dimensione: circa 39 MB (32 MB di modelli non compressi, circa 9 MB dopo il confezionamento)
- Permessi: rete e stato della rete — per controllare gli aggiornamenti del questionario e inviare feedback. Tutto funziona offline; il questionario incluso c'è sempre

Trasferisci l'APK sul telefono, consenti l'installazione da origini sconosciute, tocca installa. Al primo avvio non serve accedere.

## Cosa c'è dentro

### Il percorso

La schermata iniziale è una via che sale dal campo base, divisa in unità di quota (sei per l'alta montagna, quattro per l'escursionismo). Si sfoglia in orizzontale, due unità per pagina. Le lezioni si sbloccano in linea: una superata, la successiva si apre. I nodi mostrano le stelle, la lezione attuale porta un invito ad «iniziare», l'ultima di ogni unità è segnata in oro.

Due pillole in alto cambiano percorso; un tocco apre un foglio inferiore. Cambiando si torna alla prima pagina e la scelta resta memorizzata.

Il progresso non è un numero salvato. Si ricalcola dalle lezioni completate, così se la curva di quota viene ritoccata più avanti, i salvataggi vecchi si ricalcolano invece di restare bloccati.

### Questionario

1.533 domande ufficiali più 154 copie di ripasso inserite automaticamente, 1.687 elementi mostrati. Il volume cresce con la quota: circa 15 domande per lezione nell'introduzione a 5.000 m, 30 nelle unità tecniche a 7.000 m, 8 nell'escursionismo.

Otto tipi di domanda: scelta singola, scelta multipla, ordinamento, abbinamento, completamento, calcolo, punto caldo (toccare un punto dell'immagine) e scenario (un testo affrontato in più passaggi).

Finita una lezione si passa subito a rifare quelle sbagliate. Quelle poi corrette danno credito, le altre finiscono nel centro di ripasso. Il ripasso usa la ripetizione diluita a 0, 1, 3, 7, 16 e 35 giorni. Le domande critiche per la sicurezza sono pesate e chiedono di confermare la comprensione prima di proseguire.

### Manuale

35 articoli lunghi, da 4 a 8 sezioni, divisi per attrezzatura, tecnica, medicina, salute, meteo, meteorologia, decisioni, valutazione, sicurezza, rischio e soccorso in valanga. Il manuale è filtrato per percorso: i pezzi su neve e ghiaccio compaiono solo in alta montagna, mentre medicina d'alta quota, ipotermia, navigazione, campo e conduzione del gruppo sono condivisi. Le fonti sono materiale medico e tecnico pubblico, tre documenti commerciali di guide e trascrizioni rielaborate di sette libri alpinistici cinesi. Per accordo di prodotto i libri cinesi non sono citati per titolo; i documenti delle guide sì.

### Allenamento, ripasso, profilo

L'allenamento dà una percentuale di preparazione e la quota della vetta obiettivo. Il ripasso mette davanti le domande in scadenza oggi e gli errori critici di sicurezza. Il profilo raccoglie l'intestazione, la quota attuale, il quadro delle capacità, l'accesso ai badge, l'area account e una voce di feedback. L'ingranaggio apre le impostazioni, l'avatar la pagina del profilo.

Il profilo mostra solo i badge sbloccati. Tutti e 40 stanno nella parete dei badge, quelli bloccati come anteprima grigia, gli obiettivi nascosti come «?».

La maggior parte esiste anche come vero modello 3D — cornici in metallo, smalti, rilievo. Apri l'area di test degli obiettivi e tocca un badge per vederlo girare: trascina per ruotare, pizzica per ingrandire, e quando lasci si dondola piano da solo. Le icone vengono dai modelli stessi, quindi parete e vista 3D mostrano lo stesso oggetto; le vecchie illustrazioni IA sono state ritirate. Gli altri dodici senza modello tengono il disegno a mano e compaiono come scheda inclinata in prospettiva. Sbloccare un obiettivo in qualsiasi punto dell'app mostra lo stesso badge 3D, su un piedistallo chiaro, con i nastri che entrano dai due bordi.

### Impostazioni

L'aspetto segue il sistema o si fissa su chiaro o scuro, viene salvato e la barra di stato lo segue. La lingua segue il sistema o si sceglie a mano; interfaccia e contenuti cambiano insieme, senza riavvio. La sezione questionario mostra la versione attuale, cerca aggiornamenti e ripristina quello incluso. La modalità sviluppatore sta dietro al numero di versione in «Informazioni»: sette tocchi.

### Feedback

Profilo → Feedback e suggerimenti, tra la scheda account e l'avviso di sicurezza. Scegli un tipo — problema, idea, correzione di contenuto —, scrivi il messaggio, lascia facoltativamente un indirizzo per la risposta e allega facoltativamente la diagnostica. L'invio arriva direttamente al manutentore tramite Formspree; senza rete la schermata propone di mandare lo stesso testo per email e lo copia negli appunti se non esiste un'app di posta.

La diagnostica è spenta per impostazione predefinita e comprende versione dell'app, versione di Android, modello del dispositivo, lingua, versione del questionario, percorso attuale, numeri di progresso e l'ultima eccezione non gestita. Niente testo delle lezioni, niente storico delle risposte, niente account, niente appunti.

## Dieci lingue

I testi dell'interfaccia e i contenuti del corso sono disponibili in:

简体中文 (originale), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

Tutte e dieci le lingue sono complete. Ognuna ha il proprio strato d'ingresso, i riassunti delle lezioni, il corpo del manuale e il questionario, e ogni insieme di chiavi corrisponde all'inglese voce per voce — **10.554 / 10.554**, niente manca, niente è duplicato, nessuna chiave orfana.

| Strato | Voci | Le 9 lingue tradotte |
|---|---|---|
| Ingresso (lezioni, unità, titoli del manuale, vette) | 117 | 100 % |
| Punti chiave delle lezioni | 443 | 100 % |
| Corpo del manuale | 378 | 100 % |
| Questionario (testo, opzioni, spiegazione) | 9.422 | 100 % |

La terminologia segue l'uso alpinistico di ogni lingua invece della traduzione letterale: svedese stegjärn / isyxa / standplats, tedesco Steigeisen / Pickel, russo кошки / ледоруб, italiano corda fissa / bloccante / sosta. Dosi mediche, gradi di valanga, pendenze e temperature restano invariati — si adatta solo il formato dei numeri, e le tre convenzioni sono tenute separate di proposito:

- Inglese, cinese, giapponese: `6.5 °C`, `5,000 m`
- Tedesco, spagnolo, italiano: `6,5 °C`, `1.000 m` (virgola decimale, punto delle migliaia)
- Francese, svedese: `6,5 °C`, `5 000 m` (virgola decimale, spazio delle migliaia)

Cambia anche il modo di rivolgersi a chi legge: il tedesco usa la forma impersonale con *man*, lo spagnolo dà del *tú*, il francese del *vous*, lo svedese e l'italiano del *du* / *tu*, mentre i riassunti delle lezioni restano impersonali in tutte.

Se una voce manca nella lingua scelta, l'ordine è: lingua scelta → inglese → cinese originale. Quel percorso ormai è solo una rete di sicurezza.

## Programma

**Alta montagna**

| Unità | Quota | Lezioni | Domande | Per lezione |
|---|---|---|---|---|
| 5000M · Introduzione | 5.000 m | 16 | 249 | 16 |
| 5000M · Progressione | 5.300 m | 12 | 221 | 18 |
| 6000M | 6.000 m | 14 | 316 | 22 |
| 7000M · Campo tecnico C3 | 7.000 m | 6 | 185 | 31 |
| 7000M · Campo alto C4 | 7.500 m | 10 | 279 | 28 |
| 8000M | 8.000 m | 10 | 248 | 24 |

**Escursionismo**

| Unità | Quota | Lezioni | Domande | Per lezione |
|---|---|---|---|---|
| Camminate di un giorno | 1.200 m | 5 | 40 | 8 |
| Più giorni e campeggio | 2.500 m | 8 | 79 | 10 |
| Escursionismo d'alta quota | 3.800 m | 5 | 44 | 8 |
| Percorsi lunghi ed emergenze | 4.500 m | 3 | 26 | 8 |

38 vette e itinerari in tutto: 30 per l'alta montagna, dall'Aotaina a 5.211 m ai quattordici 8.000; 8 per l'escursionismo, dal sentiero di Huihang alla via dei Tre Passi all'Everest.

## Da dove vengono i contenuti

Due flussi principali alimentano domande e manuale.

Le fonti pubbliche includono l'aggiornamento 2024 delle linee guida cliniche della Wilderness Medical Society sul mal acuto di montagna, il CDC Yellow Book, materiale tecnico dell'American Alpine Institute e dell'American Alpine Club, REI Expert Advice, le norme UIAA e CE per l'attrezzatura e i dati valanghe di avalanche.org.

I tre documenti guida di SEVEN SUMMIT TREKS — mal di montagna, attrezzatura, pratica generale di spedizione — sono il secondo flusso.

Più avanti, trascrizioni corrette di sette libri alpinistici cinesi sono state rielaborate nel manuale. Conoscenze e casi sono riscritti con parole nostre, con riferimenti di pagina; il testo scansionato non è conservato nel repository.

| Fonte | Usata per |
|---|---|
| WMS / CDC / StatPearls | Domande e manuale di medicina d'alta quota |
| AAI / AAC / REI / BMC | Tecnica su neve, ghiaccio e progressione |
| Norme UIAA / CE | Domande sull'attrezzatura |
| avalanche.org | Valutazione delle valanghe |
| Documenti SEVEN SUMMIT TREKS | Manuale h17–h22 e domande collegate |
| 7 libri alpinistici cinesi | Manuale h23–h31 |

## Limite di sicurezza

L'app copre solo la teoria e la preparazione mentale. Le abilità pratiche — neve e ghiaccio, corde, arrampicata su ghiaccio, soccorso — vanno insegnate e valutate sul posto da un istruttore certificato.

Niente di ciò che è scritto qui è consiglio medico. Le decisioni su farmaci e primo soccorso spettano a un medico.

Nessuna vetta è promessa e nessun esito di acclimatazione è garantito.

Le lezioni segnate come «serve un istruttore» lo dicono apertamente una volta finita la teoria.

## Compilarlo da soli

L'intera toolchain può stare nella cartella personale — niente Homebrew, niente Android Studio. Quello che usa questa macchina:

- `~/android-toolchain/jdk`: Temurin OpenJDK 17
- `~/android-toolchain/sdk`: Android SDK con platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9`: Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

Il repository include anche il wrapper di Gradle 8.9, quindi una macchina senza Gradle funziona lo stesso:

```bash
cd AscentApp
./gradlew assembleDebug
```

Il risultato finisce in `app/build/outputs/apk/debug/app-debug.apk`.

## Struttura del progetto

```
AscentApp/
├── import/                      Scrittura delle domande (Markdown, da libri stampati)
├── knowledge/                   Base di conoscenza delle fonti stampate
├── tools/
│   ├── contentcheck/            Controlli sui contenuti + export del questionario (JVM pura)
│   ├── i18ncheck.py             Copertura per lingua sui quattro strati
│   ├── qbank/build.mjs          Compilatore Markdown → questionario
│   └── poc3d/                   Sincronia dei modelli, scontorno delle icone, cottura dell'emissivo
└── app/src/main/
    ├── res/values*/             Testi dell'interfaccia per 10 lingue
    ├── assets/
    │   ├── badges/*.glb         34 modelli di badge
    │   └── env/studio_ibl.ktx   Ambiente IBL generato via codice
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt      Contenuti delle lezioni principali
        │   ├── NewLessons.kt    Lezioni del programma recente
        │   ├── i18n/Pack*.kt    Traduzioni dei contenuti (ingresso / punti / manuale / questionario)
        │   ├── Diagnostics.kt   Registro dei crash + istantanea dell'ambiente per il feedback
        │   ├── FeedbackSender.kt  POST a Formspree (HttpURLConnection, nessuna dipendenza)
        │   ├── BundledBank.kt   Assemblaggio del pool di lezioni e inserimento dei ripassi
        │   └── bank/            Configurazione del questionario, decodifica JSON, rete, repository
        └── ui/                  Schermate e componenti (Badge3D.kt = scena Filament, inquadratura, luci)
```

## Modificare i contenuti

Per aggiungere domande a una lezione esistente, modifica `data/Expansion*.kt`. Per una lezione nuova, scrivi `Lesson(...)` in `data/NewLessons.kt` e aggiungi il suo id all'unità giusta in `Curriculum.kt`. Ordine e appartenenza alle unità vivono solo in `Curriculum.kt`.

Poi lancia i controlli:

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

Il primo script sincronizza i contenuti dell'app in un progetto JVM puro, esegue la vera logica di assemblaggio e controlla numero di domande, progressione di difficoltà, risposte fuori intervallo, domande vuote, logica di sblocco e definizioni degli obiettivi, quindi esporta `bank/bank.json` e `bank/manifest.json`. Il secondo riporta la copertura per lingua sui quattro strati e confronta le chiavi. Nessuno dei due richiede un emulatore.

Dopo aver toccato il questionario, alza `BUNDLED_VERSION` in `data/bank/BankConfig.kt`, altrimenti i client crederanno che nulla sia cambiato.

## Aggiornamenti online del questionario

Il questionario ha tre strati: cache locale, incluso e delta online. L'ordine di lettura è cache → incluso; la copia online sovrascrive la cache solo se è più recente.

Quindi un aereo, una valle senza segnale, un server morto, un JSON malformato, uno sha256 che non torna o una versione più vecchia lasciano tutto com'è. Gli aggiornamenti si controllano in silenzio dopo l'avvio, al massimo ogni sei ore e solo se è configurato un URL.

Per pubblicare un questionario nuovo: cambia i contenuti → alza `BUNDLED_VERSION` → esegui `contentcheck` → carica `bank/bank.json` e `bank/manifest.json` su un server o una CDN → l'app lo prende al lancio successivo, senza pubblicare una versione.

Le versioni vecchie dell'app non si rompono con un questionario più nuovo. `minAppVersion` in `manifest.json` blocca i client troppo vecchi, i campi sconosciuti vengono ignorati e una domanda malformata viene saltata invece che essere fatale.

## Cambi recenti

**v0.20.14: i badge si aprono ovunque**

- Non serve più passare dall'area di test sviluppatore: tocca un badge sbloccato in «Il mio profilo» o nella bacheca e si apre a schermo intero. La vista si sovrappone all'app invece di sostituire la pagina, quindi tornando indietro resti dov'eri — posizione della lista compresa — e la barra in basso resta coperta
- «Trail Finish» mostrava ancora l'icona segnaposto condivisa anche se il modello era già arrivato; ora la mappatura punta all'icona renderizzata. «Two Tracks» non ha ancora un modello e mantiene il segnaposto

**v0.20.13: sei modelli di badge in più**

- La libreria passa da 28 a 34: 6.000 m di apprendimento, ritorno, impronte di sette giorni e di tre settimane, venti corse e un moschettone a D per «la sicurezza prima»; self_arrest riesportato
- Il ritaglio delle icone ora toglie anche lo sfondo *racchiuso* dalla forma del badge — il foro centrale del moschettone e le perforazioni di una scheda punzonata uscivano come dischi neri pieni. Ogni area chiusa viene giudicata da sola: colore uniforme uguale allo sfondo vale come foro, i gradienti restano. Pacchetto 38.5 → 39.4 MB

**v0.20.11: illuminazione e sfondo della vista 3D dei badge**

- La vista del badge aveva due sole luci: il metallo mostrava un solo riflesso curvo e il rilievo restava piatto. Ora sono cinque luci direzionali — chiave, zenitale, riempimento frontale, controluce e un debole rimbalzo dal basso — con l'illuminazione totale invariata: più profondità senza slavare
- Un secondo sfondo più scuro (un tramonto synthwave sfocato fino a lasciare solo campi di colore) accompagna il tema scuro. La pillola in alto a destra alterna chiaro e scuro, segue l'impostazione di sistema per impostazione predefinita e ricorda la scelta; riguarda solo questa schermata

**v0.20.8 – v0.20.10: schermata di avvio**

- La schermata di avvio diventa in argilla: un disco bianco con ombreggio stretto e bordi netti, il segno della vetta e, al posto del vecchio slogan, una citazione casuale di un alpinista — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe o Bonatti. Sfuma dopo circa 1,7 s e con le animazioni ridotte quasi non compare. Nient'altro nell'app è cambiato Dalla v0.20.9 lo splash di sistema non mostra più alcun logo: il tema di avvio è solo lo stesso colore di sfondo, quindi la schermata di argilla parte su un campo bianco (o quasi nero). Ogni citazione è nella lingua originale

**v0.20.6 – v0.20.7: qualità di rendering dei badge**

- Il bloom gira per impostazione predefinita alla qualità massima: 256 px, quattro livelli, MEDIUM. Il vecchio declassamento automatico giudicava fin dal primo fotogramma, quindi qualche centinaio di millisecondi di compilazione degli shader bastava a spegnerlo per tutta la sessione. Ora aspetta 2,5 s, tollera fotogrammi da 50 ms (20 fps) per tre finestre consecutive e scende in due passi: completo → ridotto → spento
- Il MSAA 4x è attivo dove il driver lo consente, con la capacità verificata prima di creare la swap chain; se il tentativo fallisce si torna a disegnare senza MSAA e il fallimento viene ricordato, perché un'impostazione di antialiasing non deve mai impedire a un badge di comparire

**v0.20.4 – v0.20.5: piccole correzioni**

- La modalità sviluppatore è invisibile finché non viene sbloccata: l'intera sezione delle impostazioni, compreso «Spento · tocca 7 volte il numero di versione», non viene più disegnata per l'utente normale. Il contatore dei sette tocchi funziona ancora, ma non conta più ad alta voce
- La posizione di scorrimento sopravvive anche dentro l'area di test degli obiettivi: entrare nella vista 3D di un badge e tornare, passare dalle impostazioni all'area di test e tornare
- La finestra di sblocco non si chiude più con un tocco di troppo: il primo tocco sullo sfondo la arma soltanto e mostra «Tocca di nuovo per chiudere»; il secondo chiude. «Accetta» e il gesto indietro chiudono ancora in un passaggio

**v0.20.0 – v0.20.3: badge 3D**

- 34 obiettivi sono resi in 3D in tempo reale (Filament 1.75, glTF, metallo e smalto PBR). Trascina per ruotare, pizzica per ingrandire, oscilla piano quando lasci; il movimento automatico resta sulla faccia frontale perché il retro di una medaglia è una lastra liscia
- Gli altri sei tengono l'illustrazione ferma, inclinata in prospettiva con angoli arrotondati e ombra: una carta da collezione, non un'immagine piatta. Passano al 3D appena arriva un modello
- Le finestre di sblocco e anteprima passano dallo stesso motore, con un piedistallo chiaro dietro il modello (un modello scuro su una scheda scura sparisce) e 56 nastri dai due bordi. «Riduci movimento» spegne i nastri
- La pagina di esposizione poggia su un paesaggio molto sfocato, generato via codice (27 KB): cielo, quattro creste, foschia, nessuna licenza fotografica da gestire
- Direzioni di trascinamento invertite dopo il feedback; la scena è stata schiarita e poi ripresa un po' perché le parti emissive conservino contrasto
- Le luci create dentro i modelli ora sopravvivono: le entità `KHR_lights_punctual` vengono aggiunte esplicitamente alla scena (le entità di un asset glTF non le includono) e la loro intensità è scalata all'esposizione — 54 cd accanto a una luce principale da 66.000 lux sono invisibili altrimenti. `KHR_materials_emissive_strength` viene cotto in `emissiveFactor` in fase di build, il bloom resta acceso e si spegne quando il tempo di frame supera i 28 ms
- La libreria di modelli è passata da 17 a 34 in questo tratto: serie, perfezione, ripasso, ritirata, fine escursionismo e tre riesportazioni; pacchetto 21 → 39 MB
- **Le icone dei badge ora vengono dai modelli**: il render frontale viene scontornato dal fondo nero in un'icona trasparente da 256 px, così parete, finestra di sblocco e vista 3D mostrano lo stesso oggetto. Le illustrazioni IA di quei badge sono state abbandonate
- Guadagno dello spot semplificato a 5× (lo spot del modello punta dentro il corpo della medaglia, nessun guadagno lo rende visibile di fronte — va sistemato nella sorgente Blender), bloom portato a 0,05
- v0.20.3: la posizione di una lista sopravvive al passaggio in una schermata secondaria (impostazioni → lingua, manuale → articolo, profilo → impostazioni, ripasso → lezione), e tornare da una lezione non riporta più il paginatore del percorso alla prima pagina

**v0.19.25 – v0.19.30: lavoro sull'app**

- Obiettivi riallineati al questionario da 89 lezioni. `full_route` ora significa tutte e 68 le lezioni di alta montagna — prima diceva 66 e giudicava sul percorso aperto, così finire il solo escursionismo dava il «grande slam teorico». `five_zones` è diventato «sei zone», in linea con le sei unità di alta montagna. Cinque obiettivi di escursionismo aggiunti, 35 → **40**
- Il progresso degli obiettivi è calcolato su entrambi i percorsi invece che su quello scelto: cambiare percorso non sposta più il traguardo
- Icone dei percorsi ridisegnate come sagome piene — vetta innevata e scarpone — al posto dei caratteri `▲` / `≡` nel foglio; le pillole in alto portano le stesse due a 14 dp
- Logo di avvio ridotto del 40 %
- Schermata di feedback nel profilo, che invia a Formspree con ripiego via email e appunti; il gestore dei crash tiene le ultime cinque eccezioni non gestite nello spazio privato dell'app
- Profilo e home contavano il totale delle lezioni completate contro quelle del percorso attuale, mostrando «68 / 21» dopo il cambio; corretto

La v0.19 è l'ampliamento dei contenuti; struttura del questionario e logica di punteggio non cambiano.

- Lezioni 47 → 89: alta montagna ora 68 (unità 6000 / 7000 / 8000 m, tutti e quattordici gli 8.000), più 21 nuove lezioni di escursionismo
- Domande 711 → 1.533 (9.422 chiavi), manuale 31 → 35 articoli (seppellimento e rianimazione in valanga, il trio valanga, corde fisse, guado), questionario v15 → v19
- **Nove lingue complete su tutti e quattro gli strati di contenuto**: inglese, cinese tradizionale, giapponese, russo, tedesco, spagnolo, francese, svedese e italiano. Le chiavi corrispondono all'inglese (10.554 / 10.554), a runtime non ricade nulla
- Il tedesco usa la forma impersonale con man, lo spagnolo dà del tú, il francese del vous, lo svedese e l'italiano del du / tu; ognuno segue le proprie convenzioni numeriche — tedesco, spagnolo e italiano scrivono 1.000 m, francese e svedese 5 000 m, tutti con la virgola decimale
- Le chiavi orfane di vecchie modifiche (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) sono state tolte da tutte e nove le lingue, lasciando ogni insieme pulito e identico
- Il cinese tradizionale passa da OpenCC s2twp più una tabella terminologica, validato per regressione contro le traduzioni esistenti (zero differenze); il giapponese è una traduzione vera, con un registro per il questionario e uno per il manuale
- `tools/i18n/` offre tre comandi (`status.py` / `prep.py` / `apply.py`) per tutte e nove le lingue; il cinese tradizionale ha in più `tw.py` e `tw_entry.py`
- Il questionario è diviso in file: un singolo file Kotlin da circa 2.500 voci fa cadere il compilatore, quindi ciascuna delle nove lingue tradotte è spezzata in tre (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

Dalla v0.16 alla v0.18 è stato tutto lavoro multilingue; struttura del questionario e logica di punteggio non sono cambiate.

- Testi dell'interfaccia divisi in 10 insiemi `res/values*`; la lingua si imposta a mano o segue il sistema, con effetto immediato
- Nuovo strato di contenuti `data/i18n/`, quattro pacchetti per lingua: ingresso, punti chiave, corpo del manuale, questionario. 4.727 voci per lingua
- Tutti e quattro gli strati completi nelle nove lingue tradotte (zh-TW, en, ja, de, fr, es, it, ru, sv); le otto oltre l'inglese verificate chiave per chiave contro la fonte, zero mancanti
- Terminologia allineata all'uso di ogni lingua; dosi, gradi di valanga, pendenze e temperature lasciati com'erano
- Aggiunto `tools/i18ncheck.py` per vedere la copertura dei quattro strati con un comando — basta «tradotto, ma manca una lezione»
- Sistemati anche alcuni testi: un errore di virgola prima del trattino (140 in svedese, 5 in tedesco e spagnolo), due risposte a completamento con un apice di troppo e tre decimali francesi

Prima:

- v0.15.1: pulizia dei testi nell'app, via i marcatori `**` letterali, riscritte diverse spiegazioni, la pagina di accesso non promette più una sincronizzazione non implementata
- v0.15.0: nove articoli di manuale da libri cinesi (h23–h31), nuova categoria «conduzione del gruppo», 22 → 31 articoli
- v0.14.0: sei articoli guida di SEVEN SUMMIT TREKS
- v0.13.x: impostazioni, pagina profilo, parete dei badge; gesto indietro sulle schermate secondarie

## Licenza

PolyForm Noncommercial License 1.0.0 — solo uso non commerciale. L'uso commerciale richiede un permesso scritto separato; vedi `LICENSE` e `NOTICE`.

#!/usr/bin/env python3
"""把八份语言 README 迁到新图文版式，并生成各语言的示意图变体。

旧章节文字原样复用（人工校对过的术语不动），只新写少量衔接文案；
示意图（loop / arch）按语言出变体，只换 <text> 内容。
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "assets/readme"

# ---------------------------------------------------------------- 各语言文案

L = {}

L["tw"] = dict(
    hero_alt="AscentRoam 登攀 —— 高海拔攀登訓練",
    btn=("下載 APK", "支持 AscentRoam", "ascentroam.com"),
    version="**當前版本 v0.28.8**（versionCode 166）· Android 8.0+ · arm64-v8a · 約 30 MB",
    caption="路徑 · 答題 · 技能書 · 訓練 —— 英文介面，淺色主題",
    screens=("路徑頁：山峰卡、單元橫幅與課程節點", "單選題與四個選項",
             "技能書：分類篩選與文章卡", "訓練：今日推薦與練習"),
    h_climb="攀登這條路", h_mount="真實的山", h_badges="四十枚徽章，都是實體", h_arch="架構",
    leadin="每一課都走同一個循環：先讀，再答，答錯的當場訂正，過幾天再回來一次。",
    m1="學習進度現在有了形狀：一路往上爬，沿途會解鎖真實的雪峰，共 88 座，從 3,600 米的賀蘭山到 8,848.86 米的珠峰。每座都有自己的地形圖，按真實高程資料畫：模型撐得住就出立體暈渲，撐不住就畫等高線。卡片上寫著真實海拔、所在山脈與八千米排名。",
    m2="路徑頁頂端那張卡永遠顯示目前海拔對應的那座，並告訴你還差多少米。「我的 → 目前海拔」裡有完整圖譜；已解鎖的點進去看卡片，每座都寫了「關於這座山」，只寫走這條路真正要知道的事。",
    arch_intro="四層，每層只認識下面那一層。介面不直接讀內容，學習模型從內容重算，平台層決定要不要換新題庫，其餘時候不插手。",
    bullets=("[**離線優先。**] 內建題庫隨包分發；線上那份只在比本機新時才覆蓋快取",
             "[**內容是資料。**] 課程、題目、技能書正文與譯文都在 `data/` 裡，不在介面裡：加一個單元就是加資料條目，再在 `Curriculum.kt` 裡寫一行",
             "[**兩個可選服務。**] 題庫更新與帳號同步都可以不存在，其餘功能不依賴它們"),
    log_sum="發布說明：v0.28.8 往前到 v0.13",
    v288_title="**v0.28.8：設定補齊、徒步地形圖，兩處修正**",
    v288=("- 設定補齊了該有的東西：服務條款（可離線閱讀）、為應用程式評分、複製分享連結，以及一個「支持 AscentRoam」入口——點開在瀏覽器裡開啟贊助說明，應用程式內不出現任何支付平台",
          "- 鍵盤彈起不再把底部導覽列一起頂上去；右上角的天氣日輪對準了連續天數，不再壓住旁邊的繩索圖示",
          "- 徒步路線配上地形圖：44 條裡 39 條能看到山脊、河谷與海岸線，還有路線本身的走向，直接替換掉卡片上原本的海拔剖面。長線疊淡暈渲；幾百公里的朝聖之路、JMT 這類改用只畫走向的示意畫法，標出關鍵城鎮",
          "- 88 座山峰都有「關於這座山」，44 條徒步都有「關於這條線」，與路線名一起補齊九種語言；徒步百科按介面語言把你所在的大區排在最前面"),
    loop={"Lesson": "課程", "Questions": "答題", "Corrections": "訂正", "Spaced review": "間隔複習",
          "Altitude": "海拔", "ALTITUDE": "海拔", "8,848 M": "8,848 m"},
    arch={"Path": "路徑", "Lesson": "課程", "Handbook": "技能書", "Training": "訓練", "Profile": "我的",
          "Badge 3D": "3D 徽章", "Curriculum": "課程", "Progress": "進度", "Review queue": "複習佇列",
          "Achievements": "成就", "91 lessons": "91 課", "1,563 questions": "1,563 題",
          "35 articles": "35 篇", "88 peaks": "88 座山峰", "Local store": "本機儲存",
          "Bundled bank": "內建題庫", "Online bank": "線上題庫", "Account sync": "帳號同步",
          "INTERFACE": "介面層", "LEARNING MODEL": "學習模型", "CONTENT": "內容",
          "CONTENT · 10 LANGUAGES": "內容 · 十種語言", "MODEL": "模型", "PLATFORM": "平台層",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* 可選 · 不連網、不登入也能完整使用"},
)

L["ja"] = dict(
    hero_alt="AscentRoam —— 高所登山トレーニング",
    btn=("APK をダウンロード", "AscentRoam を支援", "ascentroam.com"),
    version="**現在のバージョンは v0.28.8**（versionCode 166）· Android 8.0+ · arm64-v8a · 約 30 MB",
    caption="ルート · 出題 · ハンドブック · トレーニング —— 英語 UI・ライトテーマ",
    screens=("経路画面：山峰カード、単元バナーと課ノード", "単一選択問題と四つの選択肢",
             "ハンドブック：分類フィルタと記事カード", "トレーニング：今日の提案とドリル"),
    h_climb="登りの道", h_mount="本物の山", h_badges="四十個のバッジ、みな実体", h_arch="アーキテクチャ",
    leadin="どの課も同じ流れをたどります：読む、答える、間違えたところを訂正する、後日もう一度。",
    m1="学習の進み具合に形がつきました。登るほどに本物の雪山が解除されていきます。全 88 座、3,600 m の賀蘭山から 8,848.86 m のエベレストまで。いずれも実際の標高データから描いた地形図付きで、立体レンダリングが持つ山は陰影付きの立体、持たない山は等高線。カードには実際の標高、所属山脈、8000 m 峰の順位が出ます。",
    m2="経路画面の上部にあるカードは、現在の標高に対応する一峰をいつも示し、あと何メートルかも教えてくれます。「プロフィール → 現在の標高」に全リストがあり、解除済みの山はタップでカードが開きます。どの山にも、実際に歩くときに知っておくべきことだけを書いた「この山について」が付いています。",
    arch_intro="層はそれぞれ、すぐ下の層とだけやり取りします。UI がコンテンツを直接読むことはなく、学習モデルがコンテンツから計算し直し、プラットフォーム層は新しい問題集があるかどうかを判断して、それ以外は手を出しません。",
    bullets=("[**オフラインが先。**] 同梱の問題集は APK に入っています。オンラインのコピーは、新しいときだけキャッシュを置き換えます",
             "[**コンテンツはデータ。**] 課・問題・ハンドブック本文・訳文は `data/` の中にあり、UI にはありません。単元を足すのはデータ項目を足して `Curriculum.kt` に一行書くだけです",
             "[**任意のサービスは二つ。**] 問題集の更新とアカウント同期は、どちらも無くて構いません。他の機能はこれに依存しません"),
    log_sum="リリースノート：v0.28.8 から v0.13 まで",
    v288_title="**v0.28.8：設定を補完、徒步ルートの地形図、二点の修正**",
    v288=("- 設定に足りなかったものを揃えました。利用規約（オフラインで読めます）、アプリの評価、共有リンクのコピー、「AscentRoam を支援」の入口。タップするとブラウザで支援ページが開き、アプリ内に決済サービスの名前は出ません",
          "- キーボードを出しても下部のタブバーは押し上げられず、右上の天気の日輪は連続日数の中央に収まり、隣のロープのアイコンに重なりません",
          "- 徒步ルートに地形図が付きました。44 本のうち 39 本で尾根・谷・海岸線とルートの走りが見え、標高プロファイルを置き換えます。長距離は淡い陰影を敷き、巡礼路や JMT は走向と主要な町だけの模式図に",
          "- 88 座の山に「この山について」、44 本のルートに「このルートについて」が付き、九言語で揃っています。徒步図鑑は表示言語から推定した地域を先頭に並べます"),
    loop={"Lesson": "レッスン", "Questions": "問題", "Corrections": "訂正", "Spaced review": "間隔復習",
          "Altitude": "標高", "ALTITUDE": "標高", "8,848 M": "8,848 m"},
    arch={"Path": "ルート", "Lesson": "レッスン", "Handbook": "ハンドブック", "Training": "トレーニング",
          "Profile": "マイページ", "Badge 3D": "3D バッジ", "Curriculum": "カリキュラム", "Progress": "進度",
          "Review queue": "復習キュー", "Achievements": "実績", "91 lessons": "91 課",
          "1,563 questions": "1,563 問", "35 articles": "35 本", "88 peaks": "88 座",
          "Local store": "ローカル保存", "Bundled bank": "同梱問題集", "Online bank": "オンライン更新",
          "Account sync": "アカウント同期", "INTERFACE": "インターフェース", "LEARNING MODEL": "学習モデル",
          "CONTENT": "内容", "CONTENT · 10 LANGUAGES": "コンテンツ · 10 言語", "MODEL": "モデル",
          "PLATFORM": "プラットフォーム",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "＊任意 · オフラインでも、アカウントなしでも全部使えます"},
)

L["de"] = dict(
    hero_alt="AscentRoam — Höhentraining für Android",
    btn=("APK herunterladen", "AscentRoam unterstützen", "ascentroam.com"),
    version="**Aktuelle Version v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · etwa 30 MB",
    caption="Pfad · Quiz · Handbuch · Training — englische Oberfläche, helles Design",
    screens=("Pfad-Bildschirm: Gipfelkarte, Einheitenbanner und Lektionsknoten",
             "Einfachauswahlfrage mit vier Antworten",
             "Handbuch: Kategoriefilter und Artikelkarten",
             "Training: Empfehlung des Tages und Übungen"),
    h_climb="Der Aufstieg", h_mount="Echte Berge", h_badges="Vierzig Abzeichen, echte Objekte",
    h_arch="Architektur",
    leadin="Jede Lektion läuft nach demselben Ablauf: lesen, Fragen beantworten, Fehler korrigieren, später wiederkommen.",
    m1="Wer aufsteigt, schaltet unterwegs echte Schneegipfel frei: 88 Stück, vom Helan Shan auf 3.600 m bis zum Everest auf 8.848,86 m. Jeder trägt seine eigene Geländekarte, gezeichnet aus echten Höhendaten: ein schattiertes Relief, wo das Modell trägt, Höhenlinien, wo es nicht trägt. Die Karte zeigt die echte Höhe, das Gebirge und den Rang unter den Achttausendern.",
    m2="Oben auf dem Pfad-Bildschirm zeigt die Karte stets den Berg, der zur aktuellen Höhe passt, und wie viele Meter noch fehlen. Unter „Profil → Aktuelle Höhe“ liegt der volle Atlas; freigeschaltete Gipfel öffnen ihre Karte, und jeder hat ein »Über diesen Berg«, geschrieben für das, was unterwegs wirklich zählt.",
    arch_intro="Jede Schicht spricht nur mit der darunterliegenden. Die Oberfläche liest Inhalte nie direkt; das Lernmodell rechnet aus den Inhalten neu; die Plattformschicht entscheidet, ob es einen neueren Fragenpool gibt, und hält sich sonst heraus.",
    bullets=("[**Offline zuerst.**] Der mitgelieferte Pool liegt in der APK; die Online-Kopie ersetzt den Cache nur, wenn sie neuer ist",
             "[**Inhalte sind Daten.**] Lektionen, Fragen, Handbuchtexte und Übersetzungen liegen in `data/`, nicht in der Oberfläche: eine Einheit dazu heißt Dateneinträge plus eine Zeile in `Curriculum.kt`",
             "[**Zwei optionale Dienste.**] Pool-Updates und Konto-Sync können beide fehlen; nichts anderes hängt an ihnen"),
    log_sum="Versionsnotizen, v0.28.8 zurück bis v0.13",
    v288_title="**v0.28.8: Einstellungen vervollständigt, Geländekarten für Wanderungen, zwei Korrekturen**",
    v288=("- Die Einstellungen haben, was fehlte: Nutzungsbedingungen (offline lesbar), App bewerten, Teilen-Link kopieren und ein Eintrag »AscentRoam unterstützen«, der die Unterstützungsseite im Browser öffnet — in der App selbst taucht keine Zahlungsplattform auf",
          "- Die Tastatur schiebt die untere Tableiste nicht mehr hoch, und die Wettersonne oben rechts sitzt mittig über der Serie, statt das Seil-Symbol zu überdecken",
          "- Wanderrouten haben jetzt Geländekarten: 39 von 44 zeigen Grate, Täler und Küstenlinien und den Verlauf der Route, anstelle des Höhenprofils. Lange Wege liegen auf einer weichen Schattierung; mehrere hundert Kilometer wie der Jakobsweg oder der JMT werden als Schema mit Verlauf und wichtigen Orten gezeichnet",
          "- Alle 88 Gipfel haben ein »Über diesen Berg«, alle 44 Routen ein »Über diese Route«, in neun Sprachen; der Wanderatlas stellt deine Region nach der Oberflächensprache nach vorn"),
    loop={"Lesson": "Lektion", "Questions": "Fragen", "Corrections": "Korrekturen",
          "Spaced review": "Wiederholung", "Altitude": "Höhe", "ALTITUDE": "HÖHE", "8,848 M": "8.848 m"},
    arch={"Path": "Pfad", "Lesson": "Lektion", "Handbook": "Handbuch", "Training": "Training",
          "Profile": "Profil", "Badge 3D": "3D-Abzeichen", "Curriculum": "Lehrplan",
          "Progress": "Fortschritt", "Review queue": "Wiederholung", "Achievements": "Erfolge",
          "91 lessons": "91 Lektionen", "1,563 questions": "1.563 Fragen", "35 articles": "35 Artikel",
          "88 peaks": "88 Gipfel", "Local store": "Lokaler Speicher",
          "Bundled bank": "Mitgelieferter Pool", "Online bank": "Online-Pool",
          "Account sync": "Konto-Sync", "INTERFACE": "OBERFLÄCHE", "LEARNING MODEL": "LERNMODELL",
          "CONTENT": "INHALTE", "CONTENT · 10 LANGUAGES": "INHALTE · 10 SPRACHEN",
          "MODEL": "MODELL", "PLATFORM": "PLATTFORM",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* OPTIONAL · DIE APP FUNKTIONIERT VOLLSTÄNDIG OFFLINE, OHNE KONTO"},
)

L["es"] = dict(
    hero_alt="AscentRoam — entrenamiento de altitud para Android",
    btn=("Descargar el APK", "Apoyar AscentRoam", "ascentroam.com"),
    version="**Versión actual v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · unos 30 MB",
    caption="Ruta · Pregunta · Manual · Entrenamiento — interfaz en inglés, tema claro",
    screens=("Pantalla de ruta: tarjeta de cumbre, banner de unidad y nodos de lección",
             "Pregunta de elección única con cuatro opciones",
             "Manual: filtros por categoría y tarjetas de artículo",
             "Entrenamiento: recomendación del día y ejercicios"),
    h_climb="La subida", h_mount="Montañas reales", h_badges="Cuarenta insignias, objetos reales",
    h_arch="Arquitectura",
    leadin="Cada lección sigue el mismo ciclo: leer, responder, corregir lo fallado y volver más tarde.",
    m1="El progreso de aprendizaje ya tiene forma: al subir se van desbloqueando cumbres reales, 88 en total, desde el Helan Shan a 3.600 m hasta el Everest a 8.848,86 m. Cada una lleva su propio mapa de terreno dibujado con datos reales de elevación: sombreado donde el modelo se sostiene y curvas de nivel donde no. La tarjeta muestra la altitud real, la cordillera y el puesto entre los ochomiles.",
    m2="La tarjeta de arriba en la pantalla de ruta muestra siempre la cumbre que corresponde a tu altitud actual, y cuántos metros faltan. En «Perfil → altitud actual» está el atlas completo; las cumbres desbloqueadas abren su tarjeta, y cada una tiene un «Sobre esta montaña» escrito para lo que de verdad importa en el camino.",
    arch_intro="Cada capa solo habla con la de abajo. La interfaz nunca lee el contenido directamente; el modelo de aprendizaje lo recalcula a partir del contenido; la capa de plataforma decide si existe un banco más nuevo y, si no, no interfiere.",
    bullets=("[**Sin conexión primero.**] El banco incluido va dentro del APK; la copia en línea solo sustituye la caché cuando es más nueva",
             "[**El contenido son datos.**] Lecciones, preguntas, manual y traducciones viven en `data/`, no en la interfaz: añadir una unidad es añadir entradas de datos y una línea en `Curriculum.kt`",
             "[**Dos servicios opcionales.**] Las actualizaciones del banco y la sincronización de cuenta pueden faltar; nada más depende de ellos"),
    log_sum="Notas de versión, de la v0.28.8 a la v0.13",
    v288_title="**v0.28.8: ajustes completados, mapas de terreno para senderismo, dos correcciones**",
    v288=("- Los ajustes ya tienen lo que faltaba: términos del servicio (legibles sin conexión), valorar la aplicación, copiar el enlace para compartir y una entrada «Apoyar AscentRoam» que abre la página de apoyo en el navegador: dentro de la app no aparece ninguna plataforma de pago",
          "- El teclado ya no levanta la barra de pestañas y el sol del tiempo arriba a la derecha queda centrado sobre la racha en lugar de tapar el icono de cuerda",
          "- Las rutas de senderismo llevan mapa de terreno: 39 de las 44 muestran crestas, valles y costas, y el trazado de la ruta, en lugar del perfil de altitud. Las rutas largas van sobre un sombreado suave; las de cientos de kilómetros, como el Camino o el JMT, se dibujan como esquema con el trazado y las poblaciones clave",
          "- Las 88 cumbres tienen un «Sobre esta montaña» y las 44 rutas un «Sobre esta ruta», en nueve idiomas; el atlas ordena primero tu región según el idioma de la interfaz"),
    loop={"Lesson": "Lección", "Questions": "Preguntas", "Corrections": "Correcciones",
          "Spaced review": "Repaso espaciado", "Altitude": "Altitud", "ALTITUDE": "ALTITUD",
          "8,848 M": "8.848 m"},
    arch={"Path": "Ruta", "Lesson": "Lección", "Handbook": "Manual", "Training": "Entrenamiento",
          "Profile": "Perfil", "Badge 3D": "Insignia 3D", "Curriculum": "Temario",
          "Progress": "Progreso", "Review queue": "Cola de repaso", "Achievements": "Logros",
          "91 lessons": "91 lecciones", "1,563 questions": "1.563 preguntas",
          "35 articles": "35 artículos", "88 peaks": "88 cumbres", "Local store": "Almacén local",
          "Bundled bank": "Banco incluido", "Online bank": "Banco en línea",
          "Account sync": "Sinc. de cuenta", "INTERFACE": "INTERFAZ",
          "LEARNING MODEL": "MODELO DE APRENDIZAJE", "CONTENT": "CONTENIDO",
          "CONTENT · 10 LANGUAGES": "CONTENIDO · 10 IDIOMAS", "MODEL": "MODELO",
          "PLATFORM": "PLATAFORMA",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* OPCIONAL · LA APP FUNCIONA DEL TODO SIN CONEXIÓN Y SIN CUENTA"},
)

L["fr"] = dict(
    hero_alt="AscentRoam — entraînement en altitude pour Android",
    btn=("Télécharger l’APK", "Soutenir AscentRoam", "ascentroam.com"),
    version="**Version actuelle v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · environ 30 Mo",
    caption="Parcours · Question · Manuel · Entraînement — interface en anglais, thème clair",
    screens=("Écran Parcours : carte de sommet, bandeau d’unité et nœuds de leçon",
             "Question à choix unique avec quatre réponses",
             "Manuel : filtres par catégorie et fiches d’article",
             "Entraînement : recommandation du jour et exercices"),
    h_climb="La montée", h_mount="De vraies montagnes", h_badges="Quarante badges, objets réels",
    h_arch="Architecture",
    leadin="Chaque leçon suit le même cycle : lire, répondre, corriger ce qui a été raté, revenir plus tard.",
    m1="La progression a désormais une forme : à mesure qu’on monte, de vrais sommets se débloquent, 88 au total, du Helan Shan à 3 600 m à l’Everest à 8 848,86 m. Chacun porte sa carte de terrain dessinée à partir de données d’élévation réelles : ombrage quand le rendu tient, courbes de niveau quand il ne tient pas. La fiche donne l’altitude réelle, la chaîne et le rang parmi les 8 000.",
    m2="La carte en haut de l’écran Parcours montre toujours le sommet qui correspond à votre altitude, et combien de mètres restent. « Profil → altitude actuelle » contient l’atlas complet ; les sommets débloqués ouvrent leur fiche, et chacun a un « À propos de cette montagne » écrit pour ce qui compte vraiment sur le terrain.",
    arch_intro="Chaque couche ne parle qu’à celle du dessous. L’interface ne lit jamais le contenu directement ; le modèle d’apprentissage le recalcule à partir du contenu ; la couche plateforme décide s’il existe une banque plus récente et, sinon, ne s’en mêle pas.",
    bullets=("[**Hors ligne d’abord.**] La banque embarquée est dans l’APK ; la copie en ligne ne remplace le cache que si elle est plus récente",
             "[**Le contenu est des données.**] Leçons, questions, manuel et traductions vivent dans `data/`, pas dans l’interface : ajouter une unité, c’est ajouter des entrées et une ligne dans `Curriculum.kt`",
             "[**Deux services facultatifs.**] Les mises à jour de la banque et la synchro de compte peuvent manquer ; rien d’autre n’en dépend"),
    log_sum="Notes de version, de la v0.28.8 à la v0.13",
    v288_title="**v0.28.8 : réglages complétés, cartes de terrain pour la randonnée, deux corrections**",
    v288=("- Les réglages ont ce qui manquait : conditions d’utilisation (lisibles hors ligne), noter l’application, copier le lien de partage et une entrée « Soutenir AscentRoam » qui ouvre la page de soutien dans le navigateur — aucune plateforme de paiement n’apparaît dans l’application",
          "- Le clavier ne soulève plus la barre d’onglets, et le soleil météo en haut à droite est centré sur la série au lieu de chevaucher l’icône de corde",
          "- Les itinéraires de randonnée ont une carte de terrain : 39 des 44 montrent crêtes, vallées et littoral, ainsi que le tracé, à la place du profil altimétrique. Les longues étapes reposent sur un ombrage discret ; celles de plusieurs centaines de kilomètres, comme le Chemin ou le JMT, sont dessinées en schéma avec le tracé et les villes clés",
          "- Les 88 sommets ont un « À propos de cette montagne » et les 44 itinéraires un « À propos de cet itinéraire », en neuf langues ; l’atlas place ta région en tête selon la langue de l’interface"),
    loop={"Lesson": "Leçon", "Questions": "Questions", "Corrections": "Corrections",
          "Spaced review": "Révision espacée", "Altitude": "Altitude", "ALTITUDE": "ALTITUDE",
          "8,848 M": "8 848 m"},
    arch={"Path": "Parcours", "Lesson": "Leçon", "Handbook": "Manuel", "Training": "Entraînement",
          "Profile": "Profil", "Badge 3D": "Badge 3D", "Curriculum": "Programme",
          "Progress": "Progression", "Review queue": "File de révision", "Achievements": "Succès",
          "91 lessons": "91 leçons", "1,563 questions": "1 563 questions",
          "35 articles": "35 articles", "88 peaks": "88 sommets", "Local store": "Stockage local",
          "Bundled bank": "Banque embarquée", "Online bank": "Banque en ligne",
          "Account sync": "Synchro compte", "INTERFACE": "INTERFACE",
          "LEARNING MODEL": "MODÈLE D’APPRENTISSAGE", "CONTENT": "CONTENU",
          "CONTENT · 10 LANGUAGES": "CONTENU · 10 LANGUES", "MODEL": "MODÈLE",
          "PLATFORM": "PLATEFORME",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* FACULTATIF · L’APP S’UTILISE ENTIÈREMENT HORS LIGNE, SANS COMPTE"},
)

L["it"] = dict(
    hero_alt="AscentRoam — allenamento in alta quota per Android",
    btn=("Scarica l'APK", "Sostieni AscentRoam", "ascentroam.com"),
    version="**Versione attuale v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · circa 30 MB",
    caption="Percorso · Domanda · Manuale · Allenamento — interfaccia in inglese, tema chiaro",
    screens=("Schermata del percorso: scheda della vetta, banner di unità e nodi delle lezioni",
             "Domanda a scelta singola con quattro opzioni",
             "Manuale: filtri per categoria e schede degli articoli",
             "Allenamento: consiglio del giorno ed esercizi"),
    h_climb="La salita", h_mount="Montagne vere", h_badges="Quaranta badge, oggetti veri",
    h_arch="Architettura",
    leadin="Ogni lezione segue lo stesso ciclo: leggere, rispondere, correggere gli errori, tornare più avanti.",
    m1="Il progresso ora ha una forma: salendo si sbloccano vere cime innevate, 88 in tutto, dal Helan Shan a 3.600 m all'Everest a 8.848,86 m. Ognuna porta la sua carta del terreno disegnata da dati di quota reali: ombreggiatura dove il modello regge, curve di livello dove non regge. La scheda mostra la quota reale, la catena e il posto tra gli ottomila.",
    m2="La scheda in cima alla schermata del percorso mostra sempre la vetta che corrisponde alla tua quota attuale e quanti metri mancano. In «Profilo → quota attuale» c'è l'atlante completo; le vette sbloccate aprono la loro scheda, e ognuna ha un «Su questa montagna» scritto per ciò che conta davvero sul cammino.",
    arch_intro="Ogni livello parla solo con quello sotto. L'interfaccia non legge mai i contenuti direttamente; il modello di apprendimento li ricalcola dai contenuti; il livello piattaforma decide se esiste un questionario più recente e, altrimenti, non si intromette.",
    bullets=("[**Prima l'offline.**] Il questionario incluso sta nell'APK; la copia online sostituisce la cache solo se più recente",
             "[**I contenuti sono dati.**] Lezioni, domande, manuale e traduzioni vivono in `data/`, non nell'interfaccia: aggiungere un'unità significa aggiungere voci e una riga in `Curriculum.kt`",
             "[**Due servizi facoltativi.**] Aggiornamenti del questionario e sincronizzazione dell'account possono mancare; nient'altro dipende da loro"),
    log_sum="Note di versione, dalla v0.28.8 alla v0.13",
    v288_title="**v0.28.8: impostazioni completate, carte del terreno per i sentieri, due correzioni**",
    v288=("- Le impostazioni hanno ora quello che mancava: termini di servizio (leggibili offline), valuta l'app, copia il link di condivisione e una voce «Sostieni AscentRoam» che apre la pagina di sostegno nel browser — nell'app non compare nessuna piattaforma di pagamento",
          "- La tastiera non solleva più la barra delle schede e il sole del meteo in alto a destra è centrato sulla serie invece di coprire l'icona della corda",
          "- I sentieri hanno una carta del terreno: 39 su 44 mostrano creste, valli e coste e il tracciato del percorso, al posto del profilo altimetrico. I percorsi lunghi poggiano su un'ombreggiatura tenue; quelli di centinaia di chilometri, come il Cammino o il JMT, sono disegnati come schema con tracciato e località chiave",
          "- Le 88 cime hanno un «Su questa montagna» e i 44 itinerari un «Su questo itinerario», in nove lingue; l'atlante mette la tua regione in cima in base alla lingua dell'interfaccia"),
    loop={"Lesson": "Lezione", "Questions": "Domande", "Corrections": "Correzioni",
          "Spaced review": "Ripetizione diluita", "Altitude": "Quota", "ALTITUDE": "QUOTA",
          "8,848 M": "8.848 m"},
    arch={"Path": "Percorso", "Lesson": "Lezione", "Handbook": "Manuale",
          "Training": "Allenamento", "Profile": "Profilo", "Badge 3D": "Badge 3D",
          "Curriculum": "Programma", "Progress": "Progresso", "Review queue": "Coda di ripasso",
          "Achievements": "Obiettivi", "91 lessons": "91 lezioni",
          "1,563 questions": "1.563 domande", "35 articles": "35 articoli", "88 peaks": "88 cime",
          "Local store": "Archivio locale", "Bundled bank": "Questionario incluso",
          "Online bank": "Questionario online", "Account sync": "Sinc. account",
          "INTERFACE": "INTERFACCIA", "LEARNING MODEL": "MODELLO DI APPRENDIMENTO",
          "CONTENT": "CONTENUTI", "CONTENT · 10 LANGUAGES": "CONTENUTI · 10 LINGUE",
          "MODEL": "MODELLO", "PLATFORM": "PIATTAFORMA",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* FACOLTATIVO · L'APP FUNZIONA COMPLETAMENTE OFFLINE, SENZA ACCOUNT"},
)

L["ru"] = dict(
    hero_alt="AscentRoam — тренировка в горах для Android",
    btn=("Скачать APK", "Поддержать AscentRoam", "ascentroam.com"),
    version="**Текущая версия v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · около 30 МБ",
    caption="Маршрут · Вопрос · Справочник · Тренировка — английский интерфейс, светлая тема",
    screens=("Экран маршрута: карточка вершины, баннер раздела и узлы уроков",
             "Вопрос с одиночным выбором и четыре варианта",
             "Справочник: фильтры по темам и карточки статей",
             "Тренировка: рекомендация дня и упражнения"),
    h_climb="Подъём", h_mount="Настоящие горы", h_badges="Сорок значков, настоящие предметы",
    h_arch="Архитектура",
    leadin="Каждый урок идёт по одному циклу: прочитать, ответить, разобрать ошибки, вернуться позже.",
    m1="У прогресса появилась форма: поднимаясь, вы открываете настоящие снежные вершины, всего 88, от Хэланьшаня на 3 600 м до Эвереста на 8 848,86 м. У каждой своя карта рельефа, нарисованная по реальным данным высот: объёмная отмывка там, где тянет модель, и горизонтали там, где не тянет. На карточке — реальная высота, хребет и место среди восьмитысячников.",
    m2="Карточка вверху экрана маршрута всегда показывает вершину, соответствующую вашей высоте, и сколько метров осталось. В разделе «Профиль → текущая высота» лежит весь атлас; открытые вершины раскрывают карточку, и у каждой есть «Об этой горе»: о том, что действительно важно на пути.",
    arch_intro="Каждый слой общается только со слоем под ним. Интерфейс никогда не читает содержимое напрямую; модель обучения пересчитывает его из содержимого; платформенный слой решает, нет ли более нового банка вопросов, а иначе не вмешивается.",
    bullets=("[**Сначала офлайн.**] Встроенный банк лежит в APK; онлайн-копия заменяет кеш, только если она новее",
             "[**Содержимое — это данные.**] Уроки, вопросы, справочник и переводы живут в `data/`, а не в интерфейсе: добавить раздел — значит добавить записи и одну строку в `Curriculum.kt`",
             "[**Два необязательных сервиса.**] Обновления банка и синхронизация аккаунта могут отсутствовать; больше ничего от них не зависит"),
    log_sum="Примечания к версиям: от v0.28.8 до v0.13",
    v288_title="**v0.28.8: настройки дополнены, карты рельефа для пеших маршрутов, две правки**",
    v288=("- В настройках появилось то, чего не хватало: условия использования (читаются офлайн), оценка приложения, копирование ссылки и пункт «Поддержать AscentRoam», который открывает страницу поддержки в браузере — внутри приложения нет ни одной платёжной платформы",
          "- Клавиатура больше не поднимает нижнюю панель вкладок, а солнце в правом верхнем углу стоит по центру счётчика серии и не задевает иконку верёвки",
          "- У пеших маршрутов появилась карта рельефа: у 39 из 44 видны гребни, долины и берег, а также сама линия маршрута, вместо профиля высот. Длинные маршруты лежат на мягкой отмывке; те, что в сотни километров, как Камино или JMT, нарисованы схемой — только линия и ключевые города",
          "- У всех 88 вершин есть «Об этой горе», у 44 маршрутов — «Об этом маршруте», на девяти языках; атлас ставит ваш регион первым по языку интерфейса"),
    loop={"Lesson": "Урок", "Questions": "Вопросы", "Corrections": "Повтор ошибок",
          "Spaced review": "Повторение", "Altitude": "Высота", "ALTITUDE": "ВЫСОТА",
          "8,848 M": "8 848 м"},
    arch={"Path": "Маршрут", "Lesson": "Урок", "Handbook": "Справочник", "Training": "Тренировка",
          "Profile": "Профиль", "Badge 3D": "3D-значок", "Curriculum": "Программа",
          "Progress": "Прогресс", "Review queue": "Очередь повторения", "Achievements": "Достижения",
          "91 lessons": "91 урок", "1,563 questions": "1 563 вопроса", "35 articles": "35 статей",
          "88 peaks": "88 вершин", "Local store": "Локальное хранилище",
          "Bundled bank": "Встроенный банк", "Online bank": "Онлайн-банк",
          "Account sync": "Синхр. аккаунта", "INTERFACE": "ИНТЕРФЕЙС",
          "LEARNING MODEL": "МОДЕЛЬ ОБУЧЕНИЯ", "CONTENT": "СОДЕРЖАНИЕ",
          "CONTENT · 10 LANGUAGES": "СОДЕРЖАНИЕ · 10 ЯЗЫКОВ", "MODEL": "МОДЕЛЬ",
          "PLATFORM": "ПЛАТФОРМА",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* ПО ЖЕЛАНИЮ · ПРИЛОЖЕНИЕ ПОЛНОСТЬЮ РАБОТАЕТ ОФЛАЙН И БЕЗ АККАУНТА"},
)

L["sv"] = dict(
    hero_alt="AscentRoam — höghöjdsträning för Android",
    btn=("Hämta APK:n", "Stöd AscentRoam", "ascentroam.com"),
    version="**Aktuell version v0.28.8** (versionCode 166) · Android 8.0+ · arm64-v8a · cirka 30 MB",
    caption="Rutt · Fråga · Handbok · Träning — engelskt gränssnitt, ljust tema",
    screens=(" Ruttskärm: toppkort, enhetsband och lektionsnoder",
             "Envalsfråga med fyra svarsalternativ",
             "Handbok: kategorifilter och artikelkort",
             "Träning: dagens förslag och övningar"),
    h_climb="Vägen upp", h_mount="Verkliga berg", h_badges="Fyrtio emblem, riktiga objekt",
    h_arch="Arkitektur",
    leadin="Varje lektion följer samma loop: läs, svara, rätt det du missade, kom tillbaka senare.",
    m1="Framstegen har fått en form: när du klättrar låses riktiga snötoppar upp, 88 totalt, från Helan Shan på 3 600 m till Everest på 8 848,86 m. Var och en har en egen terrängkarta ritad från verkliga höjddata: skuggad relief där modellen håller och höjdkurvor där den inte håller. Kortet visar verklig höjd, bergskedja och plats bland åttatusenmetarna.",
    m2="Kortet högst upp på Ruttskärmen visar alltid toppen som hör till din aktuella höjd, och hur många meter som återstår. Under ”Profil → aktuell höjd” finns hela atlasen; upplåsta toppar öppnar sitt kort, och var och en har ett »Om det här berget«, skrivet för det som faktiskt spelar roll på vägen.",
    arch_intro="Varje lager talar bara med lagret under. Gränssnittet läser aldrig innehållet direkt; inlärningsmodellen räknar om det från innehållet; plattformslagret avgör om det finns en nyare frågebank och lägger sig annars inte i.",
    bullets=("[**Offline först.**] Den medföljande banken ligger i APK:n; onlinekopian ersätter cachen bara när den är nyare",
             "[**Innehållet är data.**] Lektioner, frågor, handbok och översättningar ligger i `data/`, inte i gränssnittet: en enhet till är dataposter plus en rad i `Curriculum.kt`",
             "[**Två valfria tjänster.**] Bankuppdateringar och kontosynk kan saknas; inget annat beror på dem"),
    log_sum="Versionsnoteringar, v0.28.8 tillbaka till v0.13",
    v288_title="**v0.28.8: inställningarna kompletterade, terrängkartor för vandringsleder, två rättelser**",
    v288=("- Inställningarna har fått det som saknades: användarvillkor (läsbara offline), betygsätt appen, kopiera delningslänk och en post »Stöd AscentRoam« som öppnar supportsidan i webbläsaren — ingen betalplattform visas i appen",
          "- Tangentbordet lyfter inte längre den nedre flikraden, och vädrets sol uppe till höger ligger centrerad över sviten i stället för att täcka repikonen",
          "- Vandringslederna har terrängkartor: 39 av 44 visar ryggar, dalar och kustlinjer och ledens sträckning, i stället för höjdprofilen. Långa leder ligger på en mjuk skuggning; de på flera hundra kilometer, som Camino eller JMT, ritas som schema med bara sträckning och viktiga orter",
          "- Alla 88 toppar har ett »Om det här berget« och alla 44 leder ett »Om den här leden«, på nio språk; atlasen placerar din region först utifrån gränssnittsspråket"),
    loop={"Lesson": "Lektion", "Questions": "Frågor", "Corrections": "Rättelser",
          "Spaced review": "Utspridd repetition", "Altitude": "Höjd", "ALTITUDE": "HÖJD",
          "8,848 M": "8 848 m"},
    arch={"Path": "Rutt", "Lesson": "Lektion", "Handbook": "Handbok", "Training": "Träning",
          "Profile": "Profil", "Badge 3D": "3D-emblem", "Curriculum": "Kurs", "Progress": "Progress",
          "Review queue": "Repetitionskö", "Achievements": "Prestationer",
          "91 lessons": "91 lektioner", "1,563 questions": "1 563 frågor",
          "35 articles": "35 artiklar", "88 peaks": "88 toppar", "Local store": "Lokal lagring",
          "Bundled bank": "Medföljande bank", "Online bank": "Bank online",
          "Account sync": "Kontosynk", "INTERFACE": "GRÄNSSNITT",
          "LEARNING MODEL": "INLÄRNINGSMODELL", "CONTENT": "INNEHÅLL",
          "CONTENT · 10 LANGUAGES": "INNEHÅLL · 10 SPRÅK", "MODEL": "MODELL",
          "PLATFORM": "PLATTFORM",
          "* OPTIONAL · THE APP IS FULLY USABLE OFFLINE, WITHOUT AN ACCOUNT":
              "* VALFRITT · APPEN FUNGERAR HELT OFFLINE, UTAN KONTO"},
)

# 章节标题（旧文件 → 键）
SEC = {
    "tw": dict(install="裝到手機上", support="支持 AscentRoam", inside="裡面有什麼", langs="十種語言",
               cur="課程怎麼排", where="內容從哪來", safety="安全邊界", build="自己構建",
               layout="專案結構", changing="改內容", online="題庫線上更新", log="最近改了什麼", lic="許可"),
    "ja": dict(install="インストール", support="AscentRoam を支援する", inside="中身", langs="10 言語",
               cur="カリキュラム", where="コンテンツの出典", safety="安全の境界", build="自分でビルドする",
               layout="プロジェクト構成", changing="コンテンツを変える", online="問題集のオンライン更新",
               log="最近の変更", lic="ライセンス"),
    "de": dict(install="Installation", support="AscentRoam unterstützen", inside="Was drin ist",
               langs="Zehn Sprachen", cur="Lehrplan", where="Woher die Inhalte kommen",
               safety="Sicherheitsgrenze", build="Selbst bauen", layout="Projektaufbau",
               changing="Inhalte ändern", online="Online-Updates des Pools", log="Letzte Änderungen",
               lic="Lizenz"),
    "es": dict(install="Instalación", support="Apoyar AscentRoam", inside="Qué hay dentro",
               langs="Diez idiomas", cur="Temario", where="De dónde viene el contenido",
               safety="Límite de seguridad", build="Compilarlo tú mismo",
               layout="Estructura del proyecto", changing="Cambiar contenido",
               online="Actualizaciones en línea del banco", log="Cambios recientes", lic="Licencia"),
    "fr": dict(install="Installation", support="Soutenir AscentRoam", inside="Ce qu’il y a dedans",
               langs="Dix langues", cur="Programme", where="D’où viennent les contenus",
               safety="Limite de sécurité", build="Le compiler soi-même",
               layout="Structure du projet", changing="Modifier le contenu",
               online="Mises à jour en ligne de la banque", log="Changements récents", lic="Licence"),
    "it": dict(install="Installazione", support="Sostenere AscentRoam", inside="Cosa c'è dentro",
               langs="Dieci lingue", cur="Programma", where="Da dove vengono i contenuti",
               safety="Limite di sicurezza", build="Compilarlo da soli",
               layout="Struttura del progetto", changing="Modificare i contenuti",
               online="Aggiornamenti online del questionario", log="Cambi recenti", lic="Licenza"),
    "ru": dict(install="Установка", support="Поддержать AscentRoam", inside="Что внутри",
               langs="Десять языков", cur="Программа", where="Откуда берётся содержимое",
               safety="Граница безопасности", build="Собрать самому",
               layout="Структура проекта", changing="Как менять содержимое",
               online="Обновление банка по сети", log="Последние изменения", lic="Лицензия"),
    "sv": dict(install="Installation", support="Stöd AscentRoam", inside="Vad som finns inuti",
               langs="Tio språk", cur="Kursplan", where="Var innehållet kommer från",
               safety="Säkerhetsgräns", build="Bygga själv", layout="Projektstruktur",
               changing="Ändra innehåll", online="Onlineuppdatering av banken",
               log="Senaste ändringar", lic="Licens"),
}


def split_sections(text: str) -> dict:
    """按 `## 标题` 切分，返回 {标题: 正文(段落列表)}，`# ` 主标题与语言行单列。"""
    lines = text.split("\n")
    out = {"__head__": []}
    cur = "__head__"
    for ln in lines:
        m = re.match(r"^## (.+)$", ln.strip())
        if m:
            cur = m.group(1).strip()
            out[cur] = []
            continue
        out.setdefault(cur, []).append(ln)
    res = {}
    for k, v in out.items():
        body = "\n".join(v).strip()
        res[k] = [p.strip() for p in re.split(r"\n\s*\n", body) if p.strip()]
    return res


def paras(section) -> list:
    return section if isinstance(section, list) else []


def build(lang: str) -> None:
    cfg = L[lang]
    sec = SEC[lang]
    old = (ROOT / f"README.{'zh-TW' if lang == 'tw' else lang}.md").read_text(encoding="utf-8")
    S = split_sections(old)

    head = S["__head__"]
    intro = head[2]        # 第一段正文（原文复用）
    tracks = head[3]       # 两条大类段（原文复用）

    # 语言行：HTML 块内不能用 Markdown，改用 <a> / <b>
    NAMES = [("README.md", "English"), ("README.zh-CN.md", "简体中文"),
             ("README.zh-TW.md", "繁體中文"), ("README.ja.md", "日本語"),
             ("README.de.md", "Deutsch"), ("README.es.md", "Español"),
             ("README.fr.md", "Français"), ("README.it.md", "Italiano"),
             ("README.ru.md", "Русский"), ("README.sv.md", "Svenska")]
    here = "zh-TW" if lang == "tw" else lang
    langrow = " · ".join(
        f"<b><a href=\"{f}\">{n}</a></b>" if f == f"README.{here}.md" else f'<a href="{f}">{n}</a>'
        for f, n in NAMES)
    version = cfg["version"].replace("**", "")

    inside = S[sec["inside"]]
    subs, cur_title, cur_paras = [], None, []
    for p in inside:
        if p.startswith("### "):
            if cur_title:
                subs.append((cur_title, cur_paras))
            cur_title, cur_paras = p[4:].strip(), []
        else:
            cur_paras.append(p)
    if cur_title:
        subs.append((cur_title, cur_paras))
    assert len(subs) == 6, (lang, [t for t, _ in subs])
    path, bank, hand, train, settings, feedback = subs
    assert len(path[1]) == 3 and len(train[1]) == 3, (lang, len(path[1]), len(train[1]))

    # 各章节正文（段落列表 → 文本块）
    def sec_text(key):
        return "\n\n".join(S[sec[key]])

    img = lambda name, alt, w="100%": f'<img src="assets/readme/{name}" width="{w}" alt="{alt}">'

    bullets = "\n".join(
        "- " + b.replace("[**", "**", 1).replace("**] ", "** ", 1) for b in cfg["bullets"]
    )
    screens = "\n".join(
        f'  <img src="assets/readme/screen-{n}.png" width="186" alt="{a}">&nbsp;'
        for n, a in zip(("path", "quiz", "handbook", "training"), cfg["screens"])
    )

    inside_body = []
    for title, ps, drop in ((bank[0], bank[1], False), (hand[0], hand[1], False),
                            (train[0], [train[1][0]], True), (settings[0], settings[1], False),
                            (feedback[0], feedback[1], False)):
        sep = "。" if lang in ("tw", "ja") else "."
        inside_body.append(f"**{title}{sep}** {ps[0]}")
        if not drop:
            inside_body.extend(ps[1:])

    new = f"""<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/readme/hero-dark.png">
    <img src="assets/readme/hero.png" alt="{cfg['hero_alt']}" width="100%">
  </picture>
</p>

<br>

<p align="center">
  <a href="https://github.com/Solar-CV/AscentApp/releases"><img src="assets/readme/btn-download.svg" height="46" alt="{cfg['btn'][0]}"></a>&nbsp;
  <a href="https://ascentroam.com/support"><img src="assets/readme/btn-support.svg" height="46" alt="{cfg['btn'][1]}"></a>&nbsp;
  <a href="https://ascentroam.com"><img src="assets/readme/btn-site.svg" height="46" alt="{cfg['btn'][2]}"></a>
</p>

<br>

<p align="center"><sub>
{langrow}
</sub></p>

{intro}

<p align="center"><sub><b>{version}</b></sub></p>

<p align="center">
{screens}
</p>

<p align="center"><sub>{cfg['caption']}</sub></p>

<br>

<img src="assets/readme/divider.svg" width="100%" alt="">

## {cfg['h_climb']}

{tracks}

{path[1][0]}

{cfg['leadin']}

<img src="assets/readme/loop-{lang}.svg" width="100%" alt="">

{path[1][2]}

## {cfg['h_mount']}

<img src="assets/readme/peaks.png" width="100%" alt="">

{cfg['m1']}

{cfg['m2']}

## {cfg['h_badges']}

<img src="assets/readme/badges.png" width="100%" alt="">

{train[1][2]}

## {sec['inside']}

""" + "\n\n".join(inside_body) + f"""

## {sec['langs']}

{sec_text('langs')}

## {sec['cur']}

{sec_text('cur')}

## {sec['install']}

{sec_text('install')}

## {cfg['h_arch']}

<img src="assets/readme/arch-{lang}.svg" width="100%" alt="">

{cfg['arch_intro']}

{bullets}

<details>
<summary><b>{sec['layout']}</b></summary>

{sec_text('layout')}

</details>

## {sec['build']}

{sec_text('build')}

### {sec['changing']}

{sec_text('changing')}

## {sec['online']}

{sec_text('online')}

## {sec['where']}

{sec_text('where')}

## {sec['safety']}

{sec_text('safety')}

## {sec['log']}

<details>
<summary><b>{cfg['log_sum']}</b></summary>

{cfg['v288_title']}

""" + "\n\n".join(cfg["v288"]) + "\n\n" + sec_text("log") + f"""

</details>

## {sec['support']}

{sec_text('support')}

<img src="assets/readme/divider.svg" width="100%" alt="">

## {sec['lic']}

{sec_text('lic')}
"""

    (ROOT / f"README.{'zh-TW' if lang == 'tw' else lang}.md").write_text(new, encoding="utf-8")
    print(f"ok README.{lang}.md  ({new.count(chr(10)) + 1} lines)")


def make_diagrams(lang: str) -> None:
    cfg = L[lang]
    for src, dst, labels in (("loop.svg", f"loop-{lang}.svg", cfg["loop"]),
                             ("arch.svg", f"arch-{lang}.svg", cfg["arch"])):
        s = (ASSETS / src).read_text(encoding="utf-8")
        for k, v in labels.items():
            s = s.replace(f">{k}</text>", f">{v}</text>")
        (ASSETS / dst).write_text(s, encoding="utf-8")
    print(f"ok diagrams {lang}")


if __name__ == "__main__":
    import sys
    targets = sys.argv[1:] or ["tw", "ja", "de", "es", "fr", "it", "ru", "sv"]
    for lang in targets:
        make_diagrams(lang)
        build(lang)

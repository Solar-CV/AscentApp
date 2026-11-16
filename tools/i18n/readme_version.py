"""README 版本行更新到 v0.28.0，并补上山峰里程碑这一段。

只替换版本行开头那一句，后面的历史章节（v0.19.24、v0.20 等）保持不动 ——
README 是滚动累积的更新日志，旧段落是记录不是现状。
"""
import glob
import os

PARA = {
    "README.md": (
        "Current version v0.27.2 (versionCode 157).",
        "Current version v0.28.0 (versionCode 158). Learning progress has a shape now: climb, "
        "and real snow peaks unlock along the way. A card at the top of the Path screen shows the "
        "peak that matches your current altitude — 88 in all, from Helan Shan at 3,600 m to Everest "
        "at 8,848 m, each with a terrain map (contour lines where a 3D render would not hold up), "
        "its real elevation, range and 8000er rank. Profile → current altitude holds the full list; "
        "unlocked peaks open their card. The summit target is now Everest's official 8,848.86 m, "
        "so the progress bar keeps moving to the very top instead of filling at 8,000 m.",
    ),
    "README.zh-CN.md": (
        "当前版本 v0.27.2（versionCode 157）。",
        "当前版本 v0.28.0（versionCode 158）。学习进度现在有了形状：一路往上爬，沿途会解锁真实的雪峰。"
        "路径页顶部新增一张山峰卡，显示当前海拔对应的那一座——共 88 座，从 3,600 米的贺兰山到 "
        "8,848 米的珠峰，每座都有地形图（立体渲染撑不住的山改用等高线）、真实海拔、所在山脉与"
        "八千米排名；「我的 → 当前海拔」里有完整列表，已解锁的点进去看卡片。"
        "同时把目标海拔改成珠峰官方高程 8,848.86 米：课程一门没加，进度条不会在 8,000 米就满。",
    ),
    "README.zh-TW.md": (
        "當前版本 v0.27.2（versionCode 157）。",
        "當前版本 v0.28.0（versionCode 158）。學習進度現在有了形狀：一路往上爬，沿途會解鎖真實的雪峰。"
        "路徑頁頂端新增一張山峰卡，顯示目前海拔對應的那一座——共 88 座，從 3,600 米的賀蘭山到 "
        "8,848 米的珠峰，每座都有地形圖（立體渲染撐不住的山改用等高線）、真實海拔、所在山脈與"
        "八千米排名；「我的 → 目前海拔」裡有完整列表，已解鎖的點進去看卡片。"
        "同時把目標海拔改成珠峰官方高程 8,848.86 米：課程一門沒加，進度條不會在 8,000 米就滿。",
    ),
    "README.ja.md": (
        "現在のバージョンは v0.27.2（versionCode 157）。",
        "現在のバージョンは v0.28.0（versionCode 158）。学習の進み具合に形がつきました。"
        "登るほどに本物の雪山が解除されていきます。経路画面の上部に山峰カードが加わり、"
        "現在の標高に対応する一峰を表示します。全 88 座、3,600 m の賀蘭山から 8,848 m のエベレストまで。"
        "いずれも地形図（立体レンダリングが難しい山は等高線）、実際の標高、所属山脈、"
        "8000 m 峰の順位を備えています。「プロフィール → 現在の標高」には全リストがあり、"
        "解除済みの山はタップでカードを開けます。到達目標もエベレストの公式標高 8,848.86 m に変更しました。"
        "課は一つも増やしておらず、進捗バーは 8,000 m で満杯にならず頂上まで伸びます。",
    ),
    "README.de.md": (
        "Aktuelle Version v0.27.2 (versionCode 157).",
        "Aktuelle Version v0.28.0 (versionCode 158). Der Lernfortschritt hat jetzt eine Form: "
        "Wer aufsteigt, schaltet unterwegs echte Schneegipfel frei. Oben auf dem Pfad-Bildschirm "
        "zeigt eine neue Gipfelkarte den Berg, der zur aktuellen Höhe passt — insgesamt 88, vom "
        "Helan Shan auf 3.600 m bis zum Everest auf 8.848 m, jeder mit Geländekarte (Höhenlinien, "
        "wo eine 3D-Darstellung nicht trägt), echter Höhe, Gebirge und Achttausender-Rang. Unter "
        "„Profil → Aktuelle Höhe“ liegt die vollständige Liste; freigeschaltete Gipfel öffnen ihre "
        "Karte. Das Ziel liegt jetzt bei Everests offizieller Höhe von 8.848,86 m — keine Lektion "
        "kam hinzu, und der Fortschrittsbalken füllt sich nicht mehr bei 8.000 m.",
    ),
    "README.es.md": (
        "Versión actual v0.27.2 (versionCode 157).",
        "Versión actual v0.28.0 (versionCode 158). El progreso de aprendizaje ya tiene forma: al "
        "subir se van desbloqueando cumbres reales. Una tarjeta nueva en la parte superior de la "
        "pantalla de ruta muestra la cumbre que corresponde a la altitud actual: 88 en total, desde "
        "el Helan Shan a 3.600 m hasta el Everest a 8.848 m, cada una con mapa del terreno (curvas "
        "de nivel donde un render 3D no se sostiene), altitud real, cordillera y puesto entre los "
        "ochomiles. En «Perfil → altitud actual» está la lista completa; las cumbres desbloqueadas "
        "abren su tarjeta. El objetivo pasa a la altitud oficial del Everest, 8.848,86 m: no se "
        "añadió ninguna lección y la barra ya no se llena a los 8.000 m.",
    ),
    "README.fr.md": (
        "Version actuelle v0.27.2 (versionCode 157).",
        "Version actuelle v0.28.0 (versionCode 158). La progression a désormais une forme : à "
        "mesure qu’on monte, de vrais sommets se débloquent. Une carte en haut de l’écran Parcours "
        "montre le sommet correspondant à l’altitude du moment — 88 au total, du Helan Shan à "
        "3 600 m à l’Everest à 8 848 m, chacun avec sa carte de terrain (courbes de niveau quand un "
        "rendu 3D ne tient pas), son altitude réelle, sa chaîne et son rang parmi les 8 000. Dans "
        "« Profil → altitude actuelle » se trouve la liste complète ; les sommets débloqués ouvrent "
        "leur fiche. L’objectif passe à l’altitude officielle de l’Everest, 8 848,86 m : aucune "
        "leçon ajoutée, et la barre ne se remplit plus à 8 000 m.",
    ),
    "README.it.md": (
        "Versione attuale v0.27.2 (versionCode 157).",
        "Versione attuale v0.28.0 (versionCode 158). Il progresso ora ha una forma: salendo si "
        "sbloccano vere cime innevate. Una nuova scheda in cima alla schermata del percorso mostra "
        "la vetta corrispondente all’altitudine attuale: 88 in tutto, dal Helan Shan a 3.600 m "
        "all’Everest a 8.848 m, ognuna con carta del terreno (curve di livello dove un render 3D "
        "non regge), quota reale, catena e posizione tra gli ottomila. In «Profilo → quota attuale» "
        "c’è l’elenco completo; le vette sbloccate aprono la loro scheda. L’obiettivo passa alla "
        "quota ufficiale dell’Everest, 8.848,86 m: nessuna lezione aggiunta e la barra non si "
        "riempie più a 8.000 m.",
    ),
    "README.ru.md": (
        "Текущая версия v0.27.2 (versionCode 157).",
        "Текущая версия v0.28.0 (versionCode 158). У прогресса появилась форма: поднимаясь, вы "
        "открываете настоящие снежные вершины. Новая карточка вверху экрана маршрута показывает "
        "вершину, соответствующую текущей высоте — всего 88, от Хэланьшаня на 3 600 м до Эвереста "
        "на 8 848 м, у каждой карта рельефа (где объёмный рендер не тянет — горизонтали), реальная "
        "высота, хребет и место среди восьмитысячников. В разделе «Профиль → текущая высота» "
        "лежит полный список; открытые вершины раскрывают карточку. Цель теперь — официальная "
        "высота Эвереста 8 848,86 м: уроков не прибавилось, и полоса прогресса больше не "
        "заполняется на 8 000 м.",
    ),
    "README.sv.md": (
        "Aktuell version v0.27.2 (versionCode 157).",
        "Aktuell version v0.28.0 (versionCode 158). Framstegen har fått en form: när du klättrar "
        "låses riktiga snötoppar upp efter vägen. Ett nytt kort högst upp på Rutt-skärmen visar "
        "toppen som hör till den aktuella höjden — 88 totalt, från Helan Shan på 3 600 m till "
        "Everest på 8 848 m, alla med terrängkarta (höjdkurvor där en 3D-rendering inte håller), "
        "verklig höjd, bergskedja och placering bland åttatusenmetarna. Under ”Profil → aktuell "
        "höjd” finns hela listan; upplåsta toppar öppnar sitt kort. Målet är nu Everests officiella "
        "höjd 8 848,86 m — inga lektioner lades till, och förloppsindikatorn fylls inte längre vid "
        "8 000 m.",
    ),
}

if __name__ == "__main__":
    for f in sorted(glob.glob("README*.md")):
        old, new = PARA[f]
        s = open(f, encoding="utf-8").read()
        assert old in s, f
        s = s.replace(old, new, 1)
        open(f, "w", encoding="utf-8").write(s)
        print("ok", f)

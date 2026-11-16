"""把山峰介绍的中文原文写进 MountainIntro.kt，译文写进各语言的山峰包。

中文原文改成从本脚本的 ZH 表生成 —— 一份数据只放在一处，
避免「脚本里一份、Kotlin 里一份」两处漂移。

用法：python3 tools/i18n/seed_mtn_intro.py
"""
import os
import re

KT = "app/src/main/java/com/ascent/climb/data/MountainIntro.kt"
I18N = "app/src/main/java/com/ascent/climb/data/i18n"

# ---- 中文原文（唯一真源） ----
ZH = {
    "haba": "哈巴雪山在云南香格里拉，与玉龙雪山隔着虎跳峡相望。"
            "它常被叫作「人生第一座雪山」：雪线以上是缓坡，没有冰川裂缝，技术难度低。"
            "但从 2,600 m 的哈巴村出发，一天要爬升近 2,800 m，真正拦人的是高原反应。"
            "纳西语里「哈巴」意为「金子之花朵」。",
}

# ---- 九语言译文（中文不在这里，直接读上面） ----
T = {
    "en": {
        "haba": "Haba sits in Shangri-La, Yunnan, across Tiger Leaping Gorge from Yulong. "
                "It is often called a first snow mountain: above the snowline the slope is gentle, "
                "there are no crevasses, and the technical difficulty is low. "
                "But from Haba village at 2,600 m the route gains nearly 2,800 m in a single day, "
                "and altitude sickness is the real obstacle. In Naxi, haba means flower of gold.",
    },
    "zh-TW": {
        "haba": "哈巴雪山在雲南香格里拉，與玉龍雪山隔著虎跳峽相望。"
                "它常被叫作「人生第一座雪山」：雪線以上是緩坡，沒有冰川裂隙，技術難度低。"
                "但從 2,600 m 的哈巴村出發，一天要爬升近 2,800 m，真正攔人的是高山症。"
                "納西語裡「哈巴」意為「金子之花朵」。",
    },
    "ja": {
        "haba": "哈巴雪山は雲南省シャングリラにある。虎跳峡を挟んで玉龍雪山と向き合う。"
                "雪線から上は緩やかな斜面でクレバスもなく、技術的な難度は低い。"
                "「人生最初の雪山」と呼ばれるゆえんだ。"
                "ただし標高 2,600 m の哈巴村から出発すると、一日で約 2,800 m を登ることになり、"
                "実際の敵は高山病である。ナシ語で「ハバ」は「金の花」を意味する。",
    },
    "de": {
        "haba": "Der Haba liegt in Shangri-La in Yunnan, getrennt durch die Tiger-Leaping-Schlucht vom Yulong. "
                "Man nennt ihn oft den ersten Schneeberg: oberhalb der Schneegrenze ist der Hang flach, "
                "es gibt keine Gletscherspalten, und der technische Schwierigkeitsgrad ist niedrig. "
                "Von Haba-Dorf auf 2.600 m gewinnt die Route an einem einzigen Tag jedoch fast 2.800 m, "
                "und die Höhenkrankheit ist der eigentliche Gegner. "
                "Auf Naxi bedeutet Haba Blume aus Gold.",
    },
    "es": {
        "haba": "El Haba se alza en Shangri-La, Yunnan, separado del Yulong por la garganta del Salto del Tigre. "
                "Suele llamarse la primera montaña nevada: por encima de la línea de nieve la pendiente es suave, "
                "no hay grietas de glaciar y la dificultad técnica es baja. "
                "Pero desde el pueblo de Haba, a 2.600 m, la ruta gana casi 2.800 m en un solo día, "
                "y el mal de altura es el verdadero rival. En naxi, haba significa flor de oro.",
    },
    "fr": {
        "haba": "Le Haba se dresse au Shangri-La, dans le Yunnan, séparé du Yulong par les gorges du Saut du Tigre. "
                "On l'appelle souvent la première montagne enneigée : au-dessus de la limite des neiges, "
                "la pente est douce, il n'y a pas de crevasses et la difficulté technique est faible. "
                "Mais depuis le village de Haba, à 2 600 m, l'itinéraire gagne près de 2 800 m en une seule journée, "
                "et le mal des montagnes est le vrai adversaire. En naxi, haba signifie fleur d'or.",
    },
    "it": {
        "haba": "L'Haba si trova nello Shangri-La, nello Yunnan, separato dallo Yulong dalla gola del Salto della Tigre. "
                "È spesso chiamata la prima montagna innevata: sopra il limite delle nevi il pendio è dolce, "
                "non ci sono crepacci e la difficoltà tecnica è bassa. "
                "Ma dal villaggio di Haba, a 2.600 m, l'itinerario guadagna quasi 2.800 m in un solo giorno, "
                "e il mal di montagna è il vero avversario. In naxi, haba significa fiore d'oro.",
    },
    "ru": {
        "haba": "Хаба стоит в Шангри-Ла в Юньнани, отделённый от Юйлун ущельем Прыгающего тигра. "
                "Его часто называют первой снежной вершиной: выше снеговой линии склон пологий, "
                "трещин нет, а техническая сложность невелика. "
                "Но из деревни Хаба на высоте 2 600 м маршрут набирает почти 2 800 м за один день, "
                "и настоящий противник здесь — горная болезнь. "
                "На языке наси хаба означает золотой цветок.",
    },
    "sv": {
        "haba": "Haba ligger i Shangri-La i Yunnan, skild från Yulong av Tiger Leaping Gorge. "
                "Den kallas ofta det första snöberget: ovanför snögränsen är sluttningen flack, "
                "det finns inga glaciärsprickor och den tekniska svårigheten är låg. "
                "Men från byn Haba på 2 600 m vinner rutten nästan 2 800 m på en enda dag, "
                "och höjdsjuka är den verkliga motståndaren. På naxi betyder haba guldblomma.",
    },
}

PACK_FILE = {
    "en": "PackEnMtn.kt",
    "zh-TW": "PackZhTwMtn.kt",
    "ja": "PackJaMtn.kt",
    "de": "PackDeMtn.kt",
    "es": "PackEsMtn.kt",
    "fr": "PackFrMtn.kt",
    "it": "PackItMtn.kt",
    "ru": "PackRuMtn.kt",
    "sv": "PackSvMtn.kt",
}


def kt_escape(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')


def rewrite_zh():
    """按 ZH 表重建 MountainIntro.kt 里的 mapOf 段。"""
    src = open(KT, encoding="utf-8").read()
    lines = []
    for k, v in ZH.items():
        # 每句一行，读起来像人写的，不像生成器吐的
        parts = re.findall(r"[^。]*。", v) or [v]
        body = "\n".join(
            '            "%s"%s' % (kt_escape(p), " +" if i < len(parts) - 1 else "")
            for i, p in enumerate(parts)
        )
        lines.append('        "%s" to\n%s' % (k, body))
    table = "mapOf(\n" + "\n".join(lines) + "\n    )"
    new = re.sub(
        r"private val ZH: Map<String, String> = mapOf\(.*?\n    \)",
        "private val ZH: Map<String, String> = " + table,
        src,
        count=1,
        flags=re.S,
    )
    assert new != src
    open(KT, "w", encoding="utf-8").write(new)
    print("MountainIntro.kt 已按 %d 条原文重建" % len(ZH))


def insert_into_pack(lang):
    p = os.path.join(I18N, PACK_FILE[lang])
    s = open(p, encoding="utf-8").read()
    for mid, text in T[lang].items():
        key = "mtn.%s.intro" % mid
        entry = '    "%s" to "%s",\n' % (key, kt_escape(text))
        pat = re.compile(r'    "%s" to "[^"]*",\n' % re.escape(key))
        if pat.search(s):
            s = pat.sub(lambda m: entry, s, count=1)
        else:
            anchor = '    "mtn.snow.none" to '
            i = s.index(anchor)
            s = s[:i] + entry + "\n" + s[i:]
    open(p, "w", encoding="utf-8").write(s)
    print("  %-6s %s" % (lang, PACK_FILE[lang]))


if __name__ == "__main__":
    rewrite_zh()
    for lang in T:
        insert_into_pack(lang)

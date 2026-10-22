"""内容层多语言工具的共用库。

期望键从 bank/bank.json 推算（先跑 tools/contentcheck/run.sh）。
复习副本（review=true）在运行时会沿用原题 id，因此不计入期望键。
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data")
PACK_DIR = os.path.join(DATA, "i18n")
BANK = os.path.join(ROOT, "bank/bank.json")

ENTRY = re.compile(r'"([^"]+)" to "((?:[^"\\]|\\.)*)"')

PACKS = {
    "en": ["PackEn", "PackEnBrief", "PackEnHandbook", "PackEnQuiz", "PackEnQuiz2", "PackEnQuiz3"],
    "zh-TW": ["PackZhTw", "PackZhTwBrief", "PackZhTwEntry", "PackZhTwHandbook", "PackZhTwQuiz",
              "PackZhTwQuiz2", "PackZhTwQuiz3"],
    "ja": ["PackJa", "PackJaBrief", "PackJaEntry", "PackJaHandbook", "PackJaQuiz",
           "PackJaQuiz2", "PackJaQuiz3"],
    "ru": ["PackRu", "PackRuBrief", "PackRuEntry", "PackRuHandbook", "PackRuQuiz",
           "PackRuQuiz2", "PackRuQuiz3"],
    "fr": ["PackFr", "PackFrBrief", "PackFrEntry", "PackFrHandbook", "PackFrQuiz"],
    "sv": ["PackSv", "PackSvBrief", "PackSvEntry", "PackSvHandbook", "PackSvQuiz"],
    "de": ["PackDe", "PackDeBrief", "PackDeEntry", "PackDeHandbook", "PackDeQuiz",
           "PackDeQuiz2", "PackDeQuiz3"],
    "es": ["PackEs", "PackEsBrief", "PackEsEntry", "PackEsHandbook", "PackEsQuiz", "PackEsQuiz2", "PackEsQuiz3"],
    "it": ["PackIt", "PackItBrief", "PackItEntry", "PackItHandbook", "PackItQuiz"],
}

# 各层写到哪个文件（按语言）。新增语言时把这里补上，其余脚本不用改。
TARGETS = {
    "en": {"entry": "PackEn.kt", "brief": "PackEnBrief.kt",
           "handbook": "PackEnHandbook.kt", "quiz": "PackEnQuiz3.kt"},
    "zh-TW": {"entry": "PackZhTwEntry.kt", "brief": "PackZhTwBrief.kt",
              "handbook": "PackZhTwHandbook.kt", "quiz": "PackZhTwQuiz3.kt"},
    "ja": {"entry": "PackJaEntry.kt", "brief": "PackJaBrief.kt",
           "handbook": "PackJaHandbook.kt", "quiz": "PackJaQuiz3.kt"},
    "ru": {"entry": "PackRuEntry.kt", "brief": "PackRuBrief.kt",
           "handbook": "PackRuHandbook.kt", "quiz": "PackRuQuiz3.kt"},
    "fr": {"entry": "PackFrEntry.kt", "brief": "PackFrBrief.kt",
           "handbook": "PackFrHandbook.kt", "quiz": "PackFrQuiz.kt"},
    "sv": {"entry": "PackSvEntry.kt", "brief": "PackSvBrief.kt",
           "handbook": "PackSvHandbook.kt", "quiz": "PackSvQuiz.kt"},
    "de": {"entry": "PackDeEntry.kt", "brief": "PackDeBrief.kt",
           "handbook": "PackDeHandbook.kt", "quiz": "PackDeQuiz3.kt"},
    "es": {"entry": "PackEsEntry.kt", "brief": "PackEsBrief.kt",
           "handbook": "PackEsHandbook.kt", "quiz": "PackEsQuiz3.kt"},
    "it": {"entry": "PackItEntry.kt", "brief": "PackItBrief.kt",
           "handbook": "PackItHandbook.kt", "quiz": "PackItQuiz.kt"},
}

# 向后兼容：默认语言是英文
TARGET = TARGETS["en"]


def target(lang, layer):
    return TARGETS[lang][layer]



def bank():
    return json.load(open(BANK, encoding="utf-8"))


def lessons():
    out = {}
    for tr in bank()["tracks"]:
        for u in tr["units"]:
            for l in u["lessons"]:
                out[l["id"]] = l
    return out


def load(lang="en"):
    out = {}
    for name in PACKS[lang]:
        path = os.path.join(PACK_DIR, name + ".kt")
        if os.path.exists(path):
            out.update(dict(ENTRY.findall(open(path, encoding="utf-8").read())))
    return out


def quiz_keys(lid, lesson):
    ks = []
    for i, q in enumerate(lesson["questions"]):
        if q.get("review"):
            continue
        base = "q.%s#%d" % (lid, i)
        t = q["type"]
        ks.append(base + ".p")
        if t in ("single", "multi", "order"):
            ks += ["%s.o%d" % (base, n) for n in range(len(q["options"]))]
        elif t in ("fill", "calc"):
            ks += ["%s.a%d" % (base, n) for n in range(len(q["answers"]))]
        elif t == "match":
            ks += ["%s.l%d" % (base, n) for n in range(len(q["pairsLeft"]))]
            ks += ["%s.r%d" % (base, n) for n in range(len(q["pairsRight"]))]
        elif t == "scenario":
            if q.get("context"):
                ks.append(base + ".c")
            for si, st in enumerate(q.get("steps", [])):
                ks.append("%s.s%d.p" % (base, si))
                ks += ["%s.s%d.o%d" % (base, si, n) for n in range(len(st["options"]))]
                ks.append("%s.s%d.e" % (base, si))
        ks.append(base + ".e")
    return ks


def brief_keys(lid, lesson):
    return ["lesson.%s.brief.%d" % (lid, n) for n in range(1, len(lesson.get("brief", [])) + 1)]


def total_keys():
    ls = lessons()
    return sum(len(quiz_keys(k, v)) for k, v in ls.items())

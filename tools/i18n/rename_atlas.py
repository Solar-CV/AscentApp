"""一次改掉「历史山峰」相关的十语言文案，并补上「关于这座山」。

改名口径：历史 → 百科。原来的表述暗示「只有到达过的山才在列表里」，
实际列表是全量，叫历史名不副实。
"""
import os
import re

RES = "app/src/main/res"

# 语言目录 → {键: 新值}
T = {
    "values": {
        "mtn_entry_title": "山峰百科",
        "mtn_history_title": "山峰百科",
        "mtn_jump_count": "已解锁 %1$d 座 · 山峰百科",
        "mtn_jump_none": "山峰百科 · 尚未解锁",
        "mtn_intro_title": "关于这座山",
    },
    "values-zh-rTW": {
        "mtn_entry_title": "山峰百科",
        "mtn_history_title": "山峰百科",
        "mtn_jump_count": "已解鎖 %1$d 座 · 山峰百科",
        "mtn_jump_none": "山峰百科 · 尚未解鎖",
        "mtn_intro_title": "關於這座山",
    },
    "values-en": {
        "mtn_entry_title": "Peak atlas",
        "mtn_history_title": "Peak atlas",
        "mtn_jump_count": "%1$d unlocked · Atlas",
        "mtn_jump_none": "Atlas · none yet",
        "mtn_intro_title": "About this peak",
    },
    "values-ja": {
        "mtn_entry_title": "山岳図鑑",
        "mtn_history_title": "山岳図鑑",
        "mtn_jump_count": "%1$d 座到達 · 山岳図鑑",
        "mtn_jump_none": "山岳図鑑 · まだ到達なし",
        "mtn_intro_title": "この山について",
    },
    "values-de": {
        "mtn_entry_title": "Gipfellexikon",
        "mtn_history_title": "Gipfellexikon",
        "mtn_jump_count": "%1$d freigeschaltet · Lexikon",
        "mtn_jump_none": "Lexikon · noch keine",
        "mtn_intro_title": "Über diesen Gipfel",
    },
    "values-es": {
        "mtn_entry_title": "Enciclopedia de cumbres",
        "mtn_history_title": "Enciclopedia de cumbres",
        "mtn_jump_count": "%1$d desbloqueadas · Enciclopedia",
        "mtn_jump_none": "Enciclopedia · ninguna aún",
        "mtn_intro_title": "Sobre esta cumbre",
    },
    "values-fr": {
        "mtn_entry_title": "Encyclopédie des sommets",
        "mtn_history_title": "Encyclopédie des sommets",
        "mtn_jump_count": "%1$d débloqués · Encyclopédie",
        "mtn_jump_none": "Encyclopédie · aucun encore",
        "mtn_intro_title": "À propos de ce sommet",
    },
    "values-it": {
        "mtn_entry_title": "Enciclopedia delle vette",
        "mtn_history_title": "Enciclopedia delle vette",
        "mtn_jump_count": "%1$d sbloccate · Enciclopedia",
        "mtn_jump_none": "Enciclopedia · nessuna",
        "mtn_intro_title": "Su questa vetta",
    },
    "values-ru": {
        "mtn_entry_title": "Энциклопедия вершин",
        "mtn_history_title": "Энциклопедия вершин",
        "mtn_jump_count": "Открыто: %1$d · Энциклопедия",
        "mtn_jump_none": "Энциклопедия · пока пусто",
        "mtn_intro_title": "Об этой вершине",
    },
    "values-sv": {
        "mtn_entry_title": "Toppencyklopedi",
        "mtn_history_title": "Toppencyklopedi",
        "mtn_jump_count": "%1$d upplåsta · Uppslagsverk",
        "mtn_jump_none": "Uppslagsverk · inga än",
        "mtn_intro_title": "Om den här toppen",
    },
}


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main():
    for d, repl in T.items():
        p = os.path.join(RES, d, "strings.xml")
        s = open(p, encoding="utf-8").read()
        for k, v in repl.items():
            pat = re.compile(r'(<string name="%s">)(.*?)(</string>)' % re.escape(k), re.S)
            if pat.search(s):
                s = pat.sub(lambda m: m.group(1) + esc(v) + m.group(3), s, count=1)
            else:
                # 新键：插在 mtn_note_render 之后，保持 mtn_ 段连续
                anchor = re.search(r'(<string name="mtn_note_render">.*?</string>)\n', s, re.S)
                assert anchor, (d, "锚点缺失")
                s = s[: anchor.end(1)] + '\n    <string name="%s">%s</string>' % (k, esc(v)) + s[anchor.end(1):]
        open(p, "w", encoding="utf-8").write(s)
        print("ok", d)


if __name__ == "__main__":
    main()

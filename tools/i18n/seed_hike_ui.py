#!/usr/bin/env python3
"""一次性：把徒步百科的 UI 文案写进 10 个 strings.xml。

只做插入，不改既有条目；重复执行会报错（已存在同名 key 就退出）。
字符串里的半角单引号按 Android 资源规则转义成 \\'。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "app/src/main/res")
DIRS = {
    "zh-CN": "values",
    "en": "values-en",
    "zh-TW": "values-zh-rTW",
    "ja": "values-ja",
    "ru": "values-ru",
    "de": "values-de",
    "es": "values-es",
    "fr": "values-fr",
    "sv": "values-sv",
    "it": "values-it",
}

S = {
    "zh-CN": {
        "hike_entry_title": "徒步百科",
        "hike_entry_none": "尚未解锁",
        "hike_entry_progress": "已解锁 %1$d / %2$d",
        "hike_history_title": "徒步百科",
        "hike_history_summary": "已解锁 %1$d / %2$d 条 · 最高 %3$s m",
        "hike_jump_count": "已解锁 %1$d 条 · 徒步百科",
        "hike_jump_none": "徒步百科 · 尚未解锁",
        "hike_row_milestone": "解锁 %1$s m · 最高 %2$s m",
        "hike_detail_progress": "当前 %1$s m · 解锁于 %2$s m",
        "hike_metric_distance": "距离",
        "hike_metric_gain": "累计爬升",
        "hike_metric_days": "天数",
        "hike_note_profile": "海拔剖面 · 按主要营地与垭口高程绘制，非实景地图",
        "hike_intro_title": "关于这条线",
        "hike_high_line": "最高点 %1$s m · %2$s",
    },
    "zh-TW": {
        "hike_entry_title": "徒步百科",
        "hike_entry_none": "尚未解鎖",
        "hike_entry_progress": "已解鎖 %1$d / %2$d",
        "hike_history_title": "徒步百科",
        "hike_history_summary": "已解鎖 %1$d / %2$d 條 · 最高 %3$s m",
        "hike_jump_count": "已解鎖 %1$d 條 · 徒步百科",
        "hike_jump_none": "徒步百科 · 尚未解鎖",
        "hike_row_milestone": "解鎖 %1$s m · 最高 %2$s m",
        "hike_detail_progress": "目前 %1$s m · 解鎖於 %2$s m",
        "hike_metric_distance": "距離",
        "hike_metric_gain": "累計爬升",
        "hike_metric_days": "天數",
        "hike_note_profile": "海拔剖面 · 依主要營地與埡口高程繪製，非實景地圖",
        "hike_intro_title": "關於這條線",
        "hike_high_line": "最高點 %1$s m · %2$s",
    },
    "en": {
        "hike_entry_title": "Trek atlas",
        "hike_entry_none": "None yet",
        "hike_entry_progress": "%1$d / %2$d unlocked",
        "hike_history_title": "Trek atlas",
        "hike_history_summary": "%1$d / %2$d unlocked · highest %3$s m",
        "hike_jump_count": "%1$d unlocked · Trek atlas",
        "hike_jump_none": "Trek atlas · none yet",
        "hike_row_milestone": "Unlocks at %1$s m · %2$s m high",
        "hike_detail_progress": "Now %1$s m · unlocks at %2$s m",
        "hike_metric_distance": "Distance",
        "hike_metric_gain": "Total ascent",
        "hike_metric_days": "Days",
        "hike_note_profile": "Elevation profile · drawn from main camps and passes, not a map",
        "hike_intro_title": "About this route",
        "hike_high_line": "High point %1$s m · %2$s",
    },
    "ja": {
        "hike_entry_title": "トレッキング図鑑",
        "hike_entry_none": "まだ到達なし",
        "hike_entry_progress": "%1$d / %2$d 本到達",
        "hike_history_title": "トレッキング図鑑",
        "hike_history_summary": "%1$d / %2$d 本到達 · 最高 %3$s m",
        "hike_jump_count": "%1$d 本到達 · トレッキング図鑑",
        "hike_jump_none": "トレッキング図鑑 · まだ到達なし",
        "hike_row_milestone": "%1$s m で到達 · 最高 %2$s m",
        "hike_detail_progress": "現在 %1$s m · %2$s m で到達",
        "hike_metric_distance": "距離",
        "hike_metric_gain": "累計登り",
        "hike_metric_days": "日数",
        "hike_note_profile": "標高プロファイル · 主なキャンプと峠の標高から作成、実景地図ではありません",
        "hike_intro_title": "このルートについて",
        "hike_high_line": "最高点 %1$s m · %2$s",
    },
    "ru": {
        "hike_entry_title": "Атлас маршрутов",
        "hike_entry_none": "Пока нет",
        "hike_entry_progress": "Открыто %1$d / %2$d",
        "hike_history_title": "Атлас маршрутов",
        "hike_history_summary": "Открыто %1$d / %2$d · высшая точка %3$s м",
        "hike_jump_count": "Открыто %1$d · Атлас маршрутов",
        "hike_jump_none": "Атлас маршрутов · пока нет",
        "hike_row_milestone": "Открывается на %1$s м · высшая точка %2$s м",
        "hike_detail_progress": "Сейчас %1$s м · открывается на %2$s м",
        "hike_metric_distance": "Дистанция",
        "hike_metric_gain": "Набор высоты",
        "hike_metric_days": "Дней",
        "hike_note_profile": "Профиль высот · построен по высотам лагерей и перевалов, не карта",
        "hike_intro_title": "Об этом маршруте",
        "hike_high_line": "Высшая точка %1$s м · %2$s",
    },
    "de": {
        "hike_entry_title": "Wanderatlas",
        "hike_entry_none": "Noch keine",
        "hike_entry_progress": "%1$d / %2$d freigeschaltet",
        "hike_history_title": "Wanderatlas",
        "hike_history_summary": "%1$d / %2$d freigeschaltet · höchster Punkt %3$s m",
        "hike_jump_count": "%1$d freigeschaltet · Wanderatlas",
        "hike_jump_none": "Wanderatlas · noch keine",
        "hike_row_milestone": "Freigabe bei %1$s m · höchster Punkt %2$s m",
        "hike_detail_progress": "Jetzt %1$s m · Freigabe bei %2$s m",
        "hike_metric_distance": "Distanz",
        "hike_metric_gain": "Aufstieg",
        "hike_metric_days": "Tage",
        "hike_note_profile": "Höhenprofil · aus Lager- und Passhöhen gezeichnet, keine Karte",
        "hike_intro_title": "Über diese Route",
        "hike_high_line": "Höchster Punkt %1$s m · %2$s",
    },
    "es": {
        "hike_entry_title": "Atlas de rutas",
        "hike_entry_none": "Ninguna todavía",
        "hike_entry_progress": "%1$d / %2$d desbloqueadas",
        "hike_history_title": "Atlas de rutas",
        "hike_history_summary": "%1$d / %2$d desbloqueadas · punto más alto %3$s m",
        "hike_jump_count": "%1$d desbloqueada · Atlas de rutas",
        "hike_jump_none": "Atlas de rutas · ninguna todavía",
        "hike_row_milestone": "Se desbloquea a %1$s m · punto más alto %2$s m",
        "hike_detail_progress": "Ahora %1$s m · se desbloquea a %2$s m",
        "hike_metric_distance": "Distancia",
        "hike_metric_gain": "Desnivel positivo",
        "hike_metric_days": "Días",
        "hike_note_profile": "Perfil de altitud · trazado con altitudes de campamentos y puertos, no es un mapa",
        "hike_intro_title": "Sobre esta ruta",
        "hike_high_line": "Punto más alto %1$s m · %2$s",
    },
    "fr": {
        "hike_entry_title": "Atlas des randonnées",
        "hike_entry_none": "Aucune encore",
        "hike_entry_progress": "%1$d / %2$d débloquées",
        "hike_history_title": "Atlas des randonnées",
        "hike_history_summary": "%1$d / %2$d débloquées · point culminant %3$s m",
        "hike_jump_count": "%1$d débloquée · Atlas des randonnées",
        "hike_jump_none": "Atlas des randonnées · aucune encore",
        "hike_row_milestone": "Débloquée à %1$s m · point culminant %2$s m",
        "hike_detail_progress": "Actuel %1$s m · débloquée à %2$s m",
        "hike_metric_distance": "Distance",
        "hike_metric_gain": "Dénivelé positif",
        "hike_metric_days": "Jours",
        "hike_note_profile": "Profil altimétrique · tracé d'après les camps et cols principaux, pas une carte",
        "hike_intro_title": "À propos de cet itinéraire",
        "hike_high_line": "Point culminant %1$s m · %2$s",
    },
    "sv": {
        "hike_entry_title": "Vandringsatlas",
        "hike_entry_none": "Inga ännu",
        "hike_entry_progress": "%1$d / %2$d upplåsta",
        "hike_history_title": "Vandringsatlas",
        "hike_history_summary": "%1$d / %2$d upplåsta · högsta punkt %3$s m",
        "hike_jump_count": "%1$d upplåsta · Vandringsatlas",
        "hike_jump_none": "Vandringsatlas · inga ännu",
        "hike_row_milestone": "Låses upp vid %1$s m · högsta punkt %2$s m",
        "hike_detail_progress": "Nu %1$s m · låses upp vid %2$s m",
        "hike_metric_distance": "Distans",
        "hike_metric_gain": "Stigning",
        "hike_metric_days": "Dagar",
        "hike_note_profile": "Höjdprofil · ritad efter läger- och passhöjder, inte en karta",
        "hike_intro_title": "Om leden",
        "hike_high_line": "Högsta punkt %1$s m · %2$s",
    },
    "it": {
        "hike_entry_title": "Atlante dei trekking",
        "hike_entry_none": "Nessuno ancora",
        "hike_entry_progress": "%1$d / %2$d sbloccati",
        "hike_history_title": "Atlante dei trekking",
        "hike_history_summary": "%1$d / %2$d sbloccati · punto più alto %3$s m",
        "hike_jump_count": "%1$d sbloccati · Atlante dei trekking",
        "hike_jump_none": "Atlante dei trekking · nessuno ancora",
        "hike_row_milestone": "Sbloccato a %1$s m · punto più alto %2$s m",
        "hike_detail_progress": "Ora %1$s m · sbloccato a %2$s m",
        "hike_metric_distance": "Distanza",
        "hike_metric_gain": "Dislivello positivo",
        "hike_metric_days": "Giorni",
        "hike_note_profile": "Profilo altimetrico · tracciato dalle quote di campi e valichi, non è una mappa",
        "hike_intro_title": "Su questo itinerario",
        "hike_high_line": "Punto più alto %1$s m · %2$s",
    },
}

P = {
    "zh-CN": [("other", "%1$d 天")],
    "zh-TW": [("other", "%1$d 天")],
    "en": [("one", "%1$d day"), ("other", "%1$d days")],
    "ja": [("other", "%1$d 日")],
    "ru": [("one", "%1$d день"), ("few", "%1$d дня"), ("many", "%1$d дней"), ("other", "%1$d дня")],
    "de": [("one", "%1$d Tag"), ("other", "%1$d Tage")],
    "es": [("one", "%1$d día"), ("other", "%1$d días")],
    "fr": [("one", "%1$d jour"), ("other", "%1$d jours")],
    "sv": [("one", "%1$d dag"), ("other", "%1$d dagar")],
    "it": [("one", "%1$d giorno"), ("other", "%1$d giorni")],
}


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'").replace("&", "&amp;").replace("<", "&lt;")


def main():
    for lang, d in DIRS.items():
        path = os.path.join(RES, d, "strings.xml")
        s = open(path, encoding="utf-8").read()
        existing = set(re.findall(r'<(?:string|plurals) name="([^"]+)"', s))
        dup = [k for k in S[lang] if k in existing]
        if "hike_days" in existing:
            dup.append("hike_days")
        if dup:
            sys.exit("%s 已存在: %s" % (lang, dup))
        block = ["", "    <!-- 徒步百科 -->"]
        for k, v in S[lang].items():
            block.append('    <string name="%s">%s</string>' % (k, esc(v)))
        block.append('    <plurals name="hike_days">')
        for q, v in P[lang]:
            block.append('        <item quantity="%s">%s</item>' % (q, esc(v)))
        block.append("    </plurals>")
        block.append("")
        block.append("</resources>")
        assert s.rstrip().endswith("</resources>")
        s = s.rstrip()[: -len("</resources>")].rstrip() + "\n" + "\n".join(block) + "\n"
        open(path, "w", encoding="utf-8").write(s)
        print("%-6s %d 条" % (lang, len(S[lang]) + 1))


if __name__ == "__main__":
    main()

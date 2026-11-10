"""一次性：把山峰卡片/历史面板的 UI 文案写进 10 个 strings.xml。

只做插入，不改既有条目；重复执行会跳过（已存在同名 key 就报错退出）。
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "app/src/main/res")

ZH = {
    "mtn_current": "当前 %1$s m",
    "mtn_remaining": "还差 %1$s m",
    "mtn_summit_reached": "已到达",
    "mtn_unlocked": "已解锁",
    "mtn_row_locked": "未到达",
    "mtn_jump_count": "已解锁 %1$d 座 · 历史记录",
    "mtn_jump_none": "历史记录 · 尚未解锁",
    "mtn_milestone_rank": "里程碑 %1$s m · 世界第 %2$d 高",
    "mtn_milestone_range": "里程碑 %1$s m · %2$s",
    "mtn_metric_span": "山体跨度",
    "mtn_metric_snowline": "雪线",
    "mtn_metric_range": "所在山脉",
    "mtn_note_contour": "等高线地形图 · 等高距 200 m · 依据公开高程数据绘制",
    "mtn_note_render": "地形示意图 · 依据公开高程数据绘制，非实景照片",
    "mtn_entry_title": "历史山峰",
    "mtn_entry_none": "尚未解锁",
    "mtn_entry_progress": "已解锁 %1$d / %2$d",
    "mtn_history_title": "山峰历史",
    "mtn_history_summary": "已解锁 %1$d / %2$d 座 · 八千米 %3$d / %4$d",
    "mtn_row_rank": "世界第 %1$d 高 · 山高 %2$s m",
    "mtn_row_milestone": "里程碑 %1$s m · 山高 %2$s m",
    "mtn_detail_progress": "当前 %1$s m · 里程碑 %2$s m",
}

EN = {
    "mtn_current": "Now %1$s m",
    "mtn_remaining": "%1$s m to go",
    "mtn_summit_reached": "Reached",
    "mtn_unlocked": "Unlocked",
    "mtn_row_locked": "Not yet",
    "mtn_jump_count": "%1$d unlocked · History",
    "mtn_jump_none": "History · none yet",
    "mtn_milestone_rank": "Milestone %1$s m · #%2$d in the world",
    "mtn_milestone_range": "Milestone %1$s m · %2$s",
    "mtn_metric_span": "Massif span",
    "mtn_metric_snowline": "Snow line",
    "mtn_metric_range": "Range",
    "mtn_note_contour": "Contour map · 200 m interval · drawn from public elevation data",
    "mtn_note_render": "Terrain sketch · drawn from public elevation data, not a photograph",
    "mtn_entry_title": "Peak history",
    "mtn_entry_none": "None yet",
    "mtn_entry_progress": "%1$d / %2$d unlocked",
    "mtn_history_title": "Peak history",
    "mtn_history_summary": "%1$d / %2$d unlocked · 8000ers %3$d / %4$d",
    "mtn_row_rank": "#%1$d in the world · %2$s m high",
    "mtn_row_milestone": "Milestone %1$s m · %2$s m high",
    "mtn_detail_progress": "Now %1$s m · milestone %2$s m",
}

TW = {
    "mtn_current": "目前 %1$s m",
    "mtn_remaining": "還差 %1$s m",
    "mtn_summit_reached": "已抵達",
    "mtn_unlocked": "已解鎖",
    "mtn_row_locked": "未抵達",
    "mtn_jump_count": "已解鎖 %1$d 座 · 歷史紀錄",
    "mtn_jump_none": "歷史紀錄 · 尚未解鎖",
    "mtn_milestone_rank": "里程碑 %1$s m · 世界第 %2$d 高",
    "mtn_milestone_range": "里程碑 %1$s m · %2$s",
    "mtn_metric_span": "山體跨度",
    "mtn_metric_snowline": "雪線",
    "mtn_metric_range": "所在山脈",
    "mtn_note_contour": "等高線地形圖 · 等高距 200 m · 依據公開高程資料繪製",
    "mtn_note_render": "地形示意圖 · 依據公開高程資料繪製，非實景照片",
    "mtn_entry_title": "歷史山峰",
    "mtn_entry_none": "尚未解鎖",
    "mtn_entry_progress": "已解鎖 %1$d / %2$d",
    "mtn_history_title": "山峰歷史",
    "mtn_history_summary": "已解鎖 %1$d / %2$d 座 · 八千米 %3$d / %4$d",
    "mtn_row_rank": "世界第 %1$d 高 · 山高 %2$s m",
    "mtn_row_milestone": "里程碑 %1$s m · 山高 %2$s m",
    "mtn_detail_progress": "目前 %1$s m · 里程碑 %2$s m",
}

JA = {
    "mtn_current": "現在 %1$s m",
    "mtn_remaining": "あと %1$s m",
    "mtn_summit_reached": "登頂済み",
    "mtn_unlocked": "到達済み",
    "mtn_row_locked": "未到達",
    "mtn_jump_count": "%1$d 座到達 · 履歴",
    "mtn_jump_none": "履歴 · まだ到達なし",
    "mtn_milestone_rank": "マイルストーン %1$s m · 世界第 %2$d 位",
    "mtn_milestone_range": "マイルストーン %1$s m · %2$s",
    "mtn_metric_span": "山体の広がり",
    "mtn_metric_snowline": "雪線",
    "mtn_metric_range": "所属山脈",
    "mtn_note_contour": "等高線地形図 · 等高間隔 200 m · 公開標高データに基づく作図",
    "mtn_note_render": "地形スケッチ · 公開標高データに基づく作図（実写ではありません）",
    "mtn_entry_title": "山峰履歴",
    "mtn_entry_none": "まだ到達なし",
    "mtn_entry_progress": "%1$d / %2$d 座到達",
    "mtn_history_title": "山峰履歴",
    "mtn_history_summary": "%1$d / %2$d 座到達 · 8000m峰 %3$d / %4$d",
    "mtn_row_rank": "世界第 %1$d 位 · 標高 %2$s m",
    "mtn_row_milestone": "マイルストーン %1$s m · 標高 %2$s m",
    "mtn_detail_progress": "現在 %1$s m · マイルストーン %2$s m",
}

RU = {
    "mtn_current": "Сейчас %1$s м",
    "mtn_remaining": "осталось %1$s м",
    "mtn_summit_reached": "Достигнута",
    "mtn_unlocked": "Открыта",
    "mtn_row_locked": "Не достигнута",
    "mtn_jump_count": "Открыто: %1$d · История",
    "mtn_jump_none": "История · пока пусто",
    "mtn_milestone_rank": "Веха %1$s м · %2$d-я в мире",
    "mtn_milestone_range": "Веха %1$s м · %2$s",
    "mtn_metric_span": "Ширина массива",
    "mtn_metric_snowline": "Снеговая линия",
    "mtn_metric_range": "Горная система",
    "mtn_note_contour": "Карта горизонталей · сечение 200 м · по открытым данным о высотах",
    "mtn_note_render": "Схема рельефа · по открытым данным о высотах, не фотография",
    "mtn_entry_title": "История вершин",
    "mtn_entry_none": "Пока пусто",
    "mtn_entry_progress": "Открыто %1$d / %2$d",
    "mtn_history_title": "История вершин",
    "mtn_history_summary": "Открыто %1$d / %2$d · восьмитысячники %3$d / %4$d",
    "mtn_row_rank": "%1$d-я в мире · высота %2$s м",
    "mtn_row_milestone": "Веха %1$s м · высота %2$s м",
    "mtn_detail_progress": "Сейчас %1$s м · веха %2$s м",
}

DE = {
    "mtn_current": "Jetzt %1$s m",
    "mtn_remaining": "noch %1$s m",
    "mtn_summit_reached": "Erreicht",
    "mtn_unlocked": "Freigeschaltet",
    "mtn_row_locked": "Nicht erreicht",
    "mtn_jump_count": "%1$d freigeschaltet · Verlauf",
    "mtn_jump_none": "Verlauf · noch keine",
    "mtn_milestone_rank": "Meilenstein %1$s m · Nr. %2$d der Welt",
    "mtn_milestone_range": "Meilenstein %1$s m · %2$s",
    "mtn_metric_span": "Massivbreite",
    "mtn_metric_snowline": "Schneegrenze",
    "mtn_metric_range": "Gebirge",
    "mtn_note_contour": "Höhenlinienkarte · 200 m Abstand · nach öffentlichen Höhendaten gezeichnet",
    "mtn_note_render": "Geländeskizze · nach öffentlichen Höhendaten gezeichnet, keine Fotografie",
    "mtn_entry_title": "Gipfelverlauf",
    "mtn_entry_none": "Noch keine",
    "mtn_entry_progress": "%1$d / %2$d freigeschaltet",
    "mtn_history_title": "Gipfelverlauf",
    "mtn_history_summary": "%1$d / %2$d freigeschaltet · Achttausender %3$d / %4$d",
    "mtn_row_rank": "Nr. %1$d der Welt · %2$s m hoch",
    "mtn_row_milestone": "Meilenstein %1$s m · %2$s m hoch",
    "mtn_detail_progress": "Jetzt %1$s m · Meilenstein %2$s m",
}

FR = {
    "mtn_current": "Actuel %1$s m",
    "mtn_remaining": "encore %1$s m",
    "mtn_summit_reached": "Atteint",
    "mtn_unlocked": "Débloqué",
    "mtn_row_locked": "Non atteint",
    "mtn_jump_count": "%1$d débloqués · Historique",
    "mtn_jump_none": "Historique · aucun encore",
    "mtn_milestone_rank": "Jalon %1$s m · n° %2$d mondial",
    "mtn_milestone_range": "Jalon %1$s m · %2$s",
    "mtn_metric_span": "Emprise du massif",
    "mtn_metric_snowline": "Limite des neiges",
    "mtn_metric_range": "Massif",
    "mtn_note_contour": "Carte en courbes de niveau · équidistance 200 m · d\\'après des données d\\'altitude publiques",
    "mtn_note_render": "Croquis de terrain · d\\'après des données d\\'altitude publiques, pas une photo",
    "mtn_entry_title": "Historique des sommets",
    "mtn_entry_none": "Aucun encore",
    "mtn_entry_progress": "%1$d / %2$d débloqués",
    "mtn_history_title": "Historique des sommets",
    "mtn_history_summary": "%1$d / %2$d débloqués · plus de 8000 m %3$d / %4$d",
    "mtn_row_rank": "n° %1$d mondial · %2$s m",
    "mtn_row_milestone": "Jalon %1$s m · %2$s m d\\'altitude",
    "mtn_detail_progress": "Actuel %1$s m · jalon %2$s m",
}

ES = {
    "mtn_current": "Actual %1$s m",
    "mtn_remaining": "faltan %1$s m",
    "mtn_summit_reached": "Alcanzada",
    "mtn_unlocked": "Desbloqueada",
    "mtn_row_locked": "Sin alcanzar",
    "mtn_jump_count": "%1$d desbloqueadas · Historial",
    "mtn_jump_none": "Historial · ninguna aún",
    "mtn_milestone_rank": "Hito %1$s m · n.º %2$d del mundo",
    "mtn_milestone_range": "Hito %1$s m · %2$s",
    "mtn_metric_span": "Anchura del macizo",
    "mtn_metric_snowline": "Línea de nieve",
    "mtn_metric_range": "Cordillera",
    "mtn_note_contour": "Mapa de curvas de nivel · equidistancia 200 m · según datos públicos de altitud",
    "mtn_note_render": "Croquis del terreno · según datos públicos de altitud, no es una foto",
    "mtn_entry_title": "Historial de cumbres",
    "mtn_entry_none": "Ninguna aún",
    "mtn_entry_progress": "%1$d / %2$d desbloqueadas",
    "mtn_history_title": "Historial de cumbres",
    "mtn_history_summary": "%1$d / %2$d desbloqueadas · ochomiles %3$d / %4$d",
    "mtn_row_rank": "n.º %1$d del mundo · %2$s m",
    "mtn_row_milestone": "Hito %1$s m · %2$s m de altitud",
    "mtn_detail_progress": "Actual %1$s m · hito %2$s m",
}

IT = {
    "mtn_current": "Attuale %1$s m",
    "mtn_remaining": "mancano %1$s m",
    "mtn_summit_reached": "Raggiunta",
    "mtn_unlocked": "Sbloccata",
    "mtn_row_locked": "Non raggiunta",
    "mtn_jump_count": "%1$d sbloccate · Cronologia",
    "mtn_jump_none": "Cronologia · nessuna",
    "mtn_milestone_rank": "Traguardo %1$s m · n. %2$d al mondo",
    "mtn_milestone_range": "Traguardo %1$s m · %2$s",
    "mtn_metric_span": "Ampiezza del massiccio",
    "mtn_metric_snowline": "Limite delle nevi",
    "mtn_metric_range": "Catena",
    "mtn_note_contour": "Carta a curve di livello · equidistanza 200 m · da dati altimetrici pubblici",
    "mtn_note_render": "Schizzo del terreno · da dati altimetrici pubblici, non una foto",
    "mtn_entry_title": "Cronologia delle vette",
    "mtn_entry_none": "Nessuna",
    "mtn_entry_progress": "%1$d / %2$d sbloccate",
    "mtn_history_title": "Cronologia delle vette",
    "mtn_history_summary": "%1$d / %2$d sbloccate · ottomila %3$d / %4$d",
    "mtn_row_rank": "n. %1$d al mondo · %2$s m",
    "mtn_row_milestone": "Traguardo %1$s m · %2$s m di quota",
    "mtn_detail_progress": "Attuale %1$s m · traguardo %2$s m",
}

SV = {
    "mtn_current": "Nu %1$s m",
    "mtn_remaining": "%1$s m kvar",
    "mtn_summit_reached": "Uppnådd",
    "mtn_unlocked": "Upplåst",
    "mtn_row_locked": "Inte nådd",
    "mtn_jump_count": "%1$d upplåsta · Historik",
    "mtn_jump_none": "Historik · inga än",
    "mtn_milestone_rank": "Milstolpe %1$s m · nr %2$d i världen",
    "mtn_milestone_range": "Milstolpe %1$s m · %2$s",
    "mtn_metric_span": "Massivets bredd",
    "mtn_metric_snowline": "Snögräns",
    "mtn_metric_range": "Bergskedja",
    "mtn_note_contour": "Höjdkurvekarta · ekvidistans 200 m · ritad efter öppna höjddata",
    "mtn_note_render": "Terrängskiss · ritad efter öppna höjddata, inte ett foto",
    "mtn_entry_title": "Topphistorik",
    "mtn_entry_none": "Inga än",
    "mtn_entry_progress": "%1$d / %2$d upplåsta",
    "mtn_history_title": "Topphistorik",
    "mtn_history_summary": "%1$d / %2$d upplåsta · åttatusenmetare %3$d / %4$d",
    "mtn_row_rank": "nr %1$d i världen · %2$s m hög",
    "mtn_row_milestone": "Milstolpe %1$s m · %2$s m hög",
    "mtn_detail_progress": "Nu %1$s m · milstolpe %2$s m",
}

FILES = {
    "values": ZH,
    "values-en": EN,
    "values-zh-rTW": TW,
    "values-ja": JA,
    "values-ru": RU,
    "values-de": DE,
    "values-fr": FR,
    "values-es": ES,
    "values-it": IT,
    "values-sv": SV,
}

HEADER = {
    "values": "    <!-- 山峰里程碑（路径页卡片 / 我的 → 历史山峰） -->\n",
    "values-en": "    <!-- Peak milestones (path card / Profile → Peak history) -->\n",
    "values-zh-rTW": "    <!-- 山峰里程碑（路徑頁卡片 / 我的 → 歷史山峰） -->\n",
    "values-ja": "    <!-- 山峰マイルストーン（経路ページのカード / マイ → 山峰履歴） -->\n",
    "values-ru": "    <!-- Вехи вершин (карточка на маршруте / Профиль → История вершин) -->\n",
    "values-de": "    <!-- Gipfel-Meilensteine (Karte im Pfad / Profil → Gipfelverlauf) -->\n",
    "values-fr": "    <!-- Jalons de sommets (carte du parcours / Profil → Historique des sommets) -->\n",
    "values-es": "    <!-- Hitos de cumbres (tarjeta de la ruta / Perfil → Historial de cumbres) -->\n",
    "values-it": "    <!-- Traguardi delle vette (scheda del percorso / Profilo → Cronologia delle vette) -->\n",
    "values-sv": "    <!-- Toppmilstolpar (kortet i stigen / Profil → Topphistorik) -->\n",
}

OK = True
for folder, table in FILES.items():
    path = os.path.join(RES, folder, "strings.xml")
    src = open(path, encoding="utf-8").read()
    dup = [k for k in table if 'name="%s"' % k in src]
    if dup:
        print("SKIP(dup) %-14s %s" % (folder, dup))
        OK = False
        continue
    block = HEADER[folder]
    for k, v in table.items():
        block += '    <string name="%s">%s</string>\n' % (k, v)
    block += "\n"
    if "</resources>" not in src:
        print("NO END", path)
        OK = False
        continue
    src = src.replace("</resources>", block + "</resources>")
    open(path, "w", encoding="utf-8").write(src)
    print("OK %-14s %d 条" % (folder, len(table)))

print("done" if OK else "有跳过项")

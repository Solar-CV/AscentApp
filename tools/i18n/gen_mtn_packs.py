#!/usr/bin/env python3
"""生成山峰内容层（mtn.<id>.name / mtn.range.<中文> / mtn.snow.none）九语言覆盖表。

数据源：data/MountainMilestones.kt（直接解析，避免另存一份 88 座山抄错）。
文件名沿用既有约定 Pack<Lang>Mtn.kt，常量 PACK_<LANG>_MTN。

欧洲语言的名字**沿用数据里的英文名（endonym）**，只对几处当地通行写法做覆盖：
登山界通用的是马特洪峰、勃朗峰这类本名，硬翻成「Mont Cervin」反而认不出来。
日文、俄文、繁中必须转写，这三门语言给全量。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
DATA = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/MountainMilestones.kt")
OUT = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n")

CONSTS = {
    "HIMAL": "喜马拉雅", "KARAKORAM": "喀喇昆仑", "HENGDUAN": "横断山", "PAMIR": "帕米尔",
    "TIANSHAN": "天山", "KUNLUN": "昆仑山", "KUNLUN_W": "昆仑山西段", "QILIAN": "祁连山",
    "ALPS_R": "阿尔卑斯", "ROCKY": "落基山", "ANDES": "安第斯",
}


def parse():
    src = open(DATA, encoding="utf-8").read()
    body = src[src.index("val ALL: List<Mountain> = listOf("):src.index("/** 里程碑档位")]
    body = re.sub(r"//[^\n]*", "", body)
    rows = []
    for p in body.split('m("')[1:]:
        s = '"' + p
        head = s.split("MountainRegion.")[0]
        strs = [m.group(1) for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', head)]
        nums = re.search(r",\s*(\d+),\s*(\d+),\s*MountainRegion\.(\w+),", s)
        tail = s.split("MountainRegion." + nums.group(3) + ",", 1)[1]
        sp = re.search(r'\s*"([^"]*)",\s*"([^"]*)",\s*(?:"([^"]*)"|([A-Z_]+))', tail)
        rows.append({
            "id": strs[0], "zh": strs[1], "en": strs[2],
            "snow": sp.group(2),
            "range": sp.group(3) or CONSTS[sp.group(4)],
        })
    return rows


# ---------------------------------------------------------------- 山脉名
RANGE = {
    "en": {
        "贺兰山": "Helan Mountains", "秦岭": "Qin Ling", "富士火山": "Fuji Volcanic Belt",
        "雪山山脉": "Xueshan Range", "玉山山脉": "Yushan Range", "阿尔卑斯": "Alps",
        "克罗克山脉": "Crocker Range", "落基山": "Rocky Mountains", "喀斯喀特": "Cascades",
        "内华达山": "Sierra Nevada", "苏迪曼": "Sudirman Range",
        "埃尔斯沃思": "Ellsworth Mountains", "岷山": "Min Shan",
        "邛崃山": "Qionglai Mountains", "高加索": "Caucasus",
        "亚美尼亚高原": "Armenian Highlands", "东非裂谷": "East African Rift",
        "祁连山": "Qilian Mountains", "巴颜喀拉": "Bayan Har",
        "横断山": "Hengduan Mountains", "圣伊莱亚斯": "Saint Elias Mountains",
        "大雪山脉": "Daxue Mountains", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "Trans-Mexican Volcanic Belt", "阿尔金山": "Altun Mountains",
        "沙鲁里山": "Shaluli Mountains", "阿拉斯加山脉": "Alaska Range",
        "唐古拉": "Tanggula", "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha",
        "兴都库什": "Hindu Kush", "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakoram",
        "帕米尔": "Pamir", "天山": "Tian Shan", "昆仑山": "Kunlun Mountains",
        "昆仑山西段": "Western Kunlun", "安第斯": "Andes",
    },
    "ja": {
        "贺兰山": "賀蘭山脈", "秦岭": "秦嶺", "富士火山": "富士火山帯",
        "雪山山脉": "雪山山脈", "玉山山脉": "玉山山脈", "阿尔卑斯": "アルプス",
        "克罗克山脉": "クロッカー山脈", "落基山": "ロッキー山脈",
        "喀斯喀特": "カスケード山脈", "内华达山": "シエラネバダ山脈",
        "苏迪曼": "スディルマン山脈", "埃尔斯沃思": "エルズワース山脈", "岷山": "岷山",
        "邛崃山": "邛崍山脈", "高加索": "コーカサス",
        "亚美尼亚高原": "アルメニア高原", "东非裂谷": "東アフリカ大地溝帯",
        "祁连山": "祁連山脈", "巴颜喀拉": "巴顔喀拉山脈", "横断山": "横断山脈",
        "圣伊莱亚斯": "セントエライアス山脈", "大雪山脉": "大雪山脈",
        "厄尔布尔士": "アルボルズ山脈", "墨西哥火山带": "メキシコ火山帯",
        "阿尔金山": "アルトゥン山脈", "沙鲁里山": "沙魯里山脈",
        "阿拉斯加山脉": "アラスカ山脈", "唐古拉": "唐古拉山脈",
        "冈底斯": "ガンディセ山脈", "念青唐古拉": "ニェンチェンタンラ山脈",
        "兴都库什": "ヒンドゥークシュ山脈", "喜马拉雅": "ヒマラヤ山脈",
        "喀喇昆仑": "カラコルム山脈", "帕米尔": "パミール高原", "天山": "天山山脈",
        "昆仑山": "崑崙山脈", "昆仑山西段": "崑崙山脈（西段）", "安第斯": "アンデス山脈",
    },
    "ru": {
        "贺兰山": "Хэланьшань", "秦岭": "Циньлин", "富士火山": "вулкан Фудзи",
        "雪山山脉": "хребет Сюэшань", "玉山山脉": "хребет Юйшань", "阿尔卑斯": "Альпы",
        "克罗克山脉": "хребет Крокер", "落基山": "Скалистые горы",
        "喀斯喀特": "Каскадные горы", "内华达山": "Сьерра-Невада",
        "苏迪曼": "хребет Судирман", "埃尔斯沃思": "горы Элсуорт", "岷山": "Миньшань",
        "邛崃山": "Цюнлайшань", "高加索": "Кавказ",
        "亚美尼亚高原": "Армянское нагорье", "东非裂谷": "Восточно-Африканский рифт",
        "祁连山": "Циляньшань", "巴颜喀拉": "Баян-Хара", "横断山": "Хэндуаньшань",
        "圣伊莱亚斯": "горы Сент-Элайас", "大雪山脉": "хребет Дасюэ",
        "厄尔布尔士": "Эльбурс", "墨西哥火山带": "Трансмексиканский вулканический пояс",
        "阿尔金山": "Алтынтаг", "沙鲁里山": "Шалулишань",
        "阿拉斯加山脉": "Аляскинский хребет", "唐古拉": "Танггула",
        "冈底斯": "Гангдисе", "念青唐古拉": "Ньэнчентанглха", "兴都库什": "Гиндукуш",
        "喜马拉雅": "Гималаи", "喀喇昆仑": "Каракорум", "帕米尔": "Памир",
        "天山": "Тянь-Шань", "昆仑山": "Куньлунь", "昆仑山西段": "Западный Куньлунь",
        "安第斯": "Анды",
    },
    "de": {
        "贺兰山": "Helan-Gebirge", "秦岭": "Qinling", "富士火山": "Fuji-Vulkangürtel",
        "雪山山脉": "Xueshan-Kette", "玉山山脉": "Yushan-Kette", "阿尔卑斯": "Alpen",
        "克罗克山脉": "Crocker Range", "落基山": "Rocky Mountains",
        "喀斯喀特": "Kaskadenkette", "内华达山": "Sierra Nevada",
        "苏迪曼": "Sudirman-Kette", "埃尔斯沃思": "Ellsworth Mountains", "岷山": "Min Shan",
        "邛崃山": "Qionglai-Berge", "高加索": "Kaukasus",
        "亚美尼亚高原": "Armenisches Hochland", "东非裂谷": "Ostafrikanischer Graben",
        "祁连山": "Qilian-Berge", "巴颜喀拉": "Bayan Har", "横断山": "Hengduan-Berge",
        "圣伊莱亚斯": "St.-Elias-Kette", "大雪山脉": "Daxue-Berge", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "Transmexikanischer Vulkangürtel", "阿尔金山": "Altun-Berge",
        "沙鲁里山": "Shaluli-Berge", "阿拉斯加山脉": "Alaska Range", "唐古拉": "Tanggula",
        "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha", "兴都库什": "Hindukusch",
        "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakorum", "帕米尔": "Pamir",
        "天山": "Tian Shan", "昆仑山": "Kunlun", "昆仑山西段": "Westlicher Kunlun",
        "安第斯": "Anden",
    },
    "fr": {
        "贺兰山": "monts Helan", "秦岭": "Qinling", "富士火山": "chaîne volcanique du Fuji",
        "雪山山脉": "monts Xueshan", "玉山山脉": "monts Yushan", "阿尔卑斯": "Alpes",
        "克罗克山脉": "Crocker Range", "落基山": "montagnes Rocheuses",
        "喀斯喀特": "chaîne des Cascades", "内华达山": "Sierra Nevada",
        "苏迪曼": "monts Sudirman", "埃尔斯沃思": "monts Ellsworth", "岷山": "Min Shan",
        "邛崃山": "monts Qionglai", "高加索": "Caucase",
        "亚美尼亚高原": "haut plateau arménien", "东非裂谷": "rift est-africain",
        "祁连山": "monts Qilian", "巴颜喀拉": "Bayan Har", "横断山": "monts Hengduan",
        "圣伊莱亚斯": "monts Saint-Élie", "大雪山脉": "monts Daxue", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "ceinture volcanique transmexicaine", "阿尔金山": "monts Altun",
        "沙鲁里山": "monts Shaluli", "阿拉斯加山脉": "chaîne d'Alaska", "唐古拉": "Tanggula",
        "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha", "兴都库什": "Hindu Kouch",
        "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakoram", "帕米尔": "Pamir",
        "天山": "Tian Shan", "昆仑山": "Kunlun", "昆仑山西段": "Kunlun occidental",
        "安第斯": "Andes",
    },
    "es": {
        "贺兰山": "montes Helan", "秦岭": "Qinling", "富士火山": "cinturón volcánico del Fuji",
        "雪山山脉": "sierra de Xueshan", "玉山山脉": "sierra de Yushan", "阿尔卑斯": "Alpes",
        "克罗克山脉": "Crocker Range", "落基山": "montañas Rocosas",
        "喀斯喀特": "Cascadas", "内华达山": "Sierra Nevada",
        "苏迪曼": "montes Sudirman", "埃尔斯沃思": "montes Ellsworth", "岷山": "Min Shan",
        "邛崃山": "montes Qionglai", "高加索": "Cáucaso",
        "亚美尼亚高原": "altiplano armenio", "东非裂谷": "rift de África Oriental",
        "祁连山": "montes Qilian", "巴颜喀拉": "Bayan Har", "横断山": "montes Hengduan",
        "圣伊莱亚斯": "montes San Elías", "大雪山脉": "montes Daxue", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "eje volcánico transmexicano", "阿尔金山": "montes Altun",
        "沙鲁里山": "montes Shaluli", "阿拉斯加山脉": "cordillera de Alaska",
        "唐古拉": "Tanggula", "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha",
        "兴都库什": "Hindu Kush", "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakórum",
        "帕米尔": "Pamir", "天山": "Tian Shan", "昆仑山": "Kunlun",
        "昆仑山西段": "Kunlun occidental", "安第斯": "Andes",
    },
    "it": {
        "贺兰山": "monti Helan", "秦岭": "Qinling", "富士火山": "cintura vulcanica del Fuji",
        "雪山山脉": "catena Xueshan", "玉山山脉": "catena Yushan", "阿尔卑斯": "Alpi",
        "克罗克山脉": "Crocker Range", "落基山": "Montagne Rocciose",
        "喀斯喀特": "Catena delle Cascate", "内华达山": "Sierra Nevada",
        "苏迪曼": "monti Sudirman", "埃尔斯沃思": "monti Ellsworth", "岷山": "Min Shan",
        "邛崃山": "monti Qionglai", "高加索": "Caucaso",
        "亚美尼亚高原": "altopiano armeno", "东非裂谷": "Rift dell'Africa orientale",
        "祁连山": "monti Qilian", "巴颜喀拉": "Bayan Har", "横断山": "monti Hengduan",
        "圣伊莱亚斯": "monti Sant'Elia", "大雪山脉": "monti Daxue", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "cintura vulcanica transmessicana", "阿尔金山": "monti Altun",
        "沙鲁里山": "monti Shaluli", "阿拉斯加山脉": "catena dell'Alaska",
        "唐古拉": "Tanggula", "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha",
        "兴都库什": "Hindu Kush", "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakorum",
        "帕米尔": "Pamir", "天山": "Tian Shan", "昆仑山": "Kunlun",
        "昆仑山西段": "Kunlun occidentale", "安第斯": "Ande",
    },
    "sv": {
        "贺兰山": "Helanbergen", "秦岭": "Qinling", "富士火山": "Fujis vulkanbälte",
        "雪山山脉": "Xueshanbergen", "玉山山脉": "Yushanbergen", "阿尔卑斯": "Alperna",
        "克罗克山脉": "Crocker Range", "落基山": "Klippiga bergen",
        "喀斯喀特": "Kaskadbergen", "内华达山": "Sierra Nevada",
        "苏迪曼": "Sudirmanbergen", "埃尔斯沃思": "Ellsworthbergen", "岷山": "Min Shan",
        "邛崃山": "Qionglaibergen", "高加索": "Kaukasus",
        "亚美尼亚高原": "Armeniska höglandet", "东非裂谷": "Östafrikanska riftsystemet",
        "祁连山": "Qilianbergen", "巴颜喀拉": "Bayan Har", "横断山": "Hengduanbergen",
        "圣伊莱亚斯": "Saint Elias-bergen", "大雪山脉": "Daxuebergen", "厄尔布尔士": "Alborz",
        "墨西哥火山带": "Transmexikanska vulkanbältet", "阿尔金山": "Altunbergen",
        "沙鲁里山": "Shalulibergen", "阿拉斯加山脉": "Alaskaranden", "唐古拉": "Tanggula",
        "冈底斯": "Gangdise", "念青唐古拉": "Nyainqentanglha", "兴都库什": "Hindukush",
        "喜马拉雅": "Himalaya", "喀喇昆仑": "Karakoram", "帕米尔": "Pamir",
        "天山": "Tian Shan", "昆仑山": "Kunlun", "昆仑山西段": "Västra Kunlun",
        "安第斯": "Anderna",
    },
    "zh-TW": {},   # OpenCC 生成
}

# ---------------------------------------------------------------- 山名转写
# 只写与数据里的英文名不同的条目；其余语言（de/fr/es/it/sv/en）默认沿用 endonym。
NAME = {
    "ja": {
        "helan": "賀蘭山脈", "taibai": "太白山", "fuji": "富士山", "xueshan": "雪山",
        "yushan": "玉山", "eiger": "アイガー", "kinabalu": "キナバル山",
        "jungfrau": "ユングフラウ", "teton": "グランド・ティトン", "rainier": "レーニア山",
        "whitney": "ホイットニー山", "matterhorn": "マッターホルン",
        "rosa": "モンテ・ローザ", "blanc": "モンブラン", "jaya": "プンチャック・ジャヤ",
        "vinson": "ヴィンソン・マシフ", "jiuding": "九頂山",
        "siguniang_da": "四姑娘山（大峰）", "kazbek": "カズベク山", "ararat": "アララト山",
        "kenya": "ケニア山", "gangshika": "崗什卡雪峰",
        "siguniang_er": "四姑娘山（二峰）", "siguniang_san": "四姑娘山（三峰）",
        "nianbao": "年保玉則", "haba": "ハバ雪山", "baima": "白馬雪山",
        "elias": "セント・エライアス山", "xuebaoding": "雪宝頂", "nama": "那瑪峰",
        "yulong": "玉龍雪山", "damavand": "ダマーヴァンド山", "orizaba": "オリサバ山",
        "elbrus": "エルブルス山", "altun": "アルトゥン山脈", "tuanjie": "団結峰",
        "kilimanjaro": "キリマンジャロ", "yangmaiyong": "ヤンマイヨン", "logan": "ローガン山",
        "xiannairi": "シェンナイリ", "tianhaizi": "田海子山", "queer": "雀児山",
        "yuzhu": "玉珠峰", "denali": "デナリ", "genie": "格聶神山",
        "yaomei": "四姑娘山（幺妹峰）", "chimborazo": "チンボラソ",
        "amne": "アムネ・マチェン", "illimani": "イリマニ", "sajama": "サハマ山",
        "geladaindong": "格拉丹冬", "kailash": "カイラス山", "kawagarbo": "カワガルボ",
        "huascaran": "ワスカラン", "ojos": "オホス・デル・サラード",
        "aconcagua": "アコンカグア", "muztagh_ata": "ムスターグ・アタ",
        "khan": "ハン・テングリ", "qungmogangri": "チュンモガンリ",
        "lenin": "レーニン峰", "nyainqen": "ニェンチェンタンラ山",
        "noijin": "ノイジン・カンサン", "noshaq": "ノシャク峰",
        "jarabai": "ギャラ・ペリ", "chomolhari": "チョモラリ", "tomur": "トムール峰",
        "somoniy": "イスモイル・ソモニ峰", "kulha": "クラ・カンリ",
        "muztagh": "ムスターグ峰", "gongga": "ミニヤコンカ", "kongur": "コングール峰",
        "namunani": "ナイモナンニ峰", "tirich": "ティリチ・ミール",
        "namcha": "ナムチャバルワ", "shishapangma": "シシャパンマ",
        "gasherbrum2": "ガッシャーブルムII峰", "broad": "ブロード・ピーク",
        "gasherbrum1": "ガッシャーブルムI峰", "annapurna": "アンナプルナI峰",
        "nangaparbat": "ナンガ・パルバット", "manaslu": "マナスル",
        "dhaulagiri": "ダウラギリ", "chooyu": "チョ・オユー", "makalu": "マカルー",
        "lhotse": "ローツェ", "kangchenjunga": "カンチェンジュンガ", "k2": "K2",
        "everest": "エベレスト",
    },
    "ru": {
        "helan": "Хэланьшань", "taibai": "Тайбайшань", "fuji": "Фудзи",
        "xueshan": "Сюэшань", "yushan": "Юйшань", "eiger": "Айгер",
        "kinabalu": "Кинабалу", "jungfrau": "Юнгфрау", "teton": "Гранд-Титон",
        "rainier": "Рейнир", "whitney": "Уитни", "matterhorn": "Маттерхорн",
        "rosa": "Монте-Роза", "blanc": "Монблан", "jaya": "Пунчак-Джая",
        "vinson": "Массив Винсон", "jiuding": "Цзюдиншань",
        "siguniang_da": "Сигуняншань (Дафэн)", "kazbek": "Казбек", "ararat": "Арарат",
        "kenya": "Кения", "gangshika": "Ганшика",
        "siguniang_er": "Сигуняншань (Эрфэн)",
        "siguniang_san": "Сигуняншань (Саньфэн)", "nianbao": "Няньбаоюйцзэ",
        "haba": "Хаба", "baima": "Байма", "elias": "Сент-Элайас",
        "xuebaoding": "Сюэбаодин", "nama": "Нама", "yulong": "Юйлунсюэшань",
        "damavand": "Дамаванд", "orizaba": "Орисаба", "elbrus": "Эльбрус",
        "altun": "Алтынтаг", "tuanjie": "Тяньцзе", "kilimanjaro": "Килиманджаро",
        "yangmaiyong": "Янмайюн", "logan": "Логан", "xiannairi": "Сяньнайжи",
        "tianhaizi": "Тяньхайцзы", "queer": "Цюээршань", "yuzhu": "Юйчжу",
        "denali": "Денали", "genie": "Генье", "yaomei": "Сигуняншань (Яомэй)",
        "chimborazo": "Чимборасо", "amne": "Амне-Мачин", "illimani": "Ильимани",
        "sajama": "Сахама", "geladaindong": "Геладандун", "kailash": "Кайлас",
        "kawagarbo": "Кавагебо", "huascaran": "Уаскаран",
        "ojos": "Охос-дель-Саладо", "aconcagua": "Аконкагуа",
        "muztagh_ata": "Музтаг-Ата", "khan": "Хан-Тенгри",
        "qungmogangri": "Цюнмоганри", "lenin": "Пик Ленина",
        "nyainqen": "Ньэнчентанглха", "noijin": "Нойджин-Кангсанг",
        "noshaq": "Ношак", "jarabai": "Гьяла-Пери", "chomolhari": "Чомолхари",
        "tomur": "Томур", "somoniy": "Пик Исмоила Сомони", "kulha": "Кула-Кангри",
        "muztagh": "Музтаг", "gongga": "Гунгашань", "kongur": "Конгур",
        "namunani": "Наймонанью", "tirich": "Тирич-Мир", "namcha": "Намча-Барва",
        "shishapangma": "Шишабангма", "gasherbrum2": "Гашербрум II",
        "broad": "Броуд-пик", "gasherbrum1": "Гашербрум I", "annapurna": "Аннапурна I",
        "nangaparbat": "Нангапарбат", "manaslu": "Манаслу",
        "dhaulagiri": "Дхаулагири", "chooyu": "Чо-Ойю", "makalu": "Макалу",
        "lhotse": "Лхоцзе", "kangchenjunga": "Канченджанга", "k2": "К2 (Чогори)",
        "everest": "Эверест",
    },
    # 只有这几座在意大利/法国有通行的本地写法，其余一律用国际通用本名
    "it": {"matterhorn": "Cervino", "blanc": "Monte Bianco", "elias": "Monte Sant'Elia"},
    "fr": {"rosa": "Mont Rose", "elias": "Mont Saint-Élie"},
    "de": {"blanc": "Montblanc", "elias": "Mount St. Elias"},
}

SNOW_NONE = {
    "en": "No permanent snow", "zh-TW": "無常年積雪", "ja": "通年雪なし",
    "ru": "Нет постоянного снега", "de": "Kein Dauerschnee",
    "fr": "Pas de neige permanente", "es": "Sin nieve permanente",
    "it": "Nessuna neve perenne", "sv": "Ingen permanent snö",
}

FILES = {
    "en": ("PackEnMtn.kt", "PACK_EN_MTN", "English"),
    "zh-TW": ("PackZhTwMtn.kt", "PACK_ZH_TW_MTN", "繁體中文（台灣）"),
    "ja": ("PackJaMtn.kt", "PACK_JA_MTN", "日本語"),
    "ru": ("PackRuMtn.kt", "PACK_RU_MTN", "Русский"),
    "de": ("PackDeMtn.kt", "PACK_DE_MTN", "Deutsch"),
    "es": ("PackEsMtn.kt", "PACK_ES_MTN", "Español"),
    "fr": ("PackFrMtn.kt", "PACK_FR_MTN", "Français"),
    "sv": ("PackSvMtn.kt", "PACK_SV_MTN", "Svenska"),
    "it": ("PackItMtn.kt", "PACK_IT_MTN", "Italiano"),
}


def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')


def main():
    rows = parse()
    assert len(rows) == 88, len(rows)
    bad = [r["id"] for r in rows if r["snow"][0].isdigit() is False]
    assert bad == ["kinabalu"], bad
    ranges = sorted({r["range"] for r in rows})
    assert len(ranges) == 38, len(ranges)

    # 繁中：OpenCC s2twp + 术语校正（与 tools/i18n/tw.py 同一套规则）
    import opencc
    cc = opencc.OpenCC("s2twp")
    tw_range = {r: cc.convert(r) for r in ranges}

    for lang, (fname, const, label) in FILES.items():
        rng_tab = RANGE.get(lang) or {}
        if lang == "zh-TW":
            rng_tab = tw_range
        if lang != "en":
            missing = [r for r in ranges if r not in rng_tab]
            assert not missing, (lang, missing)
        lines = []
        for r in rows:
            nm = r["en"] if lang in ("en", "de", "fr", "es", "it", "sv") else r["zh"]
            if lang == "en":
                nm = r["en"]
            elif lang == "zh-TW":
                nm = cc.convert(r["zh"])
            nm = NAME.get(lang, {}).get(r["id"], nm)
            lines.append('    "mtn.%s.name" to "%s",' % (r["id"], esc(nm)))
        lines.append("")
        for r in ranges:
            lines.append('    "mtn.range.%s" to "%s",' % (r, esc(rng_tab[r])))
        lines.append("")
        lines.append('    "mtn.snow.none" to "%s",' % esc(SNOW_NONE[lang]))

        body = "\n".join(lines)
        src = (
            "package com.ascent.climb.data.i18n\n\n"
            "/** %s。山峰名与山脉名，缺项回退英文。 */\n"
            "internal val %s: Map<String, String> = mapOf(\n\n%s\n)\n"
        ) % (label, const, body)
        path = os.path.join(OUT, fname)
        open(path, "w", encoding="utf-8").write(src)
        print("%-16s %s  %d keys" % (lang, fname, 88 + len(ranges) + 1))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""生成徒步路线内容包：英文（必做）与繁體中文（OpenCC）。

只读 `data/HikingRoutes.kt`，不另存一份路线表——中文原文永远以 Kotlin 文件为准。
英文名称直接取数据里的 nameEn；地区、简介与长介绍在这里维护
（`AREA_EN` / `BRIEF_EN` / `INTRO_EN`）。中文长介绍从 `data/HikeIntro.kt` 反解析，不另存副本。

用法：
    python3 tools/i18n/gen_hike_packs.py          # 写两个 Kotlin 文件
    python3 tools/i18n/gen_hike_packs.py --check  # 只核对 id 是否齐全
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/HikingRoutes.kt")
OUT_EN = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n/PackEnHike.kt")
OUT_TW = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n/PackZhTwHike.kt")

ROW = re.compile(
    r'r\("(?P<id>[^"]+)",\s*"(?P<zh>[^"]*)",\s*"(?P<en>[^"]*)",\s*"(?P<area>[^"]*)",'
    r'\s*HikeRegion\.\w+,\s*\d+,\s*\d+,\s*\d+,\s*\d+,\s*listOf\([^)]*\),\s*"(?P<brief>[^"]*)"'
    # 有图的路线在 brief 之后还有 R.drawable.xxx，无图的直接收尾
    r'(?:,\s*R\.drawable\.[A-Za-z0-9_]+)?\)',
    re.S,
)

AREA_EN = {
    "longji": "Hong Kong, China",
    "xiangshan": "Beijing, China",
    "wutong": "Guangdong, China",
    "huihang": "Anhui / Zhejiang, China",
    "laugavegur": "Iceland",
    "trolltunga": "Norway",
    "camino": "Spain",
    "taishan": "Shandong, China",
    "overland": "Australia",
    "huangshan": "Anhui, China",
    "wugong": "Jiangxi, China",
    "hallasan": "South Korea",
    "tongariro": "New Zealand",
    "huashan": "Shaanxi, China",
    "gr20": "France",
    "changbaishan": "Jilin, China",
    "rim2rim": "USA",
    "paine": "Chile",
    "hutiaoxia": "Yunnan, China",
    "tmb": "France · Italy · Switzerland",
    "halfdome": "USA",
    "av1": "Italy",
    "wutai": "Shanxi, China",
    "emeishan": "Sichuan, China",
    "poonhill": "Nepal",
    "yubeng": "Yunnan, China",
    "changping": "Sichuan, China",
    "taibai": "Shaanxi, China",
    "fuji": "Japan",
    "yushan": "Taiwan, China",
    "abc": "Nepal",
    "toubkal": "Morocco",
    "inca": "Peru",
    "jmt": "USA",
    "yading": "Sichuan, China",
    "gongga": "Sichuan, China",
    "santacruz": "Peru",
    "chooyu": "Tibet, China",
    "shishapangma": "Tibet, China",
    "zhuoga": "Tibet, China",
    "ebc": "Nepal",
    "act": "Nepal",
    "threepasses": "Nepal",
    "kailash": "Tibet, China",
}

INTRO_EN = {
    "longji": "The ridgeline at the southeastern corner of Hong Kong Island: start at To Tei Wan, run southwest over Wan Cham Teng, drop to Tai Long Wan. Sea on both sides — Sai Kung and Tai Tam Bay north, the South China Sea south — so the point is not altitude but walking between two waters. Almost no shade, and summer stone gets hot enough to burn; carry double the water and sunscreen you think you need. Visibility is best in autumn; finish with a swim at Tai Long Wan or walk back to To Tei Wan for the bus.",
    "xiangshan": "The closest hill to central Beijing, and the easiest place to finish a first half-day with a full pack. The main summit is Xianglufeng, nicknamed Gui Jian Chou. Two usual lines from the east gate: the gradual way past Yu Huang Ding, or the direct staircase past Shuangqing Villa — all built steps, but the continuous climb is hard on knees. In red-leaf season there are more people than trees, so go on a weekday morning; winter steps ice over. A cable car runs down from the summit if your legs are done.",
    "wutong": "Shenzhen's highest point. The Haohan slope climbs continuously from Wutongshan village and is the standard place in southern China to train ascent. Stone steps and dirt alternate, and the last stretch is almost all steps — your heart gives out before your legs do. From the top you see Yantian port, Shenzhen Bay and Hong Kong across the water. Hot and humid in summer, so locals start at four or five in the morning; watch for closures in typhoon season.",
    "huihang": "The old merchant road out of the mountains: enter at Fuling in Jixi, Anhui, exit at Zhechuan in Lin'an, Zhejiang, in a single day. Mostly stone slabs. Jiangnan First Pass is the most distinctive section — steps cut along the cliff with rock on both sides. Lantian'ao halfway is the usual camping spot if you split it into two days. Stone is slippery after rain, especially on the descent; autumn is best, spring is wet.",
    "laugavegur": "Iceland's best-known highland traverse, Landmannalaugar to Thorsmork, four days. The landscape barely repeats: coloured rhyolite, black obsidian desert, geothermal steam, glacial river valleys. Book huts six months ahead; the highest camp is windy and cold, so peg the fly properly. Several meltwater rivers need fording and the water is numbing — bring wading shoes or go barefoot. Four seasons in a day, so windproof and waterproof matter more than warm layers.",
    "trolltunga": "Start at the Skjeggedal car park: a built staircase first, then open rock slope. Twenty-eight kilometres round trip with the climb packed into the first half; most people take ten to twelve hours and must be back before dark. The rock shelf has no railing — if you are afraid of heights your legs will tell you at the edge. Best from June to September once the snow is gone; winter and early spring need a guide and crampons.",
    "camino": "Start at Saint-Jean-Pied-de-Port on the French border; day one crosses the Pyrenees to Roncesvalles, then it runs west to Santiago. Twenty-odd kilometres a day, sleeping in albergue bunks, first come first served — start early in high season. You need a pilgrim passport stamped along the way, and the certificate requires the last hundred kilometres on foot. The test is not fitness but repetition: the same routine for thirty days. Spring and autumn are kindest; inland Spain hits forty degrees in July and August.",
    "taishan": "From the Red Gate through Zhongtianmen and the Eighteen Bends to the Jade Emperor Summit — seven thousand-plus steps, all stone. The Eighteen Bends is the last and steepest part: narrow steps, big grade, and it jams solid on public holidays. Climbing at night for sunrise is the standard move, and the summit is windy and cold, so bring real layers. If you are short on legs a cable car runs from Zhongtianmen, but that skips the point of this route.",
    "overland": "Tasmania's six-day traverse from Cradle Mountain to Lake St Clair, the only long walk in Australia with a quota. Book ahead; camps are fixed timber platforms and you walk the itinerary you booked, with no changes on the fly. Weather turns fast, four seasons in a day; the first half is boardwalk, the second mud and rock. Allow a day for the Cradle Mountain side trip — it is the high point of the whole walk.",
    "huangshan": "The West Sea Grand Canyon is the steepest part of Huangshan: two loops of continuous descending steps, and climbing back out costs your legs. The usual line is up from Yungu Temple on the back side and down at Ciguang Pavilion on the front, with the canyon in the middle day. Book mountain accommodation ahead; when beds are full you do it in one day. A sea of clouds is most likely after rain, but the steps are wet — use poles on the descent.",
    "wugong": "Start from Longshan or Shenzi village; the best part is the rolling alpine meadow along the ridge all the way to the Golden Summit. Sunrise and sunset from the top, camping or a guesthouse — book ahead in season. No water on the ridge, so carry both days' worth from the start. September to November turns the grass gold; it is the prettiest and most crowded time.",
    "hallasan": "Korea's highest. The standard day traverse goes up Seongpanak and down Gwaneumsa, and both trails need advance booking. The grade is gentle but the distance is real; allow a full day. Baengnokdam, the crater lake at the top, only shows when the cloud clears. Snow in winter needs crampons, and wind in spring and autumn makes the summit feel ten degrees colder than the trailhead.",
    "tongariro": "New Zealand's best-known day walk, Mangatepopo to Ketetahi — a one-way route, so book the shuttle first. Emerald Lakes, Red Crater and Blue Lake are the three markers, and the film's Mount Doom stands right beside it. Fully exposed to wind with fast-changing weather; ice in winter means crampons and preferably a guide. Parking and shuttles both need booking, and in peak season slots vanish.",
    "huashan": "Start at Yuquan Temple and walk the Huashan gorge, or take the cable car straight to North Peak; linking all five peaks takes a full day. The plank walk and the Sparrowhawk Turn are optional extras with separate tickets and queues — skip them if heights bother you. Steps are steep and narrow and the descent hammers your knees, so poles and a knee brace help. Many climb at night for sunrise, which makes a headtorch mandatory.",
    "gr20": "Corsica north to south, Calenzana to Conca, fifteen days — widely called the hardest long trail in Europe. The difficulty is not altitude but surface: endless scree, bare rock, hands-on sections, and almost impassable in rain. Book the refuges ahead; many stages have no water, so confirm the day's source before starting. June to September works; early season still holds snow up high and needs crampons.",
    "changbaishan": "From the west gate, take the shuttle to the foot of the steps, then climb some fourteen hundred steps to the rim of Tianchi. Seeing the lake is pure luck — the cloud has to break, and plenty of people get nothing but white. Windy and cold up there, so bring a jacket even in summer. West and north slopes have different gates and see different shores, so do not buy the wrong ticket.",
    "rim2rim": "Down the North Rim to the valley floor, sleep, then up the Bright Angel Trail to the South Rim — one day down, one day up. Descending hurts more than climbing; hold back in the first third or you will not walk on day two. The inner canyon passes forty degrees in summer, and May plus September–October are the window. Refill at Phantom Ranch and confirm water availability before you start.",
    "paine": "The Patagonian classic: from Puerto Natales, boat to Pudeto, then into the park. The W links three highlights — Grey Glacier, the French Valley and the Base of the Towers; fit parties do it in five days, four is possible. Camps must be booked ahead, and CONAF sites are separate from private ones, so do not book only half. Wind is the real opponent: a tent that holds in force ten and a windproof stove setup.",
    "halfdome": "From Happy Isles in Yosemite Valley, past Vernal and Nevada Falls, then up to Half Dome. The last hundred-odd metres is a cable climb — gloves needed, and never in rain or thunderstorms. The cables require a lottery permit; success rates are low in season, and without one you stop at Sub Dome. Twenty-two kilometres and fifteen hundred metres of gain in a day, so go prepared.",
    "hutiaoxia": "Start at Tiger Leaping Gorge town (Qiaotou); the High Trail runs along the Haba Snow Mountain side with the Jinsha River right below. Halfway and Tea Horse guesthouses are the landmarks — stay a night and watch the sun set on Jade Dragon Snow Mountain. The final Twenty-Eight Bends is a continuous steep drop to the road, where you pick up a bus back to Qiaotou. The rockfall section is dangerous after rain, so do not linger under the cliff.",
    "tmb": "From Les Houches, a full circuit of Mont Blanc through France, Italy and Switzerland and back to the start. A hut every night, so no tent or food to carry — but huts must be booked, and July and August are tight. Altitude is modest; the problem is daily gain, ten consecutive days over a thousand metres, and knees and ankles go first. July and August are most stable, June holds snow up high, September is quieter and cooler.",
    "av1": "The Dolomites north–south line, Lago di Braies to Belluno, ten days. Limestone towers and meadows alternate, with big days but no technical climbing — a long route you finish if you can keep walking. Sleep in rifugi and book ahead; popular stages fill six months out. Late June to September works, and afternoon thunderstorms are common in July and August, so start early.",
    "wutai": "Start at Taihuai and go up East, North, Central, West and South Terrace in turn, each with a temple on top. North Terrace, Yedou Feng, is the highest point in northern China — windy and cold, close to zero even in summer. Temples along the way take in lodgers, a bed and vegetarian meals, so no tent needed, but call ahead. May to September works; in winter the terraces close and the full circuit is basically impossible. The usual plan is three days clockwise, saving the distant South Terrace for last.",
    "emeishan": "From Baoguo Temple via Hongchunping, Xianfeng Temple and Xixiang Pool to the Golden Summit — two hard days of continuous climbing. Monkeys midway will grab food, so do not hang snacks outside your pack and do not meet their eyes. Sunrise and the sea of clouds from the top; book mountain lodging ahead, or take the bus to Leidongping and walk only the last stretch. Busiest around National Day, so pick a weekday for quiet.",
    "poonhill": "The beginner's viewpoint trek of the Annapurna region, usually from Nayapul or Ulleri, four days round trip. The point is Poon Hill above Ghorepani: at sunrise Dhaulagiri and Annapurna line up across the sky. You need both TIMS and ACAP permits, arranged in Pokhara before heading in. October and November give the clearest views; rhododendrons bloom in March and April but cloud builds by afternoon.",
    "yubeng": "Enter from Xidang hot spring — walk in, or take a villager's car — and leave through the Ninong gorge. The village is guesthouses, simple but with hot water and meals; Ice Lake and the Sacred Waterfall are two day-return side trips. The Ice Lake line has real gain and rough ground, the waterfall line is shorter but wet and slippery. October–November and April–May are best; landslide season makes the approach uncertain, so ask about road conditions first.",
    "changping": "Enter from Siguniang town, start at Lama Temple and follow Changping valley in; Muluozi is the camp at the head of the valley. Boardwalk first, then mud and scree — the further in, the quieter and the wilder. You can cross over to Bipeng valley, but that needs a guide and a permit and is not ordinary trekking. Altitude climbs fast, so do not rush the first day; give your body time.",
    "taibai": "Up from Xiabansi or Houzhenzi, with Daye Hai as the high camp, then a light summit push to Baxiantai the next day. Weather turns in minutes up there and it can snow in summer, so insulation and rain gear are not optional. The reserve requires registration, so confirm the entrance is open before you travel. Do not confuse it with the Aotai traverse — that is a different, far more dangerous line with a bad accident record.",
    "fuji": "Start at the Fifth Station; the Yoshida route has the most huts and the best support of the four main lines. Open only July to September, huts must be booked, and to see sunrise from the top you start in the middle of the night. Altitude gain is fast and headaches are common, so slow down and turn around if it gets worse. Bullet climbing — straight up without sleep — is discouraged; it is the main source of summit incidents.",
    "yushan": "Start at the Tatajia trailhead; Paiyun Lodge is the halfway bed, then a light dawn push to the main peak. The peak needs a park permit and the lodge is allocated by lottery, so leave slack in your dates. The last stretch is scree and chains — watch your balance in wind. Snow in winter needs crampons, and spring and autumn dawns hover near zero, so do not skimp on layers.",
    "abc": "In from Nayapul or the Pokhara side, via Chhomrong and Deurali to Annapurna Base Camp. Altitude is gradual, but ten days to over four thousand metres still needs rest days — stop an extra day if you feel off. Early morning at base camp shows Machhapuchhre and the Annapurna south face; once the sun is up the valley fills with cloud. October–November and March–April are the windows, and high lodges close in winter.",
    "toubkal": "From Imlil via Sidi Chamharouch to Refuge du Toubkal, summit the next day. The last part is scree; winter and early spring need crampons and an axe, and local rules require a guide. June to September is easiest, but ground heat is fierce in summer so start early. This is the highest point in North Africa and for many people their first four-thousander.",
    "inca": "From KM82, four days to Machu Picchu; day two crosses Dead Woman's Pass, the highest point of the route. There is a daily quota including porters, so book months ahead in season. On the last day you leave Winay Wayna in the dark to reach the Sun Gate at sunrise. Closed in the rainy season from December to March, and carry waterproofing in any month.",
    "jmt": "From Happy Isles in Yosemite all the way to Mount Whitney — the benchmark long trail of North America. You carry tent and food, with resupply only at Red's Meadow, VVR and Muir Trail Ranch. A permit is required and the Whitney exit quota is separate; without it you turn around at the forest boundary. July to September works, and June still has snow up high that needs crampons and an axe.",
    "yading": "From Luorong pasture to Milk Lake and Five Colour Lake — a short day at real altitude. The whole route is above four thousand metres, so walk slowly and stop if your head hurts. Horses only go halfway and the rest is on foot; park entry and shuttle are separate tickets. October has the best autumn colour, while July and August are wet and the trail turns to mud.",
    "gongga": "In from Shangmuju or Yulongxi; the Zimei pass is the most direct view of Gongga's main summit. The pass is close to four thousand seven hundred and altitude hits hard, so sleep lower the night before. Access roads are rough and monsoon rain washes them out, so check they are open. October and November have the most clear days, and the summit lights up at sunrise.",
    "santacruz": "From Vaqueria over Punta Union and back in four days — the classic circuit of the Cordillera Blanca. The pass is close to four thousand eight hundred, so start early on crossing day because cloud builds after noon. Arrieros with mules share the trail; the surface is dirt and scree and needs no technical gear. May to September is the dry season; January to March is wet and some sections close.",
    "chooyu": "In from the Old Tingri (Gangga) side, following the lateral moraine of the Rongbuk glacier to base camp, all of it above four thousand five hundred. No technical ground, but high and windy, so build in proper acclimatisation days. Tibet permits are required and foreign visitors face extra restrictions, so file the itinerary ahead. May to October works; in winter the approach is often snowed shut.",
    "shishapangma": "In from the Nyalam side; the south base camp sits around five thousand metres and sees very few people. All of it is high altitude and the first days are for acclimatisation, so do not run a lowland schedule. Border permits and reserve permits are needed, and the paperwork is harder than the walking. May to October is the window; July and August bring monsoon cloud, so seeing the mountain is luck.",
    "zhuoga": "In from Tingri via Yüpa village, Cuoxue Renma and Tangxiang — the classic east-face circuit of Everest. Cuoxue Renma shows Everest and Makalu in one frame and is the image the whole route is built around. Everything is above four thousand metres, topping out at five thousand three hundred, and yak transport must be arranged ahead. May to October works, with October giving the best odds of clear skies.",
    "ebc": "From Lukla via Namche, Tengboche, Dingboche and Lobuche to Everest Base Camp. Flights are weather-cancelled often, so keep a buffer day at each end and do not lock your return ticket. Altitude is the only real opponent: climb slowly, and take an extra day if the headache worsens. Add Kala Patthar on the way down for sunset — the best angle on Everest. March to May and October to November are the windows.",
    "act": "From Besisahar, a full circuit of the Annapurna massif over Thorong La and back toward Pokhara. Start before dawn on crossing day; wind and weather deteriorate after noon, so do not be late. Jeeps now reach parts of the route, so you can shorten it to match your legs. October–November and March–April are best; in winter the pass holds snow and needs gear and experience.",
    "threepasses": "Three five-thousand-metre passes added either side of the EBC line: Kongma La, Cho La and Renjo La. Far fewer people than the main trail, but each pass is another climb, so acclimatisation days are not optional. Cho La has a glacier section; winter and early spring need crampons and preferably a guide. October and November are most stable; March to May is windy with more cloud.",
    "kailash": "From Tarchen, clockwise around the mountain; the first night is usually at Drirapuk and the second day crosses Dolma La. The pass is the highest and hardest part — steep and thin on oxygen, so walk slowly. In the Tibetan Year of the Horse one circuit counts as thirteen, and beds get scarce. May to October works and winter snow closes the kora; respect local rules, no anticlockwise walking and no climbing.",
}

BRIEF_EN = {
    "longji": "Hong Kong Island's classic coastal ridge. Eight kilometres link beach, ridge and bay. Little shade, so start early in summer.",
    "xiangshan": "The closest hill to central Beijing: continuous stone steps and a dense trail network. A good first half-day with a pack; in autumn the summit path is busier than the maples.",
    "wutong": "Shenzhen's highest point. The Haohan Slope is a long, steep push and the standard training ground in South China. The summit looks over the whole bay.",
    "huihang": "The old merchants' trail out of the mountains, from Jixi to Lin'an in one day. Stone slabs and dirt, and a safe first long walk.",
    "laugavegur": "Iceland's highland trail in four days. Rhyolite colours, black desert and geothermal ground sit side by side, with four seasons in a day.",
    "trolltunga": "Norway's famous rock shelf, 28 km out and back. The climb is concentrated in the first half, and wet rock is slippery.",
    "camino": "From the French border to Santiago, a month at twenty-odd kilometres a day. Dormitory beds, and the test is repetition.",
    "taishan": "Seven thousand steps from Red Gate to Jade Emperor Peak, with shrines the whole way. Not a place to learn technique, but a place to learn pacing.",
    "overland": "Six days across Tasmania, alternating boardwalk and open moor. Book campsites ahead; all camping is on platforms.",
    "huangshan": "West Sea Grand Canyon is the steepest part of Huangshan; the loop takes a full day. The steps are slick, so avoid the gorge in rain.",
    "wugong": "Miles of alpine meadow along the ridge; camping for sunrise is the reason to come. No water on the ridge, so plan weight and refills.",
    "hallasan": "Up Seongpanak and down Gwaneumsa in one day. The summit is a volcanic lake that disappears in cloud.",
    "tongariro": "A one-day volcanic crossing with the Emerald Lakes as the signature. Weather changes fast, and winter needs crampons.",
    "huashan": "Cable cars reach the five peaks, but walking up is the point. The plank walk is optional; the real work is the climb from North Peak to East Peak.",
    "gr20": "Corsica's ridge traverse, fifteen days. Scree, bare rock and hard sun make it one of Europe's toughest long routes.",
    "changbaishan": "West-slope steps climb straight to the crater rim. On a clear day the whole Tianchi lake is visible. Low altitude, but strong wind and a big temperature swing.",
    "rim2rim": "Down the North Rim and up the South Rim, gaining and losing thousands of feet in a day. The inner canyon is brutally hot in summer; spring and autumn are the window.",
    "paine": "Five days along the W, with the Cuernos and French Valley as the two highlights. Windy, so the tent has to hold.",
    "hutiaoxia": "The High Trail hangs on the north wall above the Jinsha River. Two days; the rockfall section is the dangerous part, so do not walk it after rain.",
    "tmb": "A circuit of Mont Blanc through France, Italy and Switzerland. Huts every night, so a light pack works; the first Alpine multi-day for most people.",
    "halfdome": "Yosemite's icon. The last section is a cable climb up bare rock. Permits are hard to get, and it is 22 km round trip.",
    "av1": "The north-south line through the Dolomites, ten days. Limestone towers all the way and excellent huts, but book ahead in season.",
    "wutai": "The Great Pilgrimage links five summit platforms, 50 km in three days. At 3,000 m the weather turns fast, and it can snow in summer.",
    "emeishan": "A long continuous climb from Baoguo Temple to the Golden Summit. Monkeys will grab food. Split it over two days and sleep at Hongchunping or Xianfeng Temple.",
    "poonhill": "The beginner viewpoint of the Annapurnas, four days out and back. At sunrise Dhaulagiri and Annapurna line up across the sky.",
    "yubeng": "You walk in, or take a villager's car; you walk out. Ice Lake and the Sacred Waterfall are the two side trips, and four days covers both.",
    "changping": "The valley below Siguniang; beyond Muluozi it turns properly wild. Not high, but the weather inside the gorge changes fast.",
    "taibai": "The Qinling's highest peak. Two days from Houzhenzi or Tangyu to Baxiantai. High altitude plus distance makes it one of the classic northwest traverses.",
    "fuji": "The Yoshida Trail has the most huts and the best support of the four. Open only in summer; leave at night to catch sunrise.",
    "yushan": "Taiwan's highest peak, and the summit needs a permit lottery. Above Paiyun Lodge it is scree, with chains on the last section.",
    "abc": "Up the valley to Annapurna Base Camp in about ten days. Gentle altitude gain, and the standard first high-altitude multi-day trek.",
    "toubkal": "North Africa's highest peak, three days out and back. Villages and walnut groves first, then scree.",
    "inca": "Four days to Machu Picchu, with daily permits. Dead Woman's Pass is the high point, above 4,000 m.",
    "jmt": "Yosemite to Mount Whitney in three weeks, all wild camping, permit lottery first. The benchmark long trail in North America.",
    "yading": "From Luorong pasture to Milk Lake, all above 4,000 m, done in a day. Horses stop halfway; the last part is on foot.",
    "gongga": "Zimei Pass is the most direct view of Gongga's main summit, and sunrise turns it gold. The loop road is rough and often washed out in the monsoon.",
    "santacruz": "A classic loop in the Cordillera Blanca, four days over a 4,750 m pass. Arrieros and mules share the trail, and the pace is slow.",
    "chooyu": "Follow the lateral moraine of the Rongbuk glacier to base camp, close to 5,000 m but with no technical ground. A good first 5,000 m trek.",
    "shishapangma": "The south base camp route on the Nyalam side is quiet and raw. All high altitude, so build in acclimatisation days.",
    "zhuoga": "From Tingri into the valley, Cuoxue Renma is where Everest and Makalu line up together. Ten days or more, with yaks carrying the load.",
    "ebc": "Lukla to Everest Base Camp in fourteen days. The trail is not the hard part; altitude is, and going up too fast is what puts people in trouble.",
    "act": "A full circuit of the Annapurnas over Thorong La. Mature trail, good lodges, and a classic long-distance walk.",
    "threepasses": "Three 5,000 m passes beyond the main EBC route, eighteen days. Fewer people and bigger views, but you re-acclimatise every day.",
    "kailash": "Three days around the mountain, with Dolma La as the high point. Busiest in the horse year of the Tibetan calendar; book beds ahead even in normal years.",
}


def parse():
    s = open(SRC, encoding="utf-8").read()
    rows = [m.groupdict() for m in ROW.finditer(s)]
    if not rows:
        sys.exit("没有解析到任何路线，检查 HikingRoutes.kt 的格式")
    ids = [r["id"] for r in rows]
    dup = {i for i in ids if ids.count(i) > 1}
    if dup:
        sys.exit("重复 id: %s" % sorted(dup))
    return rows


def kt(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")



INTRO_SRC = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/HikeIntro.kt")
INTRO_BLOCK = re.compile(r'"([a-z0-9_]+)" to\n((?:\s+"[^"]*"(?: \+)?\n)+)')


def parse_intro():
    """从 HikeIntro.kt 读中文长介绍（`"id" to "句1" + "句2"` 拼接）。"""
    src = open(INTRO_SRC, encoding="utf-8").read()
    out = {}
    for rid, body in INTRO_BLOCK.findall(src):
        out[rid] = "".join(re.findall(r'"([^"]*)"', body))
    return out


def emit(path, name, rows, source):
    lines = [
        "package com.ascent.climb.data.i18n",
        "",
        "/**",
        " * %s" % source,
        " *",
        " * 由 `tools/i18n/gen_hike_packs.py` 從 `data/HikingRoutes.kt` 生成，不要手改。",
        " */",
        "val %s: Map<String, String> = mapOf(" % name,
    ]
    for r in rows:
        lines.append('    "hike.%s.name" to "%s",' % (r["id"], kt(r["name"])))
        lines.append('    "hike.%s.area" to "%s",' % (r["id"], kt(r["area"])))
        lines.append('    "hike.%s.brief" to "%s",' % (r["id"], kt(r["brief"])))
        if r.get("intro"):
            lines.append('    "hike.%s.intro" to "%s",' % (r["id"], kt(r["intro"])))
    lines.append(")")
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")


def main():
    rows = parse()
    zh_intro = parse_intro()
    missing_area = [r["id"] for r in rows if r["id"] not in AREA_EN]
    missing_brief = [r["id"] for r in rows if r["id"] not in BRIEF_EN]
    missing_intro = [r["id"] for r in rows if r["id"] not in zh_intro]
    missing_intro_en = [r["id"] for r in rows if r["id"] not in INTRO_EN]
    if missing_area or missing_brief or missing_intro or missing_intro_en:
        sys.exit("缺条目 area=%s brief=%s intro_zh=%s intro_en=%s"
                 % (missing_area, missing_brief, missing_intro, missing_intro_en))

    en_rows = [dict(id=r["id"], name=r["en"], area=AREA_EN[r["id"]],
                    brief=BRIEF_EN[r["id"]], intro=INTRO_EN[r["id"]])
               for r in rows]

    if "--check" in sys.argv:
        print("路线 %d 条，英文 area/brief 齐全" % len(rows))
        print("英文名空缺:", [r["id"] for r in en_rows if not r["name"].strip()])
        return

    emit(OUT_EN, "PACK_EN_HIKE", en_rows,
         "徒步路线的英文名称 / 地区 / 简介。其余语言统一回退到这里。")

    # 繁中：OpenCC s2twp + 计量单位校正（与 tw.py 同一套规则，避免两套用词）
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import opencc

    cc = opencc.OpenCC("s2twp")
    termfix = [("千米", "公里"), ("釐米", "公分"), ("厘米", "公分"),
               ("平方米", "平方公尺"), ("高原反應", "高山症反應"), ("高原病", "高山症")]
    mi = re.compile(r"(?<=[0-9０-９一二三四五六七八九十百千萬万兩两幾几半\s])米")

    def conv(s):
        s = cc.convert(s)
        for a, b in termfix:
            s = s.replace(a, b)
        return mi.sub("公尺", s)

    tw_rows = [dict(id=r["id"], name=conv(r["zh"]), area=conv(r["area"]),
                    brief=conv(r["brief"]), intro=conv(zh_intro[r["id"]]))
               for r in rows]
    emit(OUT_TW, "PACK_ZH_TW_HIKE", tw_rows,
         "徒步路線的繁體中文名稱 / 地區 / 簡介。")

    print("写入 %s / %s，共 %d 条路线 × 4 键（name/area/brief/intro）" % (
        os.path.basename(OUT_EN), os.path.basename(OUT_TW), len(rows)))


if __name__ == "__main__":
    main()

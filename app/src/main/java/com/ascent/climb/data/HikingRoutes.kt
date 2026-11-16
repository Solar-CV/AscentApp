package com.ascent.climb.data

import com.ascent.climb.R

/**
 * 徒步路线百科数据集。
 *
 * 与 [MountainMilestones] 同构，但对象是「线路」而不是「山峰」：
 *  1. 路线实体 —— [HikingRoute]
 *  2. 多语言名称 / 地区 / 简介 —— 内容层 `hike.<id>.*`（英文与繁中先落地，其余回退英文）
 *  3. 解锁里程碑 —— [HikingRoute.milestoneAlt]：徒步海拔达到多少时解锁
 *
 * ## 选线口径
 *
 * 只收**公开开放、有稳定轨迹与补给/营地、普通徒步者可以合法完成**的线路。以下一律不收：
 *  - 明令禁止穿越：鳌太线、夏特古道、乌孙古道、狼塔 C 线
 *  - 保护区内未开放或需特批：可可西里、年保玉则核心区、洛克线部分段落
 *  - 需要技术攀登的路线：雀儿山、慕士塔格、岛峰等（属于攀登轨道，不是徒步）
 *  - 最高点高于徒步轨道的终点 5,630 m：乞力马扎罗（5,895 m）等，放进来永远解不开
 *
 * 收录的国际线路也按同一把尺：印加古道、JMT、TMB 都有官方许可制度，属于「限量但合法」；
 * 允许但不推荐的野线不进清单。
 *
 * ## 里程碑
 *
 * 与山峰不同，路线不取整到 400 m 档位，而是**最高点向下取整到 50 m**：
 * 22 节课把海拔从 200 m 推到 5,630 m，每节课约 210 m，一座山一档太稀，
 * 一条线一档正好让每次升级都能看到新东西。里程碑恒 ≤ 最高点。
 */
enum class HikeRegion(val key: String) {
    CN("cn"),               // 中国大陆、香港、台湾
    ASIA("asia"),           // 尼泊尔 / 日本 / 韩国
    EUROPE("europe"),
    AMERICAS("americas"),
    AFRICA("africa"),
    OCEANIA("oceania")
}

data class HikingRoute(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    /** 展示用地区，如「中国 · 云南」。 */
    val areaZh: String,
    val region: HikeRegion,
    /** 线路最高点。 */
    val maxAlt: Int,
    val distanceKm: Int,
    val days: Int,
    /** 全程累计爬升，展示用近似值。 */
    val gainM: Int,
    /** 海拔剖面采样点（主要营地 / 垭口），用于画曲线，不是逐米轨迹。 */
    val profile: List<Int>,
    /** 中文简介原文；译文走内容层。 */
    val briefZh: String,
    /** 路线地形图；为 null 时退回海拔剖面折线。 */
    val drawable: Int? = null
) {
    /** 解锁里程碑：最高点向下取整到 50 m。恒 ≤ [maxAlt]。 */
    val milestoneAlt: Int get() = maxAlt / 50 * 50
}

object HikingRoutes {

    /** 条目构造器，把一条线路压成一人一行。 */
    private fun r(
        id: String,
        zh: String,
        en: String,
        area: String,
        region: HikeRegion,
        maxAlt: Int,
        km: Int,
        days: Int,
        gain: Int,
        profile: List<Int>,
        brief: String,
        drawable: Int? = null
    ) = HikingRoute(id, zh, en, area, region, maxAlt, km, days, gain, profile, brief, drawable)

    /**
     * 全部线路，按里程碑升序，同里程碑按最高点升序。
     *
     * 42 条：国内 22 条（含香港、台湾），尼泊尔 5 条，日本 / 韩国各 1 条，
     * 欧洲 6 条，美洲 4 条，非洲 1 条，大洋洲 2 条。
     */
    val ALL: List<HikingRoute> = listOf(
        // ================= 城市近郊 =================
        r("longji", "龙脊 · 打烂埕顶", "Dragon's Back", "中国香港", HikeRegion.CN,
            284, 8, 1, 350, listOf(50, 150, 284, 200, 60),
            "香港岛最经典的海岸山脊线，八公里内把沙滩、山脊与海湾连成一串。爬升不大，但全程少树荫，夏天要趁早。",
            R.drawable.hike_longji),
        r("xiangshan", "香山 · 鬼见愁", "Fragrant Hills", "中国 · 北京", HikeRegion.CN,
            575, 8, 1, 500, listOf(100, 250, 400, 575, 400, 120),
            "北京城最近的入门山，石阶连续、路网密，适合第一次背着包走满半天。秋天香炉峰一线的人比红叶还多。",
            R.drawable.hike_xiangshan),

        // ================= 1,000 m 上下 =================
        r("wutong", "梧桐山 · 好汉坡", "Wutong Mountain", "中国 · 广东", HikeRegion.CN,
            944, 12, 1, 900, listOf(80, 300, 550, 800, 944, 700, 80),
            "深圳最高峰，好汉坡一段连续陡上，是华南练爬升的常用场地。山顶能看到整个深圳湾。",
            R.drawable.hike_wutong),
        r("huihang", "徽杭古道", "Huihang Ancient Trail", "中国 · 安徽 / 浙江", HikeRegion.CN,
            1050, 15, 1, 700, listOf(300, 500, 800, 1050, 700, 350),
            "徽商走出大山的旧道，从绩溪到临安，一天走完。路面多是石板与土路，是「第一次长距离」的稳妥选择。",
            R.drawable.hike_huihang),
        r("laugavegur", "Laugavegur 高地线", "Laugavegur Trail", "冰岛", HikeRegion.EUROPE,
            1060, 55, 4, 2000, listOf(300, 500, 700, 900, 1060, 850, 600, 300),
            "冰岛高地线，四天。彩色流纹岩、黑沙漠与地热区挨在一起，天气一天四季。",
            R.drawable.hike_laugavegur),
        r("trolltunga", "恶魔之舌", "Trolltunga", "挪威", HikeRegion.EUROPE,
            1100, 28, 1, 1000, listOf(400, 700, 1000, 1100, 900, 600, 400),
            "挪威最出名的岩石平台，往返二十八公里。爬升集中在前半段，雨天岩石很滑。",
            R.drawable.hike_trolltunga),
        r("camino", "朝圣之路 · 法国线", "Camino Francés", "西班牙", HikeRegion.EUROPE,
            1300, 780, 30, 8000, listOf(200, 400, 600, 800, 1000, 1300, 1000, 700, 400, 200),
            "从法国边境走到圣地亚哥，一个月。每天二十多公里，住宿是通铺，考验的是重复。",
            R.drawable.hike_camino),

        // ================= 1,500 m 上下 =================
        r("taishan", "泰山 · 红门—玉皇顶", "Mount Tai · Red Gate to Jade Emperor Peak", "中国 · 山东", HikeRegion.CN,
            1545, 9, 1, 1350, listOf(250, 600, 900, 1200, 1545, 900, 250),
            "从红门到玉皇顶七千多级台阶，一路都是人文遗迹。它不适合练技术，但适合练节奏——匀速的人比冲的人先到。",
            R.drawable.hike_taishan),
        r("overland", "Overland Track", "Overland Track", "澳大利亚", HikeRegion.OCEANIA,
            1617, 65, 6, 2000, listOf(900, 1000, 1100, 1200, 1400, 1617, 1400, 1200, 1000, 900),
            "塔斯马尼亚的六天线，木栈道与荒原交替。需要提前订营位，全程有平台扎营。",
            R.drawable.hike_overland),

        // ================= 1,800 m 上下 =================
        r("huangshan", "黄山 · 西海大峡谷", "Huangshan · West Sea Grand Canyon", "中国 · 安徽", HikeRegion.CN,
            1864, 12, 2, 1100, listOf(600, 900, 1200, 1500, 1864, 1400, 800),
            "西海大峡谷是黄山最陡的一段，环线走完要一整天。台阶湿滑，雨天不建议下谷。",
            R.drawable.hike_huangshan),
        r("wugong", "武功山 · 金顶", "Wugong Mountain · Golden Summit", "中国 · 江西", HikeRegion.CN,
            1918, 22, 2, 1400, listOf(500, 900, 1300, 1600, 1918, 1500, 900, 500),
            "十万亩高山草甸铺在山脊上，露营看日出是主要理由。山脊无水，负重和补水要提前算。",
            R.drawable.hike_wugong),
        r("hallasan", "汉拿山 · 城板岳", "Hallasan · Seongpanak", "韩国", HikeRegion.ASIA,
            1950, 19, 1, 1200, listOf(750, 1000, 1300, 1600, 1950, 1600, 1000, 750),
            "从城板岳上山、观音寺下山，一天走完。山顶是火山湖，天气不好时什么都看不到。",
            R.drawable.hike_hallasan),
        r("tongariro", "Tongariro 火山穿越", "Tongariro Alpine Crossing", "新西兰", HikeRegion.OCEANIA,
            1886, 20, 1, 800, listOf(1100, 1300, 1500, 1700, 1886, 1700, 1400, 1100),
            "一天走完的火山穿越，翡翠湖是标志。天气说变就变，冬季需要冰爪。",
            R.drawable.hike_tongariro),
        r("huashan", "华山 · 五峰", "Mount Hua · Five Peaks", "中国 · 陕西", HikeRegion.CN,
            2155, 10, 1, 1500, listOf(400, 800, 1200, 1600, 2155, 1500, 700),
            "五峰各有索道，但走上去才算数。长空栈道只是支线，真正耗体力的是北峰到东峰那一段连续上升。",
            R.drawable.hike_huashan),

        // ================= 2,200 m 上下 =================
        r("gr20", "GR20 科西嘉纵线", "GR20 Corsica", "法国", HikeRegion.EUROPE,
            2225, 180, 15, 12000, listOf(200, 600, 1000, 1400, 1800, 2225, 1900, 1500, 1100, 600, 200),
            "科西嘉岛的山脊纵线，十五天。碎石、裸岩与暴晒，是欧洲最硬的长距离线之一。",
            R.drawable.hike_gr20),
        r("changbaishan", "长白山 · 西坡", "Changbai Mountain · West Slope", "中国 · 吉林", HikeRegion.CN,
            2470, 14, 1, 800, listOf(1700, 1900, 2100, 2300, 2470, 2100, 1700),
            "西坡台阶直上火山口边缘，天气好时能看到天池全貌。海拔不高，但风大、温差大。"),
        r("rim2rim", "大峡谷 · Rim-to-Rim", "Grand Canyon · Rim-to-Rim", "美国", HikeRegion.AMERICAS,
            2438, 34, 2, 1800, listOf(2100, 1500, 1100, 800, 1100, 1500, 2100),
            "从北缘下到谷底再上南缘，一天之内经历三千英尺的下降与爬升。夏天谷底极热，春秋才是窗口。",
            R.drawable.hike_rim2rim),
        r("paine", "百内 W 线", "Torres del Paine W Trek", "智利", HikeRegion.AMERICAS,
            1200, 80, 5, 3000, listOf(100, 300, 600, 900, 1200, 900, 600, 300, 100),
            "五天走完 W 形，百内角与法国谷是两处高潮。风大，帐篷要抗风。",
            R.drawable.hike_paine),

        // ================= 2,600 m 上下 =================
        r("hutiaoxia", "虎跳峡 · 高路", "Tiger Leaping Gorge · High Trail", "中国 · 云南", HikeRegion.CN,
            2670, 22, 2, 1200, listOf(1800, 2100, 2400, 2670, 2300, 1900),
            "高路挂在峡谷北侧，脚下是金沙江。两天走完，最险的是雨季的落石段，别在雨后走。",
            R.drawable.hike_hutiaoxia),
        r("tmb", "环勃朗峰 TMB", "Tour du Mont Blanc", "法国 · 意大利 · 瑞士", HikeRegion.EUROPE,
            2665, 170, 11, 10000, listOf(1000, 1400, 1800, 2200, 2500, 2665, 2300, 1800, 1200),
            "绕勃朗峰一圈，横跨法意瑞三国。每天都有山屋，轻装即可，是第一次阿尔卑斯多日线的首选。",
            R.drawable.hike_tmb),
        r("halfdome", "半圆顶", "Half Dome", "美国", HikeRegion.AMERICAS,
            2694, 22, 1, 1500, listOf(1200, 1600, 2000, 2400, 2694, 2400, 1800, 1200),
            "优胜美地的标志，最后一段靠钢缆上岩壁。抽签难，往返二十二公里。",
            R.drawable.hike_halfdome),
        r("av1", "多洛米蒂 Alta Via 1", "Dolomites Alta Via 1", "意大利", HikeRegion.EUROPE,
            2752, 120, 10, 6000, listOf(1200, 1600, 2000, 2400, 2752, 2300, 1800, 1400),
            "多洛米蒂的南北纵线，十天。石灰岩塔峰一路相随，山屋质量高，但旺季要提前订。",
            R.drawable.hike_av1),

        // ================= 3,000 m 上下 =================
        r("wutai", "五台山 · 大朝台", "Mount Wutai · Great Pilgrimage Circuit", "中国 · 山西", HikeRegion.CN,
            3061, 50, 3, 2000, listOf(1800, 2200, 2500, 2800, 3061, 2700, 2300, 1900),
            "大朝台把五座台顶连成一圈，三天五十公里。海拔三千米，天气说变就变，夏季也可能下雪。",
            R.drawable.hike_wutai),
        r("emeishan", "峨眉山 · 金顶", "Mount Emei · Golden Summit", "中国 · 四川", HikeRegion.CN,
            3099, 50, 2, 2300, listOf(500, 900, 1300, 1700, 2100, 2500, 2800, 3099),
            "从报国寺到金顶是长距离连续爬升，猴子会抢东西。分两天走，第一天住洪椿坪或仙峰寺。",
            R.drawable.hike_emeishan),
        r("poonhill", "Poon Hill 观景线", "Poon Hill", "尼泊尔", HikeRegion.ASIA,
            3210, 45, 4, 2000, listOf(1600, 2000, 2400, 2800, 3210, 2800, 2000, 1600),
            "安纳普尔纳的入门观景线，四天往返。日出时道拉吉里与安纳普尔纳一字排开。",
            R.drawable.hike_poonhill),
        r("yubeng", "雨崩", "Yubeng", "中国 · 云南", HikeRegion.CN,
            3800, 45, 4, 2400, listOf(2200, 2600, 3000, 3200, 3600, 3800, 3200, 2400),
            "进村只能徒步或坐村民的车，出村靠腿。冰湖与神瀑是两条支线，四天刚好走完。",
            R.drawable.hike_yubeng),

        // ================= 3,700 m 上下 =================
        r("changping", "四姑娘山 · 长坪沟", "Siguniang · Changping Valley", "中国 · 四川", HikeRegion.CN,
            3700, 29, 2, 1300, listOf(3200, 3400, 3550, 3700, 3500, 3300),
            "四姑娘山脚下的沟谷线，木骡子之后是真正的荒野。海拔不高，但沟里天气变化快。",
            R.drawable.hike_changping),
        r("taibai", "太白山 · 拔仙台", "Mount Taibai · Baxiantai", "中国 · 陕西", HikeRegion.CN,
            3771, 40, 3, 2600, listOf(1300, 1800, 2300, 2800, 3300, 3771, 3200, 2500),
            "秦岭主峰，从厚畛子或汤峪上山，两天到拔仙台。高海拔加长距离，是西北最经典的穿越线之一。",
            R.drawable.hike_taibai),
        r("fuji", "富士山 · 吉田线", "Mount Fuji · Yoshida Trail", "日本", HikeRegion.ASIA,
            3776, 14, 2, 1500, listOf(2300, 2700, 3000, 3300, 3600, 3776, 3300, 2300),
            "吉田线是四条线里设施最全的一条，山屋密集。只在夏季开放，山顶看日出要夜里出发。",
            R.drawable.hike_fuji),

        // ================= 4,000 m 上下 =================
        r("yushan", "玉山主峰", "Yushan Main Peak", "中国台湾", HikeRegion.CN,
            3952, 22, 2, 1400, listOf(2600, 3000, 3400, 3800, 3952, 3600, 3000),
            "台湾最高峰，登顶需要提前抽签。排云山庄之后是碎石坡，最后一段要拉铁链。",
            R.drawable.hike_yushan),
        r("abc", "ABC 安纳普尔纳大本营", "Annapurna Base Camp", "尼泊尔", HikeRegion.ASIA,
            4130, 90, 10, 5000, listOf(1100, 1500, 1900, 2300, 2700, 3100, 3500, 3800, 4130),
            "沿峡谷一路走到安纳普尔纳大本营，十天左右。海拔渐进，是第一次高原多日线的标准答案。",
            R.drawable.hike_abc),
        r("toubkal", "图卜卡勒", "Mount Toubkal", "摩洛哥", HikeRegion.AFRICA,
            4167, 40, 3, 2400, listOf(1800, 2200, 2600, 3000, 3400, 4167, 3400, 2600, 1800),
            "北非最高峰，三天往返。前半段是村庄与核桃林，后半段是碎石坡。",
            R.drawable.hike_toubkal),

        // ================= 4,500 m 上下 =================
        r("inca", "印加古道", "Inca Trail", "秘鲁", HikeRegion.AMERICAS,
            4215, 43, 4, 2000, listOf(2400, 2800, 3300, 3700, 4215, 3900, 3400, 2900, 2400),
            "四天走到马丘比丘，每天限流。最高点是死妇人垭口，海拔四千米以上。",
            R.drawable.hike_inca),
        r("jmt", "约翰缪尔步道 JMT", "John Muir Trail", "美国", HikeRegion.AMERICAS,
            4421, 340, 21, 14000, listOf(1200, 1800, 2400, 3000, 3400, 3800, 4421, 4000, 3400, 2800),
            "从优胜美地到惠特尼峰，三周。全程野营，需要提前抽许可，是北美长线的标杆。",
            R.drawable.hike_jmt),
        r("yading", "稻城亚丁 · 牛奶海", "Yading · Milk Lake", "中国 · 四川", HikeRegion.CN,
            4600, 12, 1, 1000, listOf(4100, 4300, 4500, 4600, 4400, 4150),
            "从洛绒牛场到牛奶海，海拔四千米以上，单日往返。马帮只到半路，最后一段只能走。",
            R.drawable.hike_yading),
        r("gongga", "贡嘎 · 子梅垭口", "Gongga · Zimei Pass", "中国 · 四川", HikeRegion.CN,
            4650, 40, 4, 2000, listOf(3200, 3700, 4200, 4650, 4200, 3600, 3200),
            "子梅垭口是看贡嘎主峰最直接的位置，日照金山在清晨。环线路况差，雨季常塌方。"),
        r("santacruz", "圣克鲁斯", "Santa Cruz Trek", "秘鲁", HikeRegion.AMERICAS,
            4750, 50, 4, 2200, listOf(2900, 3400, 3900, 4300, 4750, 4300, 3800, 3200),
            "布兰卡山脉的经典环线，四天翻一个四千七百米垭口。商队与驴马共用山路，节奏很慢。",
            R.drawable.hike_santacruz),
        r("chooyu", "卓奥友 · 大本营", "Cho Oyu Base Camp", "中国 · 西藏", HikeRegion.CN,
            4950, 45, 6, 1800, listOf(4300, 4500, 4700, 4900, 4950, 4700, 4300),
            "沿绒布冰川侧碛走到大本营，海拔近五千米，但没有技术路段。适合作为第一次五千米级徒步。"),

        // ================= 5,000 m 以上 =================
        r("shishapangma", "希夏邦马 · 南坡大本营", "Shishapangma South Base Camp", "中国 · 西藏", HikeRegion.CN,
            5000, 50, 7, 2200, listOf(4000, 4300, 4600, 4800, 5000, 4700, 4100),
            "南坡大本营线在聂拉木一侧，人少、路原始。全程高海拔，需要预留适应日。"),
        r("zhuoga", "珠峰东坡 · 措学仁玛", "Everest East Face · Cuoxue Renma", "中国 · 西藏", HikeRegion.CN,
            5350, 80, 10, 4000, listOf(3800, 4200, 4600, 5000, 5300, 5350, 4900, 4200),
            "从定日进沟，措学仁玛是看珠峰与马卡鲁同框的位置。十天以上，补给全靠牦牛。"),
        r("ebc", "EBC 珠峰大本营", "Everest Base Camp", "尼泊尔", HikeRegion.ASIA,
            5364, 130, 14, 5500, listOf(2860, 3440, 3250, 3860, 4410, 4940, 5164, 5364),
            "从卢卡拉走到珠峰大本营，十四天。真正难的不是路，是海拔——上升太快的人在这里最容易出事。",
            R.drawable.hike_ebc),
        r("act", "安纳普尔纳大环线 ACT", "Annapurna Circuit", "尼泊尔", HikeRegion.ASIA,
            5416, 160, 14, 6000, listOf(800, 1200, 1600, 2000, 2600, 3200, 3800, 4500, 5416, 4500, 3000, 1500),
            "绕安纳普尔纳一圈，翻陀龙垭口。路况成熟、补给充足，是长距离徒步的经典。",
            R.drawable.hike_act),
        r("threepasses", "EBC · 三垭口", "Everest Three Passes", "尼泊尔", HikeRegion.ASIA,
            5545, 150, 18, 7000, listOf(2860, 3440, 3860, 4410, 4940, 5350, 5545, 5000, 4500, 3860),
            "在 EBC 主线之外翻三座五千米垭口，十八天。人少、景好，但每天都要重新适应。",
            R.drawable.hike_threepasses),
        r("kailash", "冈仁波齐转山", "Mount Kailash Kora", "中国 · 西藏", HikeRegion.CN,
            5630, 52, 3, 1300, listOf(4600, 4900, 5200, 5630, 5200, 4800, 4600),
            "转山三天，最高点是卓玛拉垭口。藏历马年人最多，平时也要提前订住宿。",
            R.drawable.hike_kailash)
    ).sortedWith(compareBy({ it.milestoneAlt }, { it.maxAlt }, { it.id }))

    /** 地区偏好：同一条里程碑有多条线路时，国内线路优先。 */
    private val HOME_REGIONS = listOf(HikeRegion.CN)

    /**
     * 挑当前该展示的那一条。
     *
     * 规则与山峰一致：先取「已解锁的最高里程碑」，同里程碑内优先国内，再取最高点最高者。
     * 一条都没解锁时，取最近的一条（让用户看到下一步差多少）。
     */
    fun pick(currentAlt: Int, preferred: HikeRegion? = null): HikingRoute {
        val reached = ALL.filter { it.milestoneAlt <= currentAlt }
        val pool = if (reached.isNotEmpty()) {
            val top = reached.maxOf { it.milestoneAlt }
            ALL.filter { it.milestoneAlt == top }
        } else {
            val first = ALL.minOf { it.milestoneAlt }
            ALL.filter { it.milestoneAlt == first }
        }
        preferred?.let { p ->
            pool.filter { it.region == p }.maxByOrNull { it.maxAlt }?.let { return it }
        }
        pool.filter { it.region in HOME_REGIONS }.maxByOrNull { it.maxAlt }?.let { return it }
        return pool.maxByOrNull { it.maxAlt } ?: ALL.first()
    }

    /** 已解锁的线路，按里程碑升序。 */
    fun unlocked(currentAlt: Int): List<HikingRoute> =
        ALL.filter { it.milestoneAlt <= currentAlt }.sortedBy { it.milestoneAlt }

    /** 全部线路：已解锁在前，未解锁在后；未解锁部分把「本区」的提到最前。 */
    fun history(currentAlt: Int, prefer: HikeRegion? = null): List<HikingRoute> {
        val done = unlocked(currentAlt)
        val locked = ALL.filter { it.milestoneAlt > currentAlt }
            .sortedWith(compareBy({ regionRank(it.region, prefer) }, { it.milestoneAlt }))
        return done + locked
    }

    /** 0 = 用户所在区，1 = 国内，2 = 其他。只影响未解锁部分的排序，不做过滤。 */
    private fun regionRank(r: HikeRegion, prefer: HikeRegion?): Int = when {
        prefer != null && r == prefer -> 0
        r in HOME_REGIONS -> 1
        else -> 2
    }

    /** 已解锁线路里的最高点；一条都没有返回 0。 */
    fun highestUnlocked(currentAlt: Int): Int =
        unlocked(currentAlt).maxOfOrNull { it.maxAlt } ?: 0
}

/**
 * 按界面语言推断用户大致所在的大区。
 *
 * 只用来给列表排序，不做过滤，也不申请定位权限。语言与地理不是一一对应的：
 * 英语无法判断（返回 null，不排序），西语/法语/俄语横跨多个大区，
 * 按母语所在地取主流解释——西班牙、法国、俄罗斯欧洲部分。
 */
object RegionHint {

    fun forTag(tag: String): HikeRegion? = when (tag.substringBefore('-').lowercase()) {
        "zh" -> HikeRegion.CN
        "ja" -> HikeRegion.ASIA
        "de", "sv", "it", "fr", "es", "ru" -> HikeRegion.EUROPE
        else -> null
    }
}

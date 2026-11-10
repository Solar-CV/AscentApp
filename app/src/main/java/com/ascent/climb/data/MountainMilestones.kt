package com.ascent.climb.data

import com.ascent.climb.R

/**
 * 山峰里程碑数据集。
 *
 * 三层结构，与产品设计方案一致：
 *  1. 山峰实体 —— [Mountain]
 *  2. 多语言名称 —— 十种语言全量内置（`data/i18n/Pack<Lang>Mtn.kt`，127 键）。
 *  3. 里程碑映射 —— [Mountain.milestoneAlt]：学习海拔达到多少时开始展示这座山
 *
 * **匹配在本地完成**，不联网、不上报。地区只作为排序权重，不改变"到哪一档"。
 *
 * 数据口径：
 *  - 海拔用官方公布值，**不用 DEM 读数**（SRTM 在 8000 m 以上会低估 100 m 左右）
 *  - 山体跨度、雪线是展示用近似值，按山脉形态与纬度估算，**不是测量数据**
 *  - 一处刻意的取舍：**里程碑不高于山高**。学 8,160 m 时看到 8,188 m 的卓奥友，
 *    "已到达"才站得住；反过来把 8,188 m 的山挂在 8,400 m 的档位上，用户一对照就露馅。
 *    唯一的例外是低海拔四座（贺兰山 3,556、雷尼尔山 4,392、阿尔金山 5,798、乃钦康桑 7,191），
 *    档位要落在 3,600 / 4,400 / 5,800 / 7,200 这些整数刻度上，最多高 44 m。
 *
 * 档位编排（见 [MILESTONES]）：
 *  - 3,600 / 4,400 —— 低海拔只留两档。曲线起步 3,200、第一课一步到 4,800，
 *    中间按 400 m 分四档等于造了永远停不住的档。3,600 是起步时抬头能看见的那一座，
 *    4,400 是第一课落地（4,800）之后卡片会切到的最高档。
 *  - 5,000 – 7,600 —— 200 m 一档，每档 3–5 座，地区偏好才真正起作用。
 *  - 7,780 —— 南迦巴瓦单独一档，作为八千米之前最后一座。
 *  - 8,000 以上 —— **一座一档**，14 座八千米各占一档。密集处（8,000–8,200 挤了 9 座）
 *    只能 20 m 一档，这是地理事实，不是排漏了：
 *    全世界在 8,188 m（卓奥友）到 8,485 m（马卡鲁）之间没有任何独立山峰。
 */
enum class MountainRegion(val key: String) {
    CN_SW("cn-sw"),          // 中国西南：川 / 滇 / 藏
    CN_NW("cn-nw"),          // 中国西北：新疆 / 青海 / 甘肃
    CENTRAL_ASIA("central-asia"),   // 帕米尔 / 天山 / 高加索 / 兴都库什
    ALPS("alps"),
    NORTH_AMERICA("na"),
    GLOBAL("global")         // 日本 / 非洲 / 南美 / 大洋洲 / 尼泊尔等
}

data class Mountain(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    /** 山峰真实海拔，官方值。 */
    val elevation: Int,
    /** 里程碑：学习海拔达到此值时展示。恒 ≤ [elevation]。 */
    val milestoneAlt: Int,
    val region: MountainRegion,
    /** 山体水平跨度（近似，展示用）。 */
    val spanKm: String,
    /** 雪线海拔（近似，展示用；热带高山无常年雪线时写文字说明）。 */
    val snowline: String,
    /** 所在山脉。 */
    val range: String,
    /** 地形图；为 null 时 UI 用线稿符号占位。 */
    val drawable: Int? = null,
    /** true = 等高线地形图（立体渲染做不出来的山），false = 山体渲染。 */
    val contour: Boolean = false
)

object MountainMilestones {

    private const val HIMAL = "喜马拉雅"
    private const val KARAKORAM = "喀喇昆仑"
    private const val HENGDUAN = "横断山"
    private const val PAMIR = "帕米尔"
    private const val TIANSHAN = "天山"
    private const val KUNLUN = "昆仑山"
    private const val KUNLUN_W = "昆仑山西段"
    private const val QILIAN = "祁连山"
    private const val ALPS_R = "阿尔卑斯"
    private const val ROCKY = "落基山"
    private const val ANDES = "安第斯"

    /** 条目构造器，把 88 座山压成一人一行，便于按档位核对。 */
    private fun m(
        id: String,
        zh: String,
        en: String,
        elev: Int,
        tier: Int,
        region: MountainRegion,
        span: String,
        snow: String,
        range: String,
        drawable: Int? = null,
        contour: Boolean = false
    ) = Mountain(id, zh, en, elev, tier, region, span, snow, range, drawable, contour)

    /**
     * 全部里程碑。按 [Mountain.milestoneAlt] 升序，同档内按海拔升序。
     */
    val ALL: List<Mountain> = listOf(
        // ================= 3,600 — 大本营能看见的那几座 =================
        m("helan", "贺兰山", "Helan Mountains", 3556, 3600, MountainRegion.CN_NW,
            "9 km", "3,400 m", "贺兰山", R.drawable.mtn_helan, contour = true),
        m("taibai", "太白山", "Mount Taibai", 3767, 3600, MountainRegion.CN_NW,
            "10 km", "3,500 m", "秦岭", R.drawable.mtn_taibai, contour = true),
        m("fuji", "富士山", "Mount Fuji", 3776, 3600, MountainRegion.GLOBAL,
            "12 km", "3,600 m", "富士火山", R.drawable.mtn_fuji, contour = true),
        m("xueshan", "雪山", "Xueshan", 3886, 3600, MountainRegion.CN_SW,
            "5 km", "3,400 m", "雪山山脉", R.drawable.mtn_xueshan, contour = true),
        m("yushan", "玉山", "Yushan", 3952, 3600, MountainRegion.CN_SW,
            "6 km", "3,500 m", "玉山山脉", R.drawable.mtn_yushan, contour = true),
        m("eiger", "艾格峰", "Eiger", 3967, 3600, MountainRegion.ALPS,
            "3 km", "3,000 m", ALPS_R, R.drawable.mtn_eiger, contour = true),
        m("kinabalu", "京那巴鲁山", "Mount Kinabalu", 4095, 3600, MountainRegion.GLOBAL,
            "8 km", "无常年积雪", "克罗克山脉", R.drawable.mtn_kinabalu, contour = true),
        m("jungfrau", "少女峰", "Jungfrau", 4158, 3600, MountainRegion.ALPS,
            "6 km", "3,100 m", ALPS_R, R.drawable.mtn_jungfrau, contour = true),
        m("teton", "大提顿峰", "Grand Teton", 4199, 3600, MountainRegion.NORTH_AMERICA,
            "5 km", "3,200 m", ROCKY, R.drawable.mtn_teton, contour = true),

        // ================= 4,400 — 第一课一落地，整片四千已经在脚下 =================
        m("rainier", "雷尼尔山", "Mount Rainier", 4392, 4400, MountainRegion.NORTH_AMERICA,
            "9 km", "3,000 m", "喀斯喀特", R.drawable.mtn_rainier, contour = true),
        m("whitney", "惠特尼山", "Mount Whitney", 4421, 4400, MountainRegion.NORTH_AMERICA,
            "6 km", "3,600 m", "内华达山", R.drawable.mtn_whitney, contour = true),
        m("matterhorn", "马特洪峰", "Matterhorn", 4478, 4400, MountainRegion.ALPS,
            "3.2 km", "3,300 m", ALPS_R, R.drawable.mtn_matterhorn),
        m("rosa", "罗莎峰", "Monte Rosa", 4634, 4400, MountainRegion.ALPS,
            "6 km", "3,300 m", ALPS_R, R.drawable.mtn_rosa, contour = true),
        m("blanc", "勃朗峰", "Mont Blanc", 4807, 4400, MountainRegion.ALPS,
            "9 km", "3,200 m", ALPS_R, R.drawable.mtn_blanc),
        m("jaya", "查亚峰", "Puncak Jaya", 4884, 4400, MountainRegion.GLOBAL,
            "5 km", "4,400 m", "苏迪曼", R.drawable.mtn_jaya, contour = true),
        m("vinson", "文森峰", "Mount Vinson", 4892, 4400, MountainRegion.GLOBAL,
            "5 km", "4,200 m", "埃尔斯沃思"),
        m("jiuding", "九顶山", "Jiuding Shan", 4989, 4400, MountainRegion.CN_SW,
            "8 km", "4,000 m", "岷山", R.drawable.mtn_jiuding, contour = true),

        // ================= 5,000 =================
        m("siguniang_da", "四姑娘山大峰", "Siguniang Da Feng", 5025, 5000, MountainRegion.CN_SW,
            "4 km", "4,600 m", "邛崃山", R.drawable.mtn_siguniang_da, contour = true),
        m("kazbek", "卡兹别克山", "Kazbek", 5054, 5000, MountainRegion.CENTRAL_ASIA,
            "8 km", "3,600 m", "高加索", R.drawable.mtn_kazbek, contour = true),
        m("ararat", "阿拉拉特山", "Mount Ararat", 5137, 5000, MountainRegion.GLOBAL,
            "10 km", "4,000 m", "亚美尼亚高原", R.drawable.mtn_ararat, contour = true),
        m("kenya", "肯尼亚山", "Mount Kenya", 5199, 5000, MountainRegion.GLOBAL,
            "12 km", "4,600 m", "东非裂谷", R.drawable.mtn_kenya, contour = true),

        // ================= 5,200 =================
        m("gangshika", "岗什卡雪峰", "Gangshika", 5255, 5200, MountainRegion.CN_NW,
            "6 km", "4,500 m", QILIAN, R.drawable.mtn_gangshika, contour = true),
        m("siguniang_er", "四姑娘山二峰", "Siguniang Er Feng", 5276, 5200, MountainRegion.CN_SW,
            "4 km", "4,700 m", "邛崃山", R.drawable.mtn_siguniang_er, contour = true),
        m("siguniang_san", "四姑娘山三峰", "Siguniang San Feng", 5355, 5200, MountainRegion.CN_SW,
            "3 km", "4,700 m", "邛崃山", R.drawable.mtn_siguniang_san, contour = true),
        m("nianbao", "年保玉则", "Nianbaoyuze", 5369, 5200, MountainRegion.CN_NW,
            "7 km", "4,700 m", "巴颜喀拉", R.drawable.mtn_nianbao, contour = true),
        m("haba", "哈巴雪山", "Haba Snow Mountain", 5396, 5200, MountainRegion.CN_SW,
            "6 km", "4,600 m", HENGDUAN, R.drawable.mtn_haba, contour = true),

        // ================= 5,400 =================
        m("baima", "白马雪山", "Baima Snow Mountain", 5429, 5400, MountainRegion.CN_SW,
            "9 km", "4,700 m", HENGDUAN, R.drawable.mtn_baima, contour = true),
        m("elias", "圣伊莱亚斯山", "Mount Saint Elias", 5489, 5400, MountainRegion.NORTH_AMERICA,
            "15 km", "3,100 m", "圣伊莱亚斯", R.drawable.mtn_elias, contour = true),
        m("xuebaoding", "雪宝顶", "Xuebaoding", 5588, 5400, MountainRegion.CN_SW,
            "7 km", "4,700 m", "岷山", R.drawable.mtn_xuebaoding, contour = true),
        m("nama", "那玛峰", "Nama Peak", 5588, 5400, MountainRegion.CN_SW,
            "4 km", "4,800 m", "大雪山脉", R.drawable.mtn_nama, contour = true),
        m("yulong", "玉龙雪山", "Yulong Snow Mountain", 5596, 5400, MountainRegion.CN_SW,
            "8 km", "4,700 m", HENGDUAN, R.drawable.mtn_yulong),

        // ================= 5,600 =================
        m("damavand", "达马万德峰", "Damavand", 5610, 5600, MountainRegion.GLOBAL,
            "9 km", "4,700 m", "厄尔布尔士", R.drawable.mtn_damavand, contour = true),
        m("orizaba", "奥里萨巴峰", "Pico de Orizaba", 5636, 5600, MountainRegion.NORTH_AMERICA,
            "9 km", "4,700 m", "墨西哥火山带", R.drawable.mtn_orizaba, contour = true),
        m("elbrus", "厄尔布鲁士峰", "Elbrus", 5642, 5600, MountainRegion.CENTRAL_ASIA,
            "11 km", "3,800 m", "高加索", R.drawable.mtn_elbrus, contour = true),

        // ================= 5,800 =================
        m("altun", "阿尔金山", "Altun Shan", 5798, 5800, MountainRegion.CN_NW,
            "12 km", "4,800 m", "阿尔金山"),
        m("tuanjie", "团结峰", "Tuanjie Feng", 5827, 5800, MountainRegion.CN_NW,
            "9 km", "4,700 m", QILIAN),
        m("kilimanjaro", "乞力马扎罗山", "Kilimanjaro", 5895, 5800, MountainRegion.GLOBAL,
            "16 km", "4,800 m", "东非裂谷", R.drawable.mtn_kilimanjaro, contour = true),
        m("yangmaiyong", "央迈勇", "Yangmaiyong", 5958, 5800, MountainRegion.CN_SW,
            "5 km", "4,900 m", "沙鲁里山", R.drawable.mtn_yangmaiyong, contour = true),
        m("logan", "洛根山", "Mount Logan", 5959, 5800, MountainRegion.NORTH_AMERICA,
            "15 km", "3,300 m", "圣伊莱亚斯", R.drawable.mtn_logan, contour = true),

        // ================= 6,000 =================
        m("xiannairi", "仙乃日", "Xiannairi", 6032, 6000, MountainRegion.CN_SW,
            "6 km", "4,900 m", "沙鲁里山", R.drawable.mtn_xiannairi, contour = true),
        m("tianhaizi", "田海子山", "Tianhaizi Shan", 6078, 6000, MountainRegion.CN_SW,
            "5 km", "4,900 m", "大雪山脉", R.drawable.mtn_tianhaizi, contour = true),
        m("queer", "雀儿山", "Que'er Shan", 6168, 6000, MountainRegion.CN_SW,
            "8 km", "5,000 m", "沙鲁里山", R.drawable.mtn_queer, contour = true),
        m("yuzhu", "玉珠峰", "Yuzhu Peak", 6178, 6000, MountainRegion.CN_NW,
            "6 km", "5,000 m", KUNLUN, R.drawable.mtn_yuzhu, contour = true),
        m("denali", "德纳里峰", "Denali", 6190, 6000, MountainRegion.NORTH_AMERICA,
            "14 km", "3,500 m", "阿拉斯加山脉", R.drawable.mtn_denali, contour = true),

        // ================= 6,200 =================
        m("genie", "格聂神山", "Genyen", 6204, 6200, MountainRegion.CN_SW,
            "9 km", "5,000 m", "沙鲁里山", R.drawable.mtn_genie, contour = true),
        m("yaomei", "四姑娘山幺妹峰", "Siguniang Yaomei Feng", 6250, 6200, MountainRegion.CN_SW,
            "3 km", "4,900 m", "邛崃山", R.drawable.mtn_yaomei),
        m("chimborazo", "钦博拉索山", "Chimborazo", 6268, 6200, MountainRegion.GLOBAL,
            "12 km", "4,800 m", ANDES, R.drawable.mtn_chimborazo, contour = true),
        m("amne", "阿尼玛卿", "Amne Machin", 6282, 6200, MountainRegion.CN_NW,
            "10 km", "5,000 m", KUNLUN, R.drawable.mtn_amne, contour = true),

        // ================= 6,400 =================
        m("illimani", "伊宜马尼山", "Illimani", 6438, 6400, MountainRegion.GLOBAL,
            "8 km", "5,100 m", ANDES, R.drawable.mtn_illimani, contour = true),
        m("sajama", "萨哈马峰", "Nevado Sajama", 6542, 6400, MountainRegion.GLOBAL,
            "10 km", "5,200 m", ANDES, R.drawable.mtn_sajama, contour = true),

        // ================= 6,600 =================
        m("geladaindong", "格拉丹冬", "Geladaindong", 6621, 6600, MountainRegion.CN_NW,
            "12 km", "5,200 m", "唐古拉", R.drawable.mtn_geladaindong, contour = true),
        m("kailash", "冈仁波齐", "Mount Kailash", 6656, 6600, MountainRegion.CN_SW,
            "6 km", "5,600 m", "冈底斯", R.drawable.mtn_kailash),
        m("kawagarbo", "卡瓦格博", "Kawagarbo", 6740, 6600, MountainRegion.CN_SW,
            "8 km", "5,200 m", HENGDUAN, R.drawable.mtn_kawagarbo),
        m("huascaran", "瓦斯卡兰山", "Huascaran", 6768, 6600, MountainRegion.GLOBAL,
            "9 km", "5,100 m", ANDES, R.drawable.mtn_huascaran, contour = true),

        // ================= 6,800 =================
        m("ojos", "奥霍斯-德尔萨拉多峰", "Ojos del Salado", 6893, 6800, MountainRegion.GLOBAL,
            "7 km", "5,600 m", ANDES, R.drawable.mtn_ojos, contour = true),
        m("aconcagua", "阿空加瓜山", "Aconcagua", 6961, 6800, MountainRegion.GLOBAL,
            "10 km", "5,200 m", ANDES, R.drawable.mtn_aconcagua, contour = true),
        m("muztagh_ata", "木孜塔格峰", "Muztagh", 6973, 6800, MountainRegion.CN_NW,
            "9 km", "5,300 m", KUNLUN, R.drawable.mtn_muztagh_ata, contour = true),

        // ================= 7,000 =================
        m("khan", "汗腾格里峰", "Khan Tengri", 7010, 7000, MountainRegion.CENTRAL_ASIA,
            "6 km", "4,700 m", TIANSHAN, R.drawable.mtn_khan, contour = true),
        m("qungmogangri", "穷母岗日", "Qungmo Gangri", 7048, 7000, MountainRegion.CN_SW,
            "8 km", "5,400 m", "念青唐古拉", R.drawable.mtn_qungmogangri, contour = true),
        m("lenin", "列宁峰", "Lenin Peak", 7134, 7000, MountainRegion.CENTRAL_ASIA,
            "7 km", "4,900 m", PAMIR, R.drawable.mtn_lenin, contour = true),
        m("nyainqen", "念青唐古拉峰", "Nyainqentanglha", 7162, 7000, MountainRegion.CN_SW,
            "7 km", "5,400 m", "念青唐古拉", R.drawable.mtn_nyainqen, contour = true),

        // ================= 7,200 =================
        m("noijin", "乃钦康桑", "Noijin Kangsang", 7191, 7200, MountainRegion.CN_SW,
            "7 km", "5,400 m", HIMAL, R.drawable.mtn_noijin, contour = true),
        m("noshaq", "诺沙克峰", "Noshaq", 7492, 7400, MountainRegion.CENTRAL_ASIA,
            "7 km", "4,800 m", "兴都库什", R.drawable.mtn_noshaq, contour = true),
        m("jarabai", "加拉白垒", "Gyala Peri", 7294, 7200, MountainRegion.CN_SW,
            "7 km", "5,400 m", "念青唐古拉", R.drawable.mtn_jarabai, contour = true),
        m("chomolhari", "卓木拉日", "Chomolhari", 7326, 7200, MountainRegion.CN_SW,
            "6 km", "5,400 m", HIMAL, R.drawable.mtn_chomolhari, contour = true),

        // ================= 7,400 =================
        m("tomur", "托木尔峰", "Tomur Peak", 7443, 7400, MountainRegion.CN_NW,
            "9 km", "4,900 m", TIANSHAN, R.drawable.mtn_tomur),
        m("somoniy", "伊斯梅尔·索莫尼峰", "Ismoil Somoni Peak", 7495, 7400, MountainRegion.CENTRAL_ASIA,
            "9 km", "4,900 m", PAMIR, R.drawable.mtn_somoniy, contour = true),
        m("kulha", "库拉岗日", "Kula Kangri", 7538, 7400, MountainRegion.CN_SW,
            "8 km", "5,500 m", HIMAL, R.drawable.mtn_kulha, contour = true),
        m("muztagh", "慕士塔格峰", "Muztagh Ata", 7546, 7400, MountainRegion.CN_NW,
            "16 km", "5,000 m", KUNLUN_W, R.drawable.mtn_muztagh, contour = true),
        m("gongga", "贡嘎山", "Minya Konka", 7556, 7400, MountainRegion.CN_SW,
            "9 km", "5,000 m", HENGDUAN, R.drawable.mtn_gongga),

        // ================= 7,600 =================
        m("kongur", "公格尔峰", "Kongur Tagh", 7649, 7600, MountainRegion.CN_NW,
            "12 km", "5,100 m", KUNLUN_W, R.drawable.mtn_kongur),
        m("namunani", "纳木那尼峰", "Naimona'nyi", 7694, 7600, MountainRegion.CN_SW,
            "10 km", "5,400 m", HIMAL, R.drawable.mtn_namunani, contour = true),
        m("tirich", "蒂里奇米尔峰", "Tirich Mir", 7708, 7600, MountainRegion.CENTRAL_ASIA,
            "8 km", "4,800 m", "兴都库什", R.drawable.mtn_tirich, contour = true),

        // ================= 7,780 — 八千米之前最后一座 =================
        m("namcha", "南迦巴瓦峰", "Namcha Barwa", 7782, 7780, MountainRegion.CN_SW,
            "8 km", "5,400 m", HENGDUAN, R.drawable.mtn_namcha, contour = true),

        // ================= 8,000+ — 十四座，一座一档 =================
        m("shishapangma", "希夏邦马峰", "Shishapangma", 8027, 8000, MountainRegion.CN_SW,
            "8 km", "5,700 m", HIMAL, R.drawable.mtn_shishapangma, contour = true),
        m("gasherbrum2", "加舒尔布鲁姆Ⅱ峰", "Gasherbrum II", 8035, 8020, MountainRegion.GLOBAL,
            "6 km", "5,400 m", KARAKORAM, R.drawable.mtn_gasherbrum2, contour = true),
        m("broad", "布洛阿特峰", "Broad Peak", 8051, 8040, MountainRegion.GLOBAL,
            "6 km", "5,400 m", KARAKORAM, R.drawable.mtn_broad, contour = true),
        m("gasherbrum1", "加舒尔布鲁姆Ⅰ峰", "Gasherbrum I", 8080, 8060, MountainRegion.GLOBAL,
            "7 km", "5,400 m", KARAKORAM, R.drawable.mtn_gasherbrum1, contour = true),
        m("annapurna", "安纳普尔纳Ⅰ峰", "Annapurna I", 8091, 8080, MountainRegion.GLOBAL,
            "8 km", "5,400 m", HIMAL, R.drawable.mtn_annapurna, contour = true),
        m("nangaparbat", "南迦帕尔巴特峰", "Nanga Parbat", 8126, 8100, MountainRegion.GLOBAL,
            "9 km", "5,300 m", HIMAL, R.drawable.mtn_nangaparbat, contour = true),
        m("manaslu", "马纳斯鲁峰", "Manaslu", 8163, 8120, MountainRegion.GLOBAL,
            "7 km", "5,400 m", HIMAL, R.drawable.mtn_manaslu, contour = true),
        m("dhaulagiri", "道拉吉里峰", "Dhaulagiri", 8167, 8140, MountainRegion.GLOBAL,
            "8 km", "5,400 m", HIMAL, R.drawable.mtn_dhaulagiri, contour = true),
        m("chooyu", "卓奥友峰", "Cho Oyu", 8188, 8160, MountainRegion.CN_SW,
            "7 km", "5,700 m", HIMAL, R.drawable.mtn_chooyu),
        m("makalu", "马卡鲁峰", "Makalu", 8485, 8400, MountainRegion.GLOBAL,
            "7 km", "5,500 m", HIMAL, R.drawable.mtn_makalu, contour = true),
        m("lhotse", "洛子峰", "Lhotse", 8516, 8440, MountainRegion.GLOBAL,
            "6 km", "5,500 m", HIMAL, R.drawable.mtn_lhotse, contour = true),
        m("kangchenjunga", "干城章嘉峰", "Kangchenjunga", 8586, 8480, MountainRegion.GLOBAL,
            "10 km", "5,400 m", HIMAL, R.drawable.mtn_kangchenjunga, contour = true),
        m("k2", "乔戈里峰", "K2", 8611, 8600, MountainRegion.CN_NW,
            "10 km", "5,500 m", KARAKORAM, R.drawable.mtn_k2),
        m("everest", "珠穆朗玛峰", "Everest", 8848, 8800, MountainRegion.CN_SW,
            "12 km", "5,500 m", HIMAL, R.drawable.mtn_everest)
    )

    /** 里程碑档位（去重升序），用于「下一座」与历史列表分组。 */
    val MILESTONES: List<Int> = ALL.map { it.milestoneAlt }.distinct().sorted()

    /** 八千米级，按高度降序；索引 + 1 即「世界第几高」。 */
    private val EIGHT_THOUSAND: List<Mountain> =
        ALL.filter { it.elevation >= 8000 }.sortedByDescending { it.elevation }

    /** 八千米级里它排第几高（1 = 珠峰）。非八千米返回 0。 */
    fun rank8000(m: Mountain): Int =
        if (m.elevation < 8000) 0 else EIGHT_THOUSAND.indexOfFirst { it.id == m.id } + 1

    /** 当前海拔已越过的八千米数量。 */
    fun eightKReached(currentAlt: Int): Int =
        EIGHT_THOUSAND.count { it.milestoneAlt <= currentAlt }

    /**
     * 没有地区偏好时的默认倾向。
     *
     * 中文用户默认该先看到中国境内的山 —— 大本营第一屏给一座富士山很奇怪。
     * 只是排序倾向，不改变档位判定。
     */
    private val HOME_REGIONS = listOf(MountainRegion.CN_SW, MountainRegion.CN_NW)

    /**
     * 挑当前该展示的那一座。
     *
     * 规则：
     *  1. 先按学习海拔定位档位 —— 取「已到达的最高一档」；一档都没到，取最近的一档。
     *  2. 同一档内优先地区偏好 —— **同进度下，不同地区看到的山不同**。
     *  3. 没有偏好时优先中国境内的山，再取海拔最高的。
     */
    fun pick(currentAlt: Int, preferred: MountainRegion?): Mountain {
        val reachedTiers = ALL.filter { it.milestoneAlt <= currentAlt }
        val pool = if (reachedTiers.isNotEmpty()) {
            val top = reachedTiers.maxOf { it.milestoneAlt }
            ALL.filter { it.milestoneAlt == top }
        } else {
            val first = ALL.minOf { it.milestoneAlt }
            ALL.filter { it.milestoneAlt == first }
        }
        val preferredPool = pool.filter { it.region == preferred }
        if (preferredPool.isNotEmpty()) return preferredPool.maxByOrNull { it.elevation }!!

        val homePool = pool.filter { it.region in HOME_REGIONS }
        if (homePool.isNotEmpty()) return homePool.maxByOrNull { it.elevation }!!

        return pool.maxByOrNull { it.elevation } ?: ALL.first()
    }

    /** 已解锁（里程碑已到达）的山，按里程碑升序。 */
    fun unlocked(currentAlt: Int): List<Mountain> =
        ALL.filter { it.milestoneAlt <= currentAlt }.sortedBy { it.milestoneAlt }

    /** 全部山，按里程碑升序；未解锁的排在已解锁之后。 */
    fun history(currentAlt: Int): List<Mountain> {
        val unlocked = unlocked(currentAlt)
        val locked = ALL.filter { it.milestoneAlt > currentAlt }.sortedBy { it.milestoneAlt }
        return unlocked + locked
    }
}

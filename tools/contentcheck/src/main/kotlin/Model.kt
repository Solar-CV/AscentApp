package com.ascent.climb.data

/** 题型。选择题只是其中一种，7000 米以上会大量使用后面几种。 */
enum class QType {
    /** 单选 */
    Single,
    /** 多选 */
    Multi,
    /** 排序：把打乱的步骤排回正确顺序 */
    Order,
    /** 配对：左右两列一一对应 */
    Match,
    /** 填空：关键数据记忆（剂量、温度、比例） */
    Fill,
    /** 计算题：输入数值，按容差判定（如氧气续航、温差推算） */
    Calc,
    /** 热点题：在图上点出正确位置（云图、地形图判读） */
    Hotspot,
    /** 情景题：一个情境下连续追问，全部答对才算正确（用于高阶决策训练） */
    Scenario
}

/** 热点题的可点区域，坐标已归一化到 0..1（相对图片左上角）。 */
data class HotspotRegion(
    val x: Float,
    val y: Float,
    /** 半径（相对于图片宽度） */
    val r: Float,
    /** 是否为正确答案 */
    val correct: Boolean,
    /** 可选标签，答完后显示 */
    val label: String = ""
)

/**
 * 一道题。
 *
 * 不同题型使用不同字段：
 *  - Single：options + correct
 *  - Multi ：options + correctSet
 *  - Order ：options（打乱后的顺序）+ correctOrder（正确顺序的索引序列）
 *  - Match ：pairsLeft / pairsRight，同下标为一对
 *  - Fill  ：answers（可接受答案，按顺序对应题干中的空）
 */
data class Question(
    val prompt: String,
    val context: String? = null,
    /** 单选 / 多选 / 排序使用；填空与配对题留空 */
    val options: List<String> = emptyList(),
    /** 单选的正确项下标；其他题型为 -1 */
    val correct: Int = -1,
    val explanation: String,
    /** 安全关键题加权，且答错必须理解后才能继续 */
    val safetyCritical: Boolean = false,
    /** 由前面单元抽取而来的复习题 */
    val review: Boolean = false,
    /** 知识来源标注，例如「《高山探险》p.196」 */
    val source: String? = null,

    // ---- 扩展题型 ----
    val type: QType = QType.Single,
    /** 多选：正确选项索引集合 */
    val correctSet: List<Int> = emptyList(),
    /** 排序：正确顺序（数组元素为 options 的索引） */
    val correctOrder: List<Int> = emptyList(),
    /** 配对：左列 */
    val pairsLeft: List<String> = emptyList(),
    /** 配对：右列，与左列同下标为一对 */
    val pairsRight: List<String> = emptyList(),
    /** 填空：可接受答案 */
    val answers: List<String> = emptyList(),
    /** 题图文件名（放在 assets/bank/img/ 下），用于云图识别等图形题 */
    val image: String? = null,

    // ---- 计算题 ----
    /** 允许的误差（绝对值）。为 0 时按精确值判定 */
    val tolerance: Double = 0.0,
    /** 单位提示，如 “L/min”“℃” */
    val unit: String? = null,

    // ---- 热点题 ----
    /** 可点区域；用户点到 correct=true 的区域即为正确 */
    val hotspots: List<HotspotRegion> = emptyList(),

    // ---- 情景题 ----
    /**
     * 子问题（仅支持 Single / Multi）。
     * 情景题本身用 prompt 做标题、context 做情境背景，不参与选答案。
     */
    val steps: List<Question> = emptyList(),

    /**
     * 稳定标识，形如 `l3#4`（课程 id + 该课内的序号），由 BundledBank 统一赋值。
     * 内容层多语言的 key 依赖它（`q.<id>.p` / `.o<n>` / `.e`）；
     * 复习题是原题的副本，会继承同一个 id，译文因此自动复用。
     */
    val id: String = ""
)

/** 一门课程 = 路线上的一个节点。 */
data class Lesson(
    val id: String,
    val title: String,
    val code: String,
    val icon: String,
    /** 技能要点（先读后测） */
    val brief: List<String>,
    val questions: List<Question>,
    /** 需要教练现场评估的实操技能 */
    val needsCoach: Boolean = false
)

/** 目标山峰。 */
data class Peak(
    val name: String,
    val altitude: Int,
    val location: String,
    /** 类型：徒步型 / 技术型 / 混合 */
    val style: String = "",
    /** 难度或备注 */
    val note: String = ""
)

/**
 * 一个单元 = 一个海拔等级。
 * 单元下挂目标山峰，学员可以按峰准备。
 */
data class Unit(
    val id: String,
    val index: Int,
    val code: String,
    val title: String,
    val altitude: Int,
    val lessons: List<Lesson>,
    /** 所属课程轨道，例如 climb / dive */
    val track: String = CLIMB_TRACK,
    /** 单元简介 */
    val subtitle: String = "",
    /** 本单元的目标山峰 */
    val peaks: List<Peak> = emptyList()
)

/** 课程轨道。用于把「高山攀登」与后续的「开放水域潜水」等并列。 */
data class Track(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val units: List<Unit>
)

const val CLIMB_TRACK = "climb"

/** 预留的扩展轨道标识 —— 数据层已支持，内容待接入。 */
const val DIVE_TRACK = "dive"
const val TREK_TRACK = "trek"
const val SKI_TRACK = "ski"

// ============================================================
// 技能书
// ============================================================

data class HandbookSection(
    val heading: String,
    val body: String
)

data class HandbookArticle(
    val id: String,
    val title: String,
    val code: String,
    val icon: String,
    val category: String,
    val summary: String,
    val sections: List<HandbookSection>,
    val sources: List<String>,
    /**
     * 这篇在哪条课程大类下可见。
     *
     * 默认只在「高山攀登」里出现：那批内容（冰镐、冰爪、攀冰、绳结、冰裂缝）对徒步用户没有意义。
     * 通用篇（高原病、失温、导航、营地、领导力等）显式标成两条轨道共享。
     */
    val tracks: List<String> = listOf(CLIMB_TRACK)
)

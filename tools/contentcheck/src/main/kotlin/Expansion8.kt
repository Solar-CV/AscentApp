package com.ascent.climb.data

/**
 * 内容扩充包 · 第八批
 *
 * 本批用于接入两种新题型：
 *  - Hotspot 热点题（图上点击）—— 题图见 assets/bank/img/，由 tools/qbank/gen-demo-image.mjs 生成
 *  - Calc 计算题（数值 + 容差）—— 用于氧气续航、温差推算等需要真算的应用题
 */
val EXTRA_QUESTIONS_8: Map<String, List<Question>> = mapOf(

    // ---------------- m01 高山环境科学：温差推算 ----------------
    "m01" to listOf(
        Question(
            prompt = "从海拔 3,000 米的大本营上到 5,000 米的营地，按平均递减率计算，气温大约下降多少摄氏度？",
            type = QType.Calc,
            answers = listOf("13"),
            tolerance = 0.5,
            unit = "℃",
            explanation = "13℃。算法：海拔差 2,000 米，每 1,000 米降 6.5℃，即 2 × 6.5 = 13℃。这个推算在行前非常实用——它直接决定你要带多厚的保暖层与睡袋。",
            source = "《高山探险》高山环境章节"
        )
    ),

    // ---------------- l18 雪崩基础与识别：热点题 ----------------
    "l18" to listOf(
        Question(
            prompt = "图中是同一座山的三段坡面。点击雪崩风险最高的那一段。",
            type = QType.Hotspot,
            image = "demo-slope.png",
            hotspots = listOf(
                HotspotRegion(0.481f, 0.592f, 0.13f, true, "约 40° 的坡面"),
                HotspotRegion(0.188f, 0.808f, 0.11f, false, "约 16° 的缓坡"),
                HotspotRegion(0.700f, 0.250f, 0.11f, false, "约 60° 的陡坡")
            ),
            explanation = "雪崩最集中的坡度是 34°～45°。左侧缓坡（约 16°）太平缓，难以形成板状雪崩；右侧陡坡（约 60°）太陡，积雪不易堆积成厚雪板。只有中间约 40° 的坡面落在「既能积雪、又能滑动」的危险窗口里——而这恰好是登山路线最常经过的角度，这就是它最容易被低估的原因。",
            safetyCritical = true,
            source = "雪崩坡度统计资料"
        )
    ),

    // ---------------- m20 氧气系统与补给：续航与总量计算 ----------------
    "m20" to listOf(
        Question(
            prompt = "一瓶可用氧气量按 1,200 升计算。若按 3 L/min 的流量使用，大约可以支撑多少分钟？",
            type = QType.Calc,
            answers = listOf("400"),
            tolerance = 10.0,
            unit = "分钟",
            explanation = "约 400 分钟（约 6.7 小时）。算法：1200 ÷ 3 = 400。这类计算在冲顶日意义很直接——它决定了你能在 8000 米停留多久，也直接决定了折返时间该定在几点。",
            source = "高海拔氧气系统资料"
        ),
        Question(
            prompt = "冲顶计划：上行 5 小时、下行 3 小时，流量按 2 L/min，另需预留 60 分钟应急用量。所需氧气总量约为多少升？",
            type = QType.Calc,
            answers = listOf("1080"),
            tolerance = 20.0,
            unit = "升",
            explanation = "1080 升。算法：（5 + 3）小时 × 60 分钟 × 2 L/min = 960 升，再加应急 60 × 2 = 120 升，合计 1080 升。把「去程 + 回程 + 应急」三部分分开计算，是氧气计划最基本的纪律——只算“上去够不够”是最常见的错误。",
            source = "高海拔氧气系统资料"
        )
    )
)

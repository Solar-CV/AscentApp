package com.ascent.climb.data

/**
 * 内容扩充包 · 第十二批：专业气象界面（mountain-forecast.com 桌面版）
 *
 * 与手机 App 最大的差别：它把**同一时刻不同高度的天气并列展示**，
 * 所以不需要从海平面数据外推，可以直接读自己所在高度的那一行。
 */
val EXTRA_QUESTIONS_12: Map<String, List<Question>> = mapOf(

    "m15" to listOf(

        Question(
            prompt = "这张专业界面把 8000 / 6000 / 4000 / 2000 米分成四行并列展示温度。这样设计的主要价值是什么？",
            type = QType.Single,
            image = "wx-mtnforecast.png",
            options = listOf(
                "可以直接读出自己所在高度的预报，不必从低海拔数据外推",
                "让页面看起来更丰富",
                "表示这四行之间没有关联",
                "只是为了方便印刷成纸质表格"
            ),
            correct = 0,
            explanation = "普通天气预报给的是某一个地点的数据，通常来自最近的城市或机场。你要自己按递减率推算山上的情况，这一步既容易算错，也丢掉了地形细节。\n专业登山气象服务反过来做：先把山按高度切成层，再分别给出每层的预报。你只要找到「明天自己会待在哪个高度」那一行，直接读。判断一份气象服务够不够用，这也是个简单标准：它有没有按高度分层。",
            source = "mountain-forecast.com 专业预报界面"
        ),

        Question(
            prompt = "图中「云底高度（米）」一行在多数时段显示为空（—）。正确的理解是？",
            type = QType.Single,
            image = "wx-mtnforecast.png",
            options = listOf(
                "该时段云底高度为零，即云层贴地",
                "该时段没有提供云底数据，不能当作「云底为 0」",
                "表示该时段完全没有云",
                "表示云底高度超过了山峰高度"
            ),
            correct = 1,
            explanation = "气象表里的「空」和「零」是两件完全不同的事。空值表示这个项目在这一时段没有数据，可能是不覆盖，也可能是给不出有意义的估计，它不等于「值为零」。\n把空值当成零，会得出「云层贴地、能见度极差」这种跟事实无关的结论。读任何气象表，先分清这两者。这是看预报最容易出错、也最少被提醒的一个细节。",
            source = "mountain-forecast.com 专业预报界面"
        ),

        Question(
            prompt = "这份界面的风速行里，除了数字还带有箭头符号。箭头最可能表示什么？",
            type = QType.Single,
            image = "wx-mtnforecast.png",
            options = listOf(
                "风速的变化趋势",
                "风的方向",
                "降水概率",
                "气温升降"
            ),
            correct = 1,
            explanation = "风速和风向在高海拔要一起看。同样是 30 km/h，迎风面和背风面的体感完全不同，对雪层的影响也两样：风会把雪搬到背风坡，堆成雪板。这是判断雪崩时最关键的信息之一。\n只看风速数字、不看风向，等于丢了一半信息。专业界面把箭头放在数字旁边，就是因为这两者不能分开读。",
            safetyCritical = true,
            source = "mountain-forecast.com 专业预报界面"
        )
    )
)

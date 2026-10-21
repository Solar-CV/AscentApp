package com.ascent.climb.data

/**
 * 技能书 · 上篇：高海拔医学与冰雪技术
 * 每篇均标注内容来源，便于教练审核与后续修订。
 */
val HANDBOOK_A = listOf(

    HandbookArticle(
        id = "h1",
        tracks = listOf(CLIMB_TRACK, TREK_TRACK),
        title = "急性高原病完全指南",
        code = "AMS / HACE / HAPE",
        icon = "\uD83E\uDD15",
        category = "高海拔医学",
        summary = "高海拔最常见的三类病：AMS 通常可逆，HACE 和 HAPE 可能在几小时内致命。识别它们，主要看症状什么时候出现、恶化有多快。",
        sections = listOf(
            HandbookSection(
                "AMS（急性高原病）",
                "诊断采用 Lake Louise 标准：近期上升海拔后出现头痛，并至少伴随恶心或呕吐、头晕、疲乏无力、睡眠障碍中的一项。症状通常在抵达高海拔后 6–12 小时出现；海拔越高风险越大，2,500 米以下少见，2,500–4,300 米人群中约 25%–43% 会发病。"
            ),
            HandbookSection(
                "HACE（高原脑水肿）",
                "核心体征是共济失调与意识改变：脚跟接脚尖走直线失败、步态不稳、嗜睡、行为异常、出现幻觉。这是内科急症，可在数小时内致死，必须立即下降并启动医疗支援。"
            ),
            HandbookSection(
                "HAPE（高原肺水肿）",
                "典型表现是静息状态下仍然呼吸困难、活动能力急剧下降、干咳逐渐转为带泡沫痰、口唇发绀。HAPE 是高原病最主要的死因，且可能进展极快。"
            ),
            HandbookSection(
                "处置与黄金法则",
                "第一，在海拔感到不适，先按高原病处理；第二，出现症状绝不继续上升；第三，症状加重立即下降；第四，严重时立即下降并求医。下降是唯一能确定性逆转病程的措施，吸氧、高压氧袋与药物都是争取时间的辅助手段。药物（如地塞米松成人常用 4mg 每 6 小时、硝苯地平或磷酸二酯酶抑制剂用于 HAPE）须由医生指导使用。"
            )
        ),
        sources = listOf(
            "Wilderness Medical Society 临床实践指南：急性高原病的预防、诊断与治疗（2024 更新版）",
            "CDC Yellow Book：High-Altitude Travel and Altitude Illness",
            "StatPearls：Acute Mountain Sickness（流行病学数据）"
        )
    ),

    HandbookArticle(
        id = "h2",
        tracks = listOf(CLIMB_TRACK, TREK_TRACK),
        title = "高原适应节奏与预防用药",
        code = "ACCLIMATIZATION",
        icon = "\uD83D\uDCC8",
        category = "高海拔医学",
        summary = "适应是身体对低氧的代偿，需要时间。控制上升速度，比吃任何药都更根本。",
        sections = listOf(
            HandbookSection(
                "上升速度的两条硬指标",
                "一是避免在一天之内直接睡到 2,750 米以上（从低海拔出发的情况）；二是海拔超过 3,000 米后，把睡眠海拔的日升幅控制在 500 米以内，并大约每升高 1,000 米安排一个休息日。"
            ),
            HandbookSection(
                "爬高睡低",
                "白天上升到更高的海拔活动与训练，夜间返回较低海拔睡眠。这种组合能持续给身体低氧刺激，同时保证睡眠质量——而睡眠本身就是适应效率的关键变量。"
            ),
            HandbookSection(
                "预防性用药",
                "乙酰唑胺（Acetazolamide）是常用的预防药物，通常从上升前一天开始服用，成人常用量 125mg 每日两次；在上升当天开始服用仍有部分获益。它属于处方药，禁忌与副作用须由医生评估。"
            ),
            HandbookSection(
                "非药物手段",
                "充足饮水、以碳水化合物为主食、避免酒精与镇静类药物、保持规律作息。酒精与镇静剂会抑制呼吸驱动，显著加重夜间低氧。"
            )
        ),
        sources = listOf(
            "Wilderness Medical Society：急性高原病指南 2024 更新版",
            "CDC Yellow Book：高海拔旅行健康建议"
        )
    ),

    HandbookArticle(
        id = "h3",
        tracks = listOf(CLIMB_TRACK, TREK_TRACK),
        title = "失温：分级与现场处置",
        code = "HYPOTHERMIA",
        icon = "\uD83C\uDF28\uFE0F",
        category = "高海拔医学",
        summary = "失温是核心体温意外下降。最危险的时刻往往不是最冷的时候，而是颤抖停止的那一刻。",
        sections = listOf(
            HandbookSection(
                "三级识别",
                "轻度：持续颤抖、意识清醒、能自主行动。中度：颤抖停止、意识模糊或言语不清、动作笨拙。重度：昏迷、心跳与呼吸微弱甚至难以测出。"
            ),
            HandbookSection(
                "关键误判点",
                "「颤抖停止」常被误读为好转，实际上是产热机制衰竭的标志，意味着正从中度转向重度。现场的判断错误多数发生在这里。"
            ),
            HandbookSection(
                "处置原则",
                "第一，脱离风寒环境并隔离地面；第二，换掉湿衣物，包裹保温；第三，中度以上避免粗暴搬动与让四肢剧烈活动——低温心肌极易被诱发心律失常；第四，优先复温躯干核心，不要先暖四肢，以免冷血回流核心。"
            ),
            HandbookSection(
                "不要做的事",
                "不要给中度以上失温者饮酒或咖啡因，不要让重度失温者自行走动，不要用暴力搓揉四肢。重度失温是医疗急症，需尽快转运。"
            )
        ),
        sources = listOf(
            "野外医学通用处置原则（Wilderness Medicine 教材体系）",
            "American Family Physician：Hypothermia and Cold Weather Injuries"
        )
    ),

    HandbookArticle(
        id = "h4",
        tracks = listOf(CLIMB_TRACK, TREK_TRACK),
        title = "冻伤：识别、复温与决策",
        code = "FROSTBITE",
        icon = "\uD83E\uDDCA",
        category = "高海拔医学",
        summary = "冻伤是组织结冰造成的损伤。现场最难的往往不是怎么复温，而是判断该不该复温。",
        sections = listOf(
            HandbookSection(
                "识别",
                "浅表冻伤：皮肤苍白、蜡状，按压仍有弹性，伴随刺痛或灼痛，复温后可用。深层冻伤：皮肤坚硬如木、无感觉、可能出现血疱，复温后肿胀变黑，属于严重损伤。"
            ),
            HandbookSection(
                "标准复温",
                "用 37–40℃ 的温水浸泡约 30 分钟，直到皮肤变红、变软。绝不使用雪搓、火烤、热水袋或任何直接热源接触——冻伤部位感觉丧失，极易被烫伤。复温过程会非常疼痛，需要止痛处理。"
            ),
            HandbookSection(
                "复温的决策陷阱",
                "如果复温后仍可能再次冻结，就不要现场复温。复温—再冻结的循环会造成远比持续冻伤更严重的组织破坏。此时正确做法是保护患处、避免摩擦、尽快下撤到能持续保暖的环境。"
            ),
            HandbookSection(
                "后续处理",
                "水疱不要刺破，用无菌敷料松散包扎，让患处保持温暖干燥。避免患处承重。冻伤后长期后遗症包括怕冷、感觉异常与出汗异常，可能需要数月恢复。"
            )
        ),
        sources = listOf(
            "野外医学通用处置原则",
            "Cleveland Clinic / Red Cross 冻伤处置指引"
        )
    ),

    HandbookArticle(
        id = "h5",
        title = "冰镐与自我制动详解",
        code = "SELF-ARREST",
        icon = "\u26CF\uFE0F",
        category = "冰雪技术",
        summary = "自我制动能在几秒内止住滑坠，是登山里最救命的技能之一，也是读书看视频学不会的。",
        sections = listOf(
            HandbookSection(
                "持镐与行进",
                "行进时冰镐握在上坡一侧的手，镐尖朝后、镐头朝前，形成「拐杖式（piolet canne）」支撑。这个姿势的价值在于：失稳时无需换手就能直接转入制动。"
            ),
            HandbookSection(
                "制动姿势的四个要点",
                "一、迅速转为俯卧、面朝坡面；二、冰镐沿胸前斜握横跨身体；三、一只手压住镐头靠近镐尖处，另一只手握住镐柄；四、镐尖压入雪面并靠身体重量下压，同时双脚抬离雪面、脚尖朝上。"
            ),
            HandbookSection(
                "为什么脚必须抬起来",
                "这是新手最常做错的地方。脚一旦接触雪面，就会分担掉本该压在冰镐上的体重，制动效率立刻下降，还会造成踝与膝的扭伤。制动靠的是体重经由镐尖作用于雪面。"
            ),
            HandbookSection(
                "头朝下的滑坠",
                "此时镐尖角度无法有效入雪，且头部朝下的撞击风险极高。第一动作是用腰腹发力翻身，把姿态转为俯卧、脚朝下，再建立制动。"
            ),
            HandbookSection(
                "怎么练",
                "必须在坡缓、下方开阔、雪质柔软且有教练保护的场地上反复练习，直至形成本能反应。注意雪质差异：软雪容易制动，硬雪与冰面的制动距离会长得多。"
            )
        ),
        sources = listOf(
            "American Alpine Institute：Self-Arrest Techniques",
            "REI Expert Advice：How to Use an Ice Axe for Mountaineering",
            "British Mountaineering Council：Winter Essentials（自制动）"
        )
    ),

    HandbookArticle(
        id = "h6",
        title = "冰爪技术：三种体系与配套标准",
        code = "CRAMPON",
        icon = "\u2699\uFE0F",
        category = "冰雪技术",
        summary = "法式省力，德式适合陡坡，混合式用在过渡地形。技术选对了，同一个坡的体力消耗能差出一倍。",
        sections = listOf(
            HandbookSection(
                "法式（平足法 / flat-footing）",
                "让冰爪垂直于地面的所有点同时贴合坡面，脚掌放平不踢前齿。受力分散到全身与大腿，因此最省力，适合低到中等角度与硬雪地形，也是长时间雪坡行进的主力技术。"
            ),
            HandbookSection(
                "德式（前齿法 / front-pointing）",
                "前齿垂直踢入冰面，靠小腿持续发力。适合 45° 以上陡坡与冰壁，但极其消耗体力，无法长时间维持。它把体重集中在两三个前齿上。"
            ),
            HandbookSection(
                "混合式（American technique）",
                "一只脚平贴坡面做支撑，另一只脚用前齿推进，两脚交替使用。用于中等坡度到陡坡之间的过渡地形，是法式与德式之间的桥梁。"
            ),
            HandbookSection(
                "横切与鸭步",
                "横切雪坡时用「鸭步」：脚跟并拢、脚尖外八，让整副冰爪贴合坡面，避免重量只压在内侧几颗齿上。重心要略向坡内倾斜。"
            ),
            HandbookSection(
                "靴爪配套标准（安全底线）",
                "B1 软底靴配 C1 全绑带式冰爪；B2 半硬靴配 C2 前绑后卡式；B3 全硬靴配 C3 全卡式。把全卡式冰爪装在软底靴上，靴体会变形导致卡扣失去咬合，冰爪可能在陡坡上脱落。"
            )
        ),
        sources = listOf(
            "American Alpine Institute：French Technique / Crampon Technique for Ice Climbing",
            "American Alpine Club：The Principles of Snow Climbing",
            "REI Expert Advice：How to Use Crampons"
        )
    )
)

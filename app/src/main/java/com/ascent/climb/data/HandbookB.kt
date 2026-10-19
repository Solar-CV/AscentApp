package com.ascent.climb.data

/**
 * 技能书 · 下篇：攀登技术、绳索系统与风险判断
 */
val HANDBOOK_B = listOf(

    HandbookArticle(
        id = "h7",
        title = "攀冰技术与 WI 难度分级",
        code = "ICE CLIMB / WI",
        icon = "\uD83E\uDDD7",
        category = "攀冰技术",
        summary = "攀冰是在冰壁上攀爬的技术。它比攀岩更吃工具、冰况和当天的身体状态。",
        sections = listOf(
            HandbookSection(
                "挥镐",
                "靠手腕「甩」而不是大臂「砸」。镐尖垂直入冰，听到清脆的一声「咔」表示咬合良好；闷响通常意味着角度不好或冰质松散。挥镐次数要克制——冰壁被打碎后会形成难以受力的剥落面。"
            ),
            HandbookSection(
                "踢爪与三点固定",
                "屈膝抬脚，让前齿垂直踢入冰面。先稳定双脚，再挥镐，始终保留三个可靠接触点后才移动第四个点。节奏稳定比动作幅度重要。"
            ),
            HandbookSection(
                "WI 水冰分级",
                "WI1：低角度冰面，可徒步通过，不需要工具。WI2：约 60° 的持续冰面并有鼓包，保护良好。WI3：持续 60°–75°，通常保护良好但较费力。WI4：接近垂直并含短段垂直，保护位置可能不理想。WI5：持续垂直，保护较差且强度很大。WI6：持续垂直并含外悬段，保护极差。"
            ),
            HandbookSection(
                "为什么冰况比等级更重要",
                "同一路线在不同温度、日照与季节下难度差异巨大：早晨的硬冰需要更精准的入镐，中午的湿软冰则容易形成「烂冰」而无法受力。等级只是参考，冰况要现场判断。"
            ),
            HandbookSection(
                "安全边界",
                "攀冰必须在有经验的保护者看护下进行，并具备可靠的保护站与撤退方案。坠落对冰壁攀登者的冲击远大于运动攀岩，冰锥在受力时可能崩出——这是攀冰与攀岩最本质的差别。"
            )
        ),
        sources = listOf(
            "UIAA：The Scales of Difficulty in Climbing（WI 分级定义）",
            "Alpinist：Climbing Grade Comparison Chart",
            "Alpine Institute：Crampon Technique for Ice Climbing"
        )
    ),

    HandbookArticle(
        id = "h8",
        title = "绳结系统速查",
        code = "KNOTS",
        icon = "\uD83E\uDEA2",
        category = "绳索技术",
        summary = "五个结就够覆盖冰川行进和基础保护的大部分场景。前提是每个都能不看手打出来，并且会检查。",
        sections = listOf(
            HandbookSection(
                "回穿 8 字结（Figure-8 follow-through）",
                "连接安全带的标准结。结构直观、便于他人目视检查、受力后仍相对容易解开。这是冰川与岩雪路线的默认连接方式。"
            ),
            HandbookSection(
                "克氏结（Clove hitch）",
                "把绳子连接到主锁上，可单手推拉快速调节长度。用于保护站的自我固定（self-anchor），让攀登者能在保护站上自由调整站位。"
            ),
            HandbookSection(
                "双渔人结（Double fisherman's）",
                "用于连接两根绳子，或把辅绳做成闭合的抓结环。特点是极其稳定、不易松脱，但受力后较难解开。"
            ),
            HandbookSection(
                "抓结 / 普鲁士结（Prusik）",
                "摩擦结，受力时抓住绳索，不受力时可沿绳推动。用于上升、备份与机械优势系统。可靠性取决于绳径搭配与缠绕圈数，必须实地验证过再依赖。"
            ),
            HandbookSection(
                "检查习惯",
                "每个结完成后留足够尾端（一般不少于 10 倍绳径），并与同伴互相检查。绳结是保护系统中唯一由手工完成、无出厂检验的环节。"
            )
        ),
        sources = listOf(
            "AMGA / NOLS 绳索系统教学框架",
            "American Alpine Club：The Principles of Snow Climbing"
        )
    ),

    HandbookArticle(
        id = "h9",
        title = "保护站原则 SRENE / EARNEST",
        code = "ANCHORS",
        icon = "\u2693",
        category = "绳索技术",
        summary = "保护站的设计目标只有一个：容错。任何一个点失效，系统都不该垮。",
        sections = listOf(
            HandbookSection(
                "SRENE 四原则",
                "Solid（牢固）：每个点的强度足够承载预期负荷。Redundant（冗余）：多个独立保护点。Equalized（均衡受力）：负荷合理分配，不依赖单点。No Extension（不延展）：某点失效时不产生冲击性延展。"
            ),
            HandbookSection(
                "EARNEST 扩展版",
                "在 SRENE 基础上补充：Efficient（高效，操作不繁琐）、Anchored（固定可靠）、Strong（强度足够）、Timely（快速建立，避免长时间暴露）。它更贴近真实场景的综合评估。"
            ),
            HandbookSection(
                "为什么 No Extension 很重要",
                "如果保护点之间留有松余，某点崩脱时其余点会承受突然的冲击载荷——冲击力远大于静载。预先均衡收紧，是为了把这个冲击从系统里消掉。"
            ),
            HandbookSection(
                "不同介质的不同做法",
                "雪地可用雪锥、死挂（deadman）、T 型雪槽，但雪地保护点的可靠性高度依赖雪质与埋设质量。冰面使用冰锥，需注意冰质是否蜂窝化或有空腔。岩石使用岩塞与岩钉。永远不要用别的介质的直觉去判断当前介质。"
            )
        ),
        sources = listOf(
            "保护站评估通用原则（SRENE / EARNEST 框架）",
            "美国阿尔卑斯协会雪坡攀登原理"
        )
    ),

    HandbookArticle(
        id = "h10",
        title = "冰裂缝救援系统",
        code = "CREVASSE RESCUE",
        icon = "\uD83D\uDD73\uFE0F",
        category = "绳索技术",
        summary = "冰川救援的标准流程：制动 → 建站 → 转荷 → 拉升。四步之间不能颠倒。",
        sections = listOf(
            HandbookSection(
                "第一步：制动",
                "坠落发生时，坠落者的体重会瞬间通过绳索全部压到队友身上。队友必须立即转入自制动姿势稳住自己。这是整个救援链条的地基。"
            ),
            HandbookSection(
                "第二步：建站",
                "在自制动的同时，用雪锥、冰镐埋设雪锚或 T 型雪槽建立独立保护站，遵循 SRENE 原则。注意：在建立保护站之前，你不能松开制动。"
            ),
            HandbookSection(
                "第三步：转荷",
                "把坠落者的体重从你的身体转移到保护站上。常用的做法是用抓结把受力绳固定在保护站，或用克氏结调整。转荷完成后你才获得自由操作的能力。"
            ),
            HandbookSection(
                "第四步：拉升",
                "使用 3:1 的 Z 系统（Z-pulley / Z-drag）：在受力绳上装抓结并加滑轮，配合保护站形成三倍机械优势。结构简单、器材少、搭建快，是冰川救援的标准系统。"
            ),
            HandbookSection(
                "坠落者的配合",
                "保持冷静，避免绳索继续切入裂缝边缘的雪檐；尝试使用抓结自我上升；与上方保持沟通。团队事先必须演练过整套流程，临场才能执行。"
            )
        ),
        sources = listOf(
            "Alpine Savvy：Modern Crevasse Rescue Techniques",
            "Cascade Mountain Ascents：Crevasse Rescue 教学资料",
            "REI Expert Advice：Crevasse Rescue Skills"
        )
    ),

    HandbookArticle(
        id = "h11",
        title = "雪崩风险判断",
        code = "AVALANCHE",
        icon = "\uD83C\uDFD4\uFE0F",
        category = "风险判断",
        summary = "雪崩风险来自三样东西：雪层稳定性、地形坡度、人的决策。前两样改不了，第三样可以。",
        sections = listOf(
            HandbookSection(
                "坡度：最强的单一因子",
                "约 96% 的雪崩发生在 30°–50° 的坡面上，其中大多数集中在 34°–45°。低于约 30° 太平缓，难以形成板状雪崩；高于约 50° 雪不易堆积成厚雪板。这意味着最危险的往往是那些「看起来不太陡」的坡。"
            ),
            HandbookSection(
                "危险等级 1–5",
                "低 / 中等 / 相当危险 / 高 / 极高。从统计上看，事故最集中在等级 3「相当危险」——条件看起来尚可，地形看起来可以通过，于是大量人进入雪崩地形。等级 4、5 时人们普遍回避，事故占比反而下降。"
            ),
            HandbookSection(
                "三件套与行进守则",
                "信标（收发器）、探杆、雪铲必须随身并会熟练使用。通过雪崩地形时拉开间距、一人一人通过、指定位于安全位置的观察员、提前规划撤离路线，避免全队被埋。"
            ),
            HandbookSection(
                "决策习惯",
                "出发前查看当地雪崩预报与近期天气；行进中持续观察雪面裂纹、塌陷声（whumpf）、新雪堆积等信号；一旦产生明确疑虑，就选择更保守的路线。在雪崩风险上，习惯比知识更能救命。"
            )
        ),
        sources = listOf(
            "avalanche.org — Avalanche Encyclopedia：Slope Angle（96% / 34°–45° 统计）",
            "各区域雪崩中心发布的危险等级体系（1–5）"
        )
    ),

    HandbookArticle(
        id = "h12",
        title = "高山装备配套系统",
        code = "GEAR SYSTEM",
        icon = "\uD83E\uDDF0",
        category = "装备知识",
        summary = "高山装备不是一张清单，是一套互相牵制的系统。靴子决定冰爪，冰爪决定冰镐，冰镐又决定你能走什么地形。",
        sections = listOf(
            HandbookSection(
                "冰镐：B 型与 T 型",
                "B 型（基础型）镐柄较直，用于雪坡行走与自我制动，强度足以承受行走与制动负荷。T 型（技术型）镐柄可插入冰面并承受拉力，用于陡峭冰壁。用 B 型镐去做 T 型镐的事，是明确的误用。"
            ),
            HandbookSection(
                "冰爪：C1 / C2 / C3",
                "C1 全绑带式，适配软底靴；C2 前绑后卡式，适配半硬靴；C3 全卡式，适配全硬靴。档位越高、刚性越好，但对靴体的要求越高。不匹配的组合会导致冰爪脱落。"
            ),
            HandbookSection(
                "高山靴：单层与双层",
                "单层靴轻便灵活，适合中低海拔与攀冰。双层靴的内靴可取出，夜间贴身或放进睡袋烘干，避免内靴结冰——这是多日高海拔行程的关键优势。"
            ),
            HandbookSection(
                "容易被忽略的三样东西",
                "雪镜（UV400）：雪面反射紫外线，可致雪盲；高倍防晒：高海拔紫外线强度显著上升，晒伤非常常见；备用保暖层与厚手套：必须密封防潮，潮湿是失温的前置条件。"
            ),
            HandbookSection(
                "检查习惯",
                "每次出发前检查冰爪卡扣、冰镐镐尖与镐柄、安全带缝线、绳索磨损、头盔裂纹。装备的失效往往发生在最不该发生的时刻，而检查成本几乎为零。"
            )
        ),
        sources = listOf(
            "UIAA / CE 装备标准体系（EN 13089 冰镐、EN 893 冰爪、UIAA 105 安全带、EN 12492 头盔）",
            "REI Expert Advice：高山装备选择指南"
        )
    )
)

package com.ascent.climb.data

/**
 * 营地 3 · C2 绳索与保护
 * 内容依据：AMGA / NOLS 绳索系统教学框架、美国阿尔卑斯协会雪坡攀登原理、
 * 保护站评估通用原则（SRENE / EARNEST）、冰川救援 Z 系统标准流程整理。
 */
val UNIT_3 = Unit(
    id = "u3",
    index = 3,
    code = "CAMP 2",
    title = "C2 · 绳索与保护",
    altitude = 4200,
    lessons = listOf(

        Lesson(
            id = "l9",
            title = "基础绳结",
            code = "KNOTS",
            icon = "\uD83E\uDEA2",
            brief = listOf(
                "回穿 8 字结（figure-8 follow-through）：连接安全带最常用的结。容易目视检查、受力后仍可解开，是冰川与岩雪路线的标准连接结。",
                "克氏结（clove hitch）：用于把绳子连到主锁上并可单手快速调节长度，是保护站自我固定的标配结。",
                "双渔人结（double fisherman's）：用来连接两根绳，或制作抓结环。",
                "抓结 / 普鲁士结（prusik）：一种摩擦结。受力时抓紧绳索，不受力时可推动——用于上升、备份与救援系统。",
                "绳结打完要留够尾端，一般不少于 10 倍绳径，再互相检查一遍。这个结打得好不好，直接关系到安全。"
            ),
            questions = listOf(
                Question(
                    prompt = "用来把绳子连接到安全带、且最常用于冰川路线的结是？",
                    options = listOf("回穿 8 字结", "平结", "单结", "称人结"),
                    correct = 0,
                    explanation = "回穿 8 字结强度高、结构直观、便于他人检查，受力后仍相对容易解开。平结在受力后可能滑脱或死结化，绝不能用于保护性连接。"
                ),
                Question(
                    prompt = "需要单手快速调节长度、把绳子固定到主锁上时，用哪个结？",
                    options = listOf("双渔人结", "克氏结", "蝴蝶结", "水结"),
                    correct = 1,
                    explanation = "克氏结可以在主锁上单手推拉调节长度，是保护站自我固定（self-anchor）的标准做法。双渔人结用于连接两绳，蝴蝶结用于绳中固定点。"
                ),
                Question(
                    prompt = "抓结（普鲁士结一类摩擦结）的主要特性是？",
                    options = listOf(
                        "受力时抓紧绳索，不受力时可沿绳推动",
                        "可以永久替代主绳",
                        "只能用于打结装饰",
                        "受力后会自行解开"
                    ),
                    correct = 0,
                    explanation = "摩擦结的单向抓握特性，使它可以用于上升、备份与机械优势系统。也正因为它可靠与否取决于绳径搭配与缠绕圈数，所以必须实地练习验证。"
                )
            )
        ),

        Lesson(
            id = "l10",
            title = "安全带与结组系统",
            code = "ROPE TEAM",
            icon = "\uD83D\uDD17",
            brief = listOf(
                "冰川结组最理想是 3 人一队：当一人坠入裂缝时，另外两人仍有余量建立保护并实施救援。2 人队救援余量小，风险显著更高。",
                "结组间距通常在 8–12 米。在绳段之间打结（如蝴蝶结）可增加绳索陷入雪面的摩擦，抑制坠落者继续下坠。",
                "结组不是万能保险。它成立的前提是队友能有效制动、能建保护。队友做不到，绳队就会变成集体坠落。",
                "冰川行进应全程佩戴安全带，随身携带主锁、抓结环、雪锥等自救器材，并保证伸手可及。"
            ),
            questions = listOf(
                Question(
                    prompt = "冰川结组最理想的队伍人数是？",
                    options = listOf("2 人", "3 人", "6 人以上", "单人结组"),
                    correct = 1,
                    explanation = "3 人绳队在一人坠入裂缝时有足够的人力建立保护站并操作救援系统。2 人队一旦有人坠落，剩下的人既要制动又要建站救人，余量极小。"
                ),
                Question(
                    prompt = "冰川结组的常见间距是？",
                    options = listOf("2–3 米", "8–12 米", "30 米以上", "越短越好"),
                    correct = 1,
                    explanation = "8–12 米既能保证坠落时队友有足够的反应与制动距离，又便于交流与协作。间距过短会让坠落的冲击几乎无法缓冲。"
                ),
                Question(
                    prompt = "结组能真正提供保护的前提是？",
                    options = listOf(
                        "只要系上绳子就自动安全",
                        "队友能够有效制动并建立起保护",
                        "绳子足够长",
                        "人数足够多"
                    ),
                    correct = 1,
                    explanation = "绳队的安全来自「制动 + 固定」这两个动作的可靠执行。缺少制动能力或保护站技术的绳队，只是把人绑在一起一起掉下去而已。"
                )
            )
        ),

        Lesson(
            id = "l11",
            title = "保护站原则 SRENE / EARNEST",
            code = "ANCHORS",
            icon = "\u2693",
            brief = listOf(
                "SRENE 原则：Solid（牢固）、Redundant（冗余）、Equalized（均衡受力）、No Extension（不延展）。",
                "EARNEST 是更完整的评估框架：Efficient（高效）、Anchored（固定可靠）、Redundant（冗余）、No Extension（不延展）、Strong（强度足够）、Timely（快速建立）。",
                "判断标准很简单：拿掉任何一个点，系统还能不能站住。能，才算冗余。",
                "常见保护点：雪地可用雪锥、死挂（deadman）、T 型雪槽；冰面用冰锥。不同介质对应不同技术。"
            ),
            questions = listOf(
                Question(
                    prompt = "SRENE 原则中的 R 代表什么？",
                    options = listOf("Rapid 快速", "Redundant 冗余", "Rope 绳索", "Rigid 刚性"),
                    correct = 1,
                    explanation = "Redundant（冗余）要求系统中有多个独立保护点，任一点失效时其余点仍能承载。它是保护站安全最核心的一条。"
                ),
                Question(
                    prompt = "「No Extension」这条原则的含义是？",
                    options = listOf(
                        "绳子越长越好",
                        "某一点失效时系统不应产生冲击性延展",
                        "保护站不应建在低处",
                        "不需要额外备用器材"
                    ),
                    correct = 1,
                    explanation = "如果保护点之间留有松余，某点崩脱时其他点会承受突然的冲击载荷（冲击力远大于静载）。No Extension 要求预先均衡收紧，避免这种冲击。"
                ),
                Question(
                    prompt = "保护站设计最核心的思想是？",
                    options = listOf(
                        "尽量节省器材",
                        "单点失效不导致整个系统崩溃",
                        "建得越高越好",
                        "只用最粗的绳子"
                    ),
                    correct = 1,
                    explanation = "岩雪保护点的可靠性无法做到绝对确定，因此系统的设计目标是容错：让任何单一失效都不致命，为攀登者争取反应与备份时间。"
                )
            )
        ),

        Lesson(
            id = "l12",
            title = "冰裂缝与结组行进",
            code = "CREVASSE",
            icon = "\uD83D\uDD73\uFE0F",
            brief = listOf(
                "冰裂缝是冰川上最主要的风险。雪桥可能被新雪完全覆盖，外观与普通雪面毫无差别——这是它最致命的地方。",
                "通过裂缝区必须结组行进，绳索保持适度张紧、避免大量下垂，否则坠落者会先自由下坠再突然受力。",
                "一人坠入裂缝的标准流程：队友立即自制动 → 建立雪地保护站 → 把重量转移到保护站 → 用机械优势系统拉升。",
                "最常用的拉升系统是 3:1 的 Z 系统（Z-pulley / Z-drag）：在受力绳上装抓结与滑轮，配合保护站形成三倍机械优势。",
                "坠落者本人应保持冷静，尝试用抓结自我上升，并避免绳索继续切入裂缝边缘的雪檐。"
            ),
            questions = listOf(
                Question(
                    prompt = "队友坠入冰裂缝后，第一步应该做什么？",
                    options = listOf(
                        "立刻跳下去救他",
                        "立即自制动并建立保护站，把重量转移到保护站上",
                        "先打电话求救",
                        "先把绳子割断"
                    ),
                    correct = 1,
                    safetyCritical = true,
                    explanation = "坠落者的体重此刻全部压在你身上。必须先用自制动稳住自己，再建立独立保护站并把负荷转移过去，之后才有余力操作救援系统。跳下去或割绳都会让情况立刻恶化。"
                ),
                Question(
                    prompt = "救援拉升最常用的机械优势系统是？",
                    options = listOf("1:1 直接拉", "3:1 Z 系统（Z-pulley）", "10:1 系统", "不需要机械优势"),
                    correct = 1,
                    explanation = "3:1 的 Z 系统用抓结加一个滑轮就能获得三倍机械优势，结构简单、搭建快、器材需求少，是冰川救援的标准选择。"
                ),
                Question(
                    prompt = "为什么雪桥特别危险？",
                    options = listOf(
                        "因为它总是发出响声",
                        "被新雪覆盖后外观与普通雪面无异，难以识别",
                        "因为它只在夜间出现",
                        "因为它一定很薄"
                    ),
                    correct = 1,
                    explanation = "雪桥是最隐蔽的冰川风险：表面平整厚实，实际下方是空腔。判断雪桥可靠性需要观察裂缝走向、雪层结构与季节条件，这也是必须结组的原因。"
                )
            )
        )
    )
)

package com.ascent.climb.data

/**
 * 打包在应用内的服务条款（zh-CN / zh-TW / en）。
 *
 * 与隐私政策同一套排版结构（[PrivacyDoc]），由 PrivacyScreen 渲染，
 * 只是标题不同。其余七种界面语言回退英文——法律文本不做机器翻译。
 *
 * 改条款时官网 `/terms` 要一起改，版本写进 [TermsPolicy.VERSION]。
 */
object TermsPolicy {

    /** 条款版本（生效日期）。改条款就换这个值。 */
    const val VERSION = "2026-11-24"

    fun of(languageTag: String): PrivacyDoc = when {
        languageTag.startsWith("zh-TW") || languageTag.startsWith("zh-HK") ||
            languageTag.startsWith("zh-Hant") -> ZH_TW
        languageTag.startsWith("zh") -> ZH
        else -> EN
    }

    private val ZH: PrivacyDoc = PrivacyDoc(
        title = "服务条款。",
        lede = "这份条款说明 AscentRoam 的使用范围、内容边界与责任限制。它很短，因为这款应用本身也简单：免费、离线、不卖任何东西。",
        updated = "生效与更新：2026 年 11 月 24 日",
        summaryLabel = "摘要",
        summary = "免费供个人使用。应用提供教学与认知准备，不是向导、不是医疗建议，也不保证任何一次登顶。",
        intro = "下面是完整条款，共 6 节。",
        docNote = "继续使用本应用即表示你接受这些条款。",
        sections = listOf(
            PrivacySection(
                num = "01",
                title = "使用许可",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "AscentRoam 授予你个人的、非独占的、不可转让的使用许可。你可以把它装在自己的设备上，用于自己或家人的学习。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "不得把本应用或其内容作为付费产品转售、二次分发，或打包进收费课程。源码按仓库中的许可协议开放，与本文分开。"
                    )
                )
            ),
            PrivacySection(
                num = "02",
                title = "内容边界",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "应用内的课程、题库与技能书用于教学与认知准备，不构成专业向导、医疗建议或任何形式的登顶保证。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "冰雪、绳索、攀冰与救援技术必须在持证教练的现场指导下学习。高原病的判断与用药，以现场医生和最新指南为准。"
                    )
                )
            ),
            PrivacySection(
                num = "03",
                title = "账号与数据",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "账号与同步是可选的，不注册也能使用全部课程。我们如何处理数据，见应用内的隐私政策。"
                    )
                )
            ),
            PrivacySection(
                num = "04",
                title = "知识产权",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "应用内的文字、题目与插图由本项目编写或整理，版权归项目所有；引用的公开医学与技术资料归各自作者。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "未经许可，不得批量抓取题库，也不得把内容用于训练商业模型。"
                    )
                )
            ),
            PrivacySection(
                num = "05",
                title = "免责",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "本应用按「现状」提供，不附带任何明示或暗示的担保。在法律允许的范围内，因使用本应用而产生的直接或间接损失，由使用者自行承担。"
                    )
                )
            ),
            PrivacySection(
                num = "06",
                title = "变更与联系",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "条款更新会同时发布在应用内与官网，并更新生效日期。更新后继续使用，即表示接受新条款。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "有疑问写信到："
                    ),
                    PrivacyBlock(PrivacyBlockKind.MAIL, "support@ascentroam.com")
                )
            )
        )
    )

    private val ZH_TW: PrivacyDoc = PrivacyDoc(
        title = "服務條款。",
        lede = "這份條款說明 AscentRoam 的使用範圍、內容邊界與責任限制。它很短，因為這款應用程式本身也簡單：免費、離線、不賣任何東西。",
        updated = "生效與更新：2026 年 11 月 24 日",
        summaryLabel = "摘要",
        summary = "免費供個人使用。應用程式提供教學與認知準備，不是嚮導、不是醫療建議，也不保證任何一次登頂。",
        intro = "下面是完整條款，共 6 節。",
        docNote = "繼續使用本應用程式即表示你接受這些條款。",
        sections = listOf(
            PrivacySection(
                num = "01",
                title = "使用授權",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "AscentRoam 授予你個人的、非專屬的、不可轉讓的使用授權。你可以將它安裝在自己的裝置上，供自己或家人學習使用。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "不得將本應用程式或其內容作為付費產品轉售、二次散布，或包裝進收費課程。原始碼依倉庫中的授權條款開放，與本文分開。"
                    )
                )
            ),
            PrivacySection(
                num = "02",
                title = "內容邊界",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "應用程式內的課程、題庫與技能書用於教學與認知準備，不構成專業嚮導、醫療建議或任何形式的登頂保證。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "冰雪、繩索、攀冰與救援技術必須在持證教練的現場指導下學習。高山症的判斷與用藥，以現場醫師和最新指引為準。"
                    )
                )
            ),
            PrivacySection(
                num = "03",
                title = "帳號與資料",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "帳號與同步是可選的，不註冊也能使用全部課程。我們如何處理資料，見應用程式內的隱私權政策。"
                    )
                )
            ),
            PrivacySection(
                num = "04",
                title = "智慧財產權",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "應用程式內的文字、題目與插圖由本專案編寫或整理，著作權歸專案所有；引用的公開醫學與技術資料歸各自作者。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "未經許可，不得批次抓取題庫，也不得將內容用於訓練商業模型。"
                    )
                )
            ),
            PrivacySection(
                num = "05",
                title = "免責",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "本應用程式依「現狀」提供，不附帶任何明示或暗示的擔保。在法律允許的範圍內，因使用本應用程式而產生的直接或間接損失，由使用者自行承擔。"
                    )
                )
            ),
            PrivacySection(
                num = "06",
                title = "變更與聯絡",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "條款更新會同時發布在應用程式內與官網，並更新生效日期。更新後繼續使用，即表示接受新條款。"
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "有疑問請寫信到："
                    ),
                    PrivacyBlock(PrivacyBlockKind.MAIL, "support@ascentroam.com")
                )
            )
        )
    )

    private val EN: PrivacyDoc = PrivacyDoc(
        title = "Terms of service.",
        lede = "These terms cover what AscentRoam is for, where its content stops, and how far liability goes. They are short because the app is simple: free, offline, and selling nothing.",
        updated = "Effective and last updated: 24 November 2026",
        summaryLabel = "Summary",
        summary = "Free for personal use. The app is teaching and mental preparation — not a guide, not medical advice, and no promise of any summit.",
        intro = "The full terms follow in six sections.",
        docNote = "Continuing to use the app means you accept these terms.",
        sections = listOf(
            PrivacySection(
                num = "01",
                title = "Licence",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "AscentRoam grants you a personal, non-exclusive, non-transferable licence to use the app. You may install it on your own devices for yourself or your family."
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "You may not resell, redistribute or bundle the app or its content into a paid product or course. The source is released under the licence in its repository, which is separate from these terms."
                    )
                )
            ),
            PrivacySection(
                num = "02",
                title = "What the content is",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "Lessons, questions and field notes are for teaching and mental preparation. They are not professional guiding, medical advice, or any kind of guarantee that a summit will be reached."
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "Snow, rope, ice and rescue skills must be learned on site under a certified instructor. Diagnosis and medication for altitude illness follow the on-site doctor and current guidelines."
                    )
                )
            ),
            PrivacySection(
                num = "03",
                title = "Accounts and data",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "An account and sync are optional; every lesson works without one. How data is handled is set out in the privacy policy inside the app."
                    )
                )
            ),
            PrivacySection(
                num = "04",
                title = "Intellectual property",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "The text, questions and illustrations are written or compiled for this project and remain its copyright. Public medical and technical sources quoted here belong to their authors."
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "Do not bulk-scrape the question bank, or use the content to train commercial models, without permission."
                    )
                )
            ),
            PrivacySection(
                num = "05",
                title = "Liability",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "The app is provided as is, without warranty of any kind. To the extent the law allows, any direct or indirect loss arising from its use is borne by the user."
                    )
                )
            ),
            PrivacySection(
                num = "06",
                title = "Changes and contact",
                blocks = listOf(
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "Updates are published in the app and on the website, with a new effective date. Continuing to use the app after a change means you accept the new terms."
                    ),
                    PrivacyBlock(
                        PrivacyBlockKind.PARAGRAPH,
                        "Questions: write to"
                    ),
                    PrivacyBlock(PrivacyBlockKind.MAIL, "support@ascentroam.com")
                )
            )
        )
    )
}

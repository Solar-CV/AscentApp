package com.ascent.climb.data

/**
 * 打包在应用内的隐私政策（zh-CN / zh-TW / en）。
 *
 * 内容与官网 `ascentroam.com/privacy` 同源，改条款时两边一起改；版本写进
 * [PrivacyPolicy.VERSION]，注册前的同意记录只依赖这个字符串。
 *
 * 其余七种界面语言回退英文——法律文本不做机器翻译，宁可让用户读到有效力的版本。
 */
enum class PrivacyBlockKind { PARAGRAPH, HEADING, BULLETS, MAIL }

data class PrivacyBlock(
    val kind: PrivacyBlockKind,
    val text: String = "",
    /** 只有 [PrivacyBlockKind.BULLETS] 使用。 */
    val items: List<String> = emptyList()
)

data class PrivacySection(
    /** 章节序号，如 "01"。 */
    val num: String,
    val title: String,
    val blocks: List<PrivacyBlock>
)

data class PrivacyDoc(
    val title: String,
    val lede: String,
    val updated: String,
    val summaryLabel: String,
    val summary: String,
    val intro: String,
    val docNote: String,
    val sections: List<PrivacySection>
)

object PrivacyPolicy {

    /** 条款版本（生效日期）。改了条款就换这个值，注册同意记录跟着它走。 */
    const val VERSION = "2026-11-05"

    /** 按界面语言取条款；只有简中 / 繁中 / 英文三份，其余回退英文。 */
    fun of(languageTag: String): PrivacyDoc = when {
        languageTag.startsWith("zh-TW") || languageTag.startsWith("zh-HK") ||
            languageTag.startsWith("zh-Hant") -> ZH_TW
        languageTag.startsWith("zh") -> ZH
        else -> EN
    }

    private val ZH: PrivacyDoc = PrivacyDoc(
        title = "隐私政策。",
        lede = "AscentRoam 默认离线工作。只有你主动注册并开启同步，账号与学习数据才会离开这台设备。这份政策说明我们收集什么、为什么收集，以及你随时可以做什么。",
        updated = "生效与更新：2026 年 11 月 5 日",
        summaryLabel = "一句话版本",
        summary = "默认离线，只有注册并开启同步才会上传账号与学习数据。我们不投放广告、不做用户画像，也不出售数据。",
        intro = "下面是完整条款，共 10 节。你可以从头读，也可以用左侧目录直接跳到关心的部分。",
        docNote = "本页是隐私政策的网页版本，与 App 内展示的条款一致。若两者出现差异，以本页为准。",
        sections = listOf(
        PrivacySection(
            num = "01",
            title = "谁在处理你的数据",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "AscentRoam 由个人开发者独立开发与维护，不是由某家公司运营的产品。对本政策有疑问，或需要行使下文权利，请使用第 10 节的联系方式。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本政策适用于 AscentRoam Android 应用（包名 com.ascentroam.app）以及本站。如果你只使用离线功能、不注册账号，我们不会收到任何可识别你身份的信息。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "生效日期：2026 年 11 月 5 日。此前版本不再适用。"),
            )
        ),
        PrivacySection(
            num = "02",
            title = "我们收集哪些数据",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们只收集运行账号与同步所必需的数据，不收集与训练无关的信息。"),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "账号与登录"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("注册邮箱；", "昵称或用户名；", "密码哈希（scrypt，不可逆；我们不保存密码原文）；", "使用 Google 登录时的 Google 账号标识、邮箱与头像链接；", "注册时间、最近登录时间，以及用于区分设备的随机标识。")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "学习数据"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("课程与课时的完成状态；", "当前海拔、连续学习天数、成就徽章与复习队列；", "头像文件与本机显示名；", "同步时间戳与冲突处理所需的版本号。")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "App 内反馈"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "你在 App 内提交反馈时，我们会收到你填写的正文、你选择填写的邮箱，以及只有你主动勾选才会附上的诊断信息：应用版本、Android 版本、设备型号、界面语言与最近一次崩溃记录。诊断信息默认不附加。"),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "网站"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本站没有账号系统，也不使用广告、第三方分析或跨站跟踪。浏览器只在本地保存两项偏好：语言（ar-lang）与主题（ar-theme）。你可以随时在浏览器设置中清除它们。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "如果你使用网页的删除账号表单，我们会收到你填写的邮箱、昵称或用户名（可选）、备注（可选）、确认状态、界面语言、提交时间与来源页面。服务器日志可能记录 IP 地址、浏览器标识与请求时间，仅用于安全、限流与故障排查，不用于用户画像。"),
            )
        ),
        PrivacySection(
            num = "03",
            title = "我们为什么使用这些数据",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "上述数据只用于以下目的："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("创建和验证账号，保持登录状态与账号安全；", "在你的设备之间同步课程进度、海拔、徽章与复习队列；", "显示昵称与头像，计算成就和复习计划；", "处理删除、导出、更正与支持请求；", "防止滥用、排查故障并履行法律义务。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们不会出售你的数据，也不会把它用于广告画像或与本应用无关的推荐。"),
            )
        ),
        PrivacySection(
            num = "04",
            title = "数据存放在哪里",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "账号与学习数据存储在腾讯云 CloudBase 的上海地域。账号相关邮件通过 Resend 发送。传输使用 HTTPS；服务端访问受最小权限控制，数据库不直接暴露给客户端。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "如果你从中国大陆以外的地区使用本应用，数据会被传输到上述区域处理。这通常是提供同步服务所必需的；使用账号与同步功能即表示你知悉这一传输。我们会采取合理措施保护传输与存储安全。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "保留期限：账号数据在账号存续期间保留；删除账号后，生产库中的账号与学习数据立即删除，加密备份最多保留 30 天后自动删除；邮箱验证码 10 分钟内失效。"),
            )
        ),
        PrivacySection(
            num = "05",
            title = "我们如何共享数据",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们不出售、不出租、不交换你的个人数据。只在以下情形共享必要范围："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("云托管、邮件发送等服务商，仅按我们的指示处理数据，并受合同与保密义务约束；", "法律、司法或监管机关依法提出要求时；", "发生合并、收购或资产转让时，数据可能作为资产转移；我们会提前通知，并要求接收方继续受本政策约束；", "经过你明确同意的其他情形。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "应用不包含广告 SDK、第三方分析 SDK 或社交分享 SDK。"),
            )
        ),
        PrivacySection(
            num = "06",
            title = "你的权利",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "你可以随时："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("在 App 内查看或更正昵称、头像与界面语言；", "在 App 内删除账号：我的 → 设置 → 账号 → 删除账号；", "无法打开 App 时，使用本站的删除账号页提交请求，或直接发邮件；", "请求导出你的账号与学习数据；", "撤回同意、停止使用账号与同步，或注销账号；", "向所在地数据保护监管机构投诉。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "删除账号后，生产库中的账号与云端学习数据立即删除；加密备份最多保留 30 天，到期自动删除。删除不可恢复。本机数据可在 App 内重置，或通过卸载 App 清除。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们通常在 7 个工作日内回复请求。为保护账号安全，可能会先核验你的注册邮箱。"),
            )
        ),
        PrivacySection(
            num = "07",
            title = "第三方服务",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "使用 Google 登录时，Google 按其自身隐私政策处理认证信息。我们只接收完成登录所需的账号标识、邮箱与头像链接，不接收你的 Google 密码。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "账号数据存储在腾讯云 CloudBase；账号邮件由 Resend 发送。它们只在提供相应服务所必需的范围内处理数据。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "App 内反馈与网页的删除账号表单通过 Formspree 送达我们的支持邮箱；它只负责转发，不会将内容用于其他目的。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本站的图片、字体与界面资源均为自托管，不通过第三方广告或分析脚本加载。站内的 GitHub 等外部链接受其各自隐私政策约束。"),
            )
        ),
        PrivacySection(
            num = "08",
            title = "儿童与未成年人",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本服务不面向 13 岁以下儿童，我们不会有意收集他们的个人数据。如果你认为孩子向我们提供了数据，请联系我们，我们会尽快删除。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "中国大陆法律对未成年人个人信息另有规定的，从其规定。未成年人在注册或使用账号前，应取得监护人同意。"),
            )
        ),
        PrivacySection(
            num = "09",
            title = "政策变更",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们可能会更新本政策。变更会在本页发布并更新生效日期；涉及权利范围或处理目的的重大变更，会在 App 内或本页显著提示。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "变更生效后继续使用账号或同步功能，即表示你接受更新后的政策。如果你不同意，可以停止使用并删除账号。"),
            )
        ),
        PrivacySection(
            num = "10",
            title = "联系我们",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "隐私问题、数据导出、更正或删除请求，请联系："),
                PrivacyBlock(PrivacyBlockKind.MAIL, text = "support@ascentroam.com"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我们通常在 7 个工作日内回复。如果对回复不满意，你可以向所在地数据保护监管机构投诉。"),
            )
        ),
        )
    )

    private val ZH_TW: PrivacyDoc = PrivacyDoc(
        title = "隱私政策。",
        lede = "AscentRoam 預設離線工作。只有你主動註冊並開啟同步，帳號與學習資料才會離開這台裝置。這份政策說明我們收集什麼、為什麼收集，以及你隨時可以做什麼。",
        updated = "生效與更新：2026 年 11 月 5 日",
        summaryLabel = "一句話版本",
        summary = "預設離線，只有註冊並開啟同步才會上傳帳號與學習資料。我們不投放廣告、不做使用者畫像，也不出售資料。",
        intro = "下面是完整條款，共 10 節。你可以從頭讀，也可以用左側目錄直接跳到關心的部分。",
        docNote = "本頁是隱私政策的網頁版本，與 App 內展示的條款一致。若兩者出現差異，以本頁為準。",
        sections = listOf(
        PrivacySection(
            num = "01",
            title = "誰在處理你的資料",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "AscentRoam 由個人開發者獨立開發與維護，不是由某家公司營運的產品。對本政策有疑問，或需要行使下文權利，請使用第 10 節的聯絡方式。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本政策適用於 AscentRoam Android 應用程式（包名 com.ascentroam.app）以及本站。如果你只使用離線功能、不註冊帳號，我們不會收到任何可識別你身份的資訊。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "生效日期：2026 年 11 月 5 日。此前版本不再適用。"),
            )
        ),
        PrivacySection(
            num = "02",
            title = "我們收集哪些資料",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們只收集執行帳號與同步所必需的資料，不收集與訓練無關的資訊。"),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "帳號與登入"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("註冊電子郵件；", "暱稱或使用者名稱；", "密碼雜湊（scrypt，不可逆；我們不儲存密碼原文）；", "使用 Google 登入時的 Google 帳號標識、電子郵件與頭像連結；", "註冊時間、最近登入時間，以及用於區分裝置的隨機標識。")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "學習資料"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("課程與課時的完成狀態；", "當前海拔、連續學習天數、成就徽章與複習佇列；", "頭像檔案與本機顯示名；", "同步時間戳與衝突處理所需的版本號。")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "App 內回饋"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "你在 App 內提交回饋時，我們會收到你填寫的內容、你選擇填寫的電子郵件，以及只有你主動勾選才會附上的診斷資訊：應用程式版本、Android 版本、裝置型號、介面語言與最近一次當機記錄。診斷資訊預設不附加。"),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "網站"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本站沒有帳號系統，也不使用廣告、第三方分析或跨站跟蹤。瀏覽器只在本地儲存兩項偏好：語言（ar-lang）與主題（ar-theme）。你可以隨時在瀏覽器設定中清除它們。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "如果你使用網頁的刪除帳號表單，我們會收到你填寫的電子郵件、暱稱或使用者名稱（可選）、備註（可選）、確認狀態、介面語言、提交時間與來源頁面。伺服器日誌可能記錄 IP 地址、瀏覽器標識與請求時間，僅用於安全、限流與故障排查，不用於使用者畫像。"),
            )
        ),
        PrivacySection(
            num = "03",
            title = "我們為什麼使用這些資料",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "上述資料只用於以下目的："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("建立和驗證帳號，保持登入狀態與帳號安全；", "在你的裝置之間同步課程進度、海拔、徽章與複習佇列；", "顯示暱稱與頭像，計算成就和複習計劃；", "處理刪除、匯出、更正與支援請求；", "防止濫用、排查故障並履行法律義務。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們不會出售你的資料，也不會把它用於廣告畫像或與本應用無關的推薦。"),
            )
        ),
        PrivacySection(
            num = "04",
            title = "資料存放在哪裡",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "帳號與學習資料儲存在騰訊雲 CloudBase 的上海地域。帳號相關郵件透過 Resend 傳送。傳輸使用 HTTPS；服務端訪問受最小許可權控制，資料庫不直接暴露給客戶端。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "如果你從中國大陸以外的地區使用本應用，資料會被傳輸到上述區域處理。這通常是提供同步服務所必需的；使用帳號與同步功能即表示你知悉這一傳輸。我們會採取合理措施保護傳輸與儲存安全。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "保留期限：帳號資料在帳號存續期間保留；刪除帳號後，生產庫中的帳號與學習資料立即刪除，加密備份最多保留 30 天后自動刪除；電子郵件驗證碼 10 分鐘內失效。"),
            )
        ),
        PrivacySection(
            num = "05",
            title = "我們如何共享資料",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們不出售、不出租、不交換你的個人資料。只在以下情形共享必要範圍："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("雲託管、郵件傳送等服務商，僅按我們的指示處理資料，並受合同與保密義務約束；", "法律、司法或監管機關依法提出要求時；", "發生合併、收購或資產轉讓時，資料可能作為資產轉移；我們會提前通知，並要求接收方繼續受本政策約束；", "經過你明確同意的其他情形。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "應用不包含廣告 SDK、第三方分析 SDK 或社交分享 SDK。"),
            )
        ),
        PrivacySection(
            num = "06",
            title = "你的權利",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "你可以隨時："),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("在 App 內檢視或更正暱稱、頭像與介面語言；", "在 App 內刪除帳號：我的 → 設定 → 帳號 → 刪除帳號；", "無法開啟 App 時，使用本站的刪除帳號頁提交請求，或直接發郵件；", "請求匯出你的帳號與學習資料；", "撤回同意、停止使用帳號與同步，或登出帳號；", "向所在地資料保護監管機構投訴。")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "刪除帳號後，生產庫中的帳號與雲端學習資料立即刪除；加密備份最多保留 30 天，到期自動刪除。刪除不可恢復。本機資料可在 App 內重置，或透過解除安裝 App 清除。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們通常在 7 個工作日內回覆請求。為保護帳號安全，可能會先核驗你的註冊電子郵件。"),
            )
        ),
        PrivacySection(
            num = "07",
            title = "第三方服務",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "使用 Google 登入時，Google 按其自身隱私政策處理認證資訊。我們只接收完成登入所需的帳號標識、電子郵件與頭像連結，不接收你的 Google 密碼。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "帳號資料儲存在騰訊雲 CloudBase；帳號郵件由 Resend 傳送。它們只在提供相應服務所必需的範圍內處理資料。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "App 內回饋與網頁的刪除帳號表單透過 Formspree 送達我們的支援信箱；它只負責轉送，不會將內容用於其他目的。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本站的圖片、字型與介面資源均為自託管，不透過第三方廣告或分析指令碼載入。站內的 GitHub 等外部連結受其各自隱私政策約束。"),
            )
        ),
        PrivacySection(
            num = "08",
            title = "兒童與未成年人",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "本服務不面向 13 歲以下兒童，我們不會有意收集他們的個人資料。如果你認為孩子向我們提供了資料，請聯絡我們，我們會盡快刪除。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "中國大陸法律對未成年人個人資訊另有規定的，從其規定。未成年人在註冊或使用帳號前，應取得監護人同意。"),
            )
        ),
        PrivacySection(
            num = "09",
            title = "政策變更",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們可能會更新本政策。變更會在本頁釋出並更新生效日期；涉及權利範圍或處理目的的重大變更，會在 App 內或本頁顯著提示。"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "變更生效後繼續使用帳號或同步功能，即表示你接受更新後的政策。如果你不同意，可以停止使用並刪除帳號。"),
            )
        ),
        PrivacySection(
            num = "10",
            title = "聯絡我們",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "隱私問題、資料匯出、更正或刪除請求，請聯絡："),
                PrivacyBlock(PrivacyBlockKind.MAIL, text = "support@ascentroam.com"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "我們通常在 7 個工作日內回覆。如果對回覆不滿意，你可以向所在地資料保護監管機構投訴。"),
            )
        ),
        )
    )

    private val EN: PrivacyDoc = PrivacyDoc(
        title = "Privacy policy.",
        lede = "AscentRoam is offline by default. Account and learning data leave this device only if you register and enable sync. This policy explains what we collect, why, and what you can do about it.",
        updated = "Effective and last updated: 5 November 2026",
        summaryLabel = "In one paragraph",
        summary = "Offline by default. Account and learning data are uploaded only if you register and enable sync. No ads, no profiling, no sale of data.",
        intro = "The full terms follow in ten sections. Read from the top, or use the contents to jump to the part you need.",
        docNote = "This page is the web version of the privacy policy and matches the terms shown in the app. If they differ, this page controls.",
        sections = listOf(
        PrivacySection(
            num = "01",
            title = "Who processes your data",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "AscentRoam is built and maintained by an individual developer, not operated by a company. For questions about this policy, or to exercise the rights below, use the contact details in section 10."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "This policy covers the AscentRoam Android app (package com.ascentroam.app) and this website. If you use only the offline features and never create an account, we receive no information that identifies you."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Effective date: 5 November 2026. Earlier versions no longer apply."),
            )
        ),
        PrivacySection(
            num = "02",
            title = "What we collect",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We collect only what is needed to run an account and sync your progress. Nothing unrelated to training."),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "Account and sign-in"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("Your email address;", "A nickname or username;", "A password hash (scrypt, irreversible; we never store the password itself);", "If you sign in with Google, your Google account ID, email address and avatar URL;", "Registration and last sign-in times, and a random identifier used to distinguish devices.")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "Learning data"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("Lesson and unit completion;", "Current altitude, streak, achievement badges and review queue;", "Avatar file and local display name;", "Sync timestamps and the version numbers needed to resolve conflicts.")),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "In-app feedback"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "When you send feedback from the app, we receive the message you write, the email address you choose to give, and — only if you tick the box — diagnostic details: app version, Android version, device model, interface language and the most recent crash record. Diagnostics are never attached by default."),
                PrivacyBlock(PrivacyBlockKind.HEADING, text = "This website"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "This site has no account system and uses no ads, third-party analytics or cross-site tracking. Your browser stores two preferences locally: language (ar-lang) and theme (ar-theme). You can clear them in your browser settings at any time."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "If you use the account-deletion form on this site, we receive the email address you enter, an optional nickname or username, an optional note, your confirmation, interface language, submission time and the referring page. Server logs may record IP address, browser identifier and request time for security, rate limiting and troubleshooting only — never for profiling."),
            )
        ),
        PrivacySection(
            num = "03",
            title = "Why we use it",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We use the data above only to:"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("Create and verify your account and keep sign-in and account security working;", "Sync lesson progress, altitude, badges and review queue across your devices;", "Show your nickname and avatar and calculate achievements and review schedules;", "Handle deletion, export, correction and support requests;", "Prevent abuse, diagnose failures and meet legal obligations.")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We do not sell your data or use it for advertising profiles or recommendations unrelated to this app."),
            )
        ),
        PrivacySection(
            num = "04",
            title = "Where the data lives",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Account and learning data are stored in Tencent Cloud CloudBase in the Shanghai region. Account-related email is sent through Resend. Traffic uses HTTPS; server access is least-privilege and the database is not exposed directly to clients."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "If you use the app from outside mainland China, data is transferred to that region for processing. This is normally necessary to provide sync; using an account and sync means you understand this transfer. We take reasonable measures to protect it in transit and at rest."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Retention: account data is kept while the account exists. After deletion, account and learning data are removed from production immediately; encrypted backups are kept for at most 30 days and then deleted automatically. Email verification codes expire within 10 minutes."),
            )
        ),
        PrivacySection(
            num = "05",
            title = "How we share data",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We do not sell, rent or trade your personal data. We share only what is necessary in these cases:"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("Cloud hosting and email delivery providers, which process data only on our instructions under contractual and confidentiality obligations;", "When required by law, a court or a regulator;", "In a merger, acquisition or transfer of assets, data may transfer as an asset; we will give notice beforehand and require the recipient to remain bound by this policy;", "Any other case with your explicit consent.")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "The app contains no advertising SDK, third-party analytics SDK or social-sharing SDK."),
            )
        ),
        PrivacySection(
            num = "06",
            title = "Your rights",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "At any time, you can:"),
                PrivacyBlock(PrivacyBlockKind.BULLETS, items = listOf("View or correct your nickname, avatar and interface language in the app;", "Delete your account in the app: Profile → Settings → Account → Delete account;", "If you cannot open the app, use the account-deletion page on this site or email us;", "Request an export of your account and learning data;", "Withdraw consent, stop using an account and sync, or close the account;", "Complain to your local data-protection regulator.")),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "After deletion, the account and synced learning data are removed from production immediately; encrypted backups remain for at most 30 days and are then deleted automatically. Deletion is irreversible. Local data can be reset in the app or removed by uninstalling it."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We normally respond within 7 business days. To protect the account, we may first verify your registered email address."),
            )
        ),
        PrivacySection(
            num = "07",
            title = "Third-party services",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "When you sign in with Google, Google handles authentication under its own privacy policy. We receive only the account ID, email address and avatar URL needed to complete sign-in — never your Google password."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Account data is stored in Tencent Cloud CloudBase and account email is sent through Resend. Each processes data only as needed to provide that service."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "In-app feedback and the account-deletion form on this site are delivered to our support inbox through Formspree, which forwards the content and does not use it for any other purpose."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Images, fonts and interface assets on this site are self-hosted and are not loaded through third-party advertising or analytics scripts. External links such as GitHub are governed by their own privacy policies."),
            )
        ),
        PrivacySection(
            num = "08",
            title = "Children and minors",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "The service is not directed at children under 13, and we do not knowingly collect their personal data. If you believe a child has provided data to us, contact us and we will delete it promptly."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Where mainland Chinese law sets additional rules for minors’ personal information, those rules apply. Minors should obtain a guardian’s consent before registering or using an account."),
            )
        ),
        PrivacySection(
            num = "09",
            title = "Changes to this policy",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We may update this policy. Changes are published on this page with a new effective date; material changes to rights or purposes will be flagged in the app or prominently here."),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "Continuing to use an account or sync after a change takes effect means you accept the updated policy. If you do not agree, you can stop using the service and delete your account."),
            )
        ),
        PrivacySection(
            num = "10",
            title = "Contact us",
            blocks = listOf(
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "For privacy questions, data export, correction or deletion requests, contact:"),
                PrivacyBlock(PrivacyBlockKind.MAIL, text = "support@ascentroam.com"),
                PrivacyBlock(PrivacyBlockKind.PARAGRAPH, text = "We normally reply within 7 business days. If you are not satisfied with the response, you can complain to your local data-protection regulator."),
            )
        ),
        )
    )

}

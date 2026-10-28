package com.ascent.climb.ui

/** 开屏展示的一句引用：原文 + 原作者名，均保持原语言，不做翻译。 */
data class SplashQuote(val text: String, val author: String)

/**
 * 开屏名言库（随机一条）。
 *
 * **一律用原话原文，不翻译**：这几句都是登山史上有出处的原话，译成中文或任何语言
 * 都会丢掉语气与分寸；读者看到的是本人怎么写、怎么说的。
 *
 * 出处（逐条核对过）：
 * - Mallory, "Because it's there." — 1923 年答《纽约时报》问为何要登珠峰
 * - Whymper, "Scrambles Amongst the Alps"（1871）
 * - Muir, 1873 年致妹妹的信
 * - Boukreev, "Above the Clouds"（2001），亦刻在安纳普尔纳大本营纪念石上
 * - Viesturs, "No Shortcuts to the Top"（2006）扉页自律格言
 * - Tabei, 1975 年成为首位登顶珠峰的女性后所言（英文转述）
 * - Messner, "All Fourteen 8,000ers"（1987）
 * - Lowe，登山圈流传最广的一句，收录于 "The Quotable Climber"
 * - Bonatti，意大利登山家，论攀登本质的常引之语（英文转述）
 */
object SplashQuotes {

    private data class Entry(val text: String, val author: String)

    private val entries = listOf(
        Entry("Because it's there.", "George Mallory"),
        Entry(
            "Climb if you will, but remember that courage and strength are nought without prudence.",
            "Edward Whymper"
        ),
        Entry("The mountains are calling and I must go.", "John Muir"),
        Entry(
            "Mountains are not stadiums where I satisfy my ambition to achieve, they are the cathedrals where I practice my religion.",
            "Anatoli Boukreev"
        ),
        Entry("Getting to the top is optional. Getting down is mandatory.", "Ed Viesturs"),
        Entry(
            "Technique and ability alone do not get you to the top; it is the willpower that is the most important.",
            "Junko Tabei"
        ),
        Entry(
            "Mountains are not fair or unfair, they are just dangerous.",
            "Reinhold Messner"
        ),
        Entry(
            "The best climber in the world is the one who's having the most fun.",
            "Alex Lowe"
        ),
        Entry(
            "Climbing is not a battle with the elements, nor against the law of gravity. It's a battle against oneself.",
            "Walter Bonatti"
        )
    )

    /** 每次冷启动随机一条；与界面语言无关，原文照用。 */
    fun pick(): SplashQuote {
        val e = entries.random()
        return SplashQuote(e.text, e.author)
    }

    /** 供开发者模式/自检使用：条目数。 */
    val size: Int get() = entries.size
}

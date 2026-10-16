package com.ascent.climb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ALL_ARTICLES
import com.ascent.climb.data.HANDBOOK_CATEGORIES
import com.ascent.climb.data.HandbookArticle

/** 分类 → 线性图标（替代 emoji）。 */
private fun categoryIcon(category: String): ImageVector = when {
    category.contains("装备") -> Ic.Backpack
    category.contains("技术") -> Ic.Compass
    category.contains("医学") || category.contains("健康") -> Ic.HeartPulse
    category.contains("天气") || category.contains("气象") -> Ic.CloudSnow
    category.contains("决策") || category.contains("判断") -> Ic.Brain
    category.contains("安全") || category.contains("风险") -> Ic.ShieldAlert
    else -> Ic.BookOpen
}

private fun readingMinutes(a: HandbookArticle): Int {
    val chars = a.sections.sumOf { it.body.length } + a.summary.length
    return (chars / 300).coerceAtLeast(2)
}

@Composable
fun HandbookScreen() {
    var openId by remember { mutableStateOf<String?>(null) }
    val article = openId?.let { id -> ALL_ARTICLES.firstOrNull { it.id == id } }

    if (article == null) {
        ArticleList(onOpen = { openId = it })
    } else {
        ArticleDetail(article = article, onBack = { openId = null })
    }
}

@Composable
private fun ArticleList(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("全部") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item(key = "title") {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text("技能书", color = Ascent.Fg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MonoLabel("${ALL_ARTICLES.size} 篇 · LIBRARY")
            }
        }

        item(key = "search") {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(shape)
                    .background(Ascent.Surface)
                    .border(1.dp, Ascent.Border, shape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.Search, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("搜索技能书", color = Ascent.Fg3, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ascent.Fg, fontSize = 14.sp),
                        cursorBrush = SolidColor(Ascent.Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item(key = "chips") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip("全部", cat == "全部") { cat = "全部" }
                HANDBOOK_CATEGORIES.forEach { c ->
                    CategoryChip(c, cat == c) { cat = c }
                }
            }
        }

        val filtered = ALL_ARTICLES.filter { a ->
            (cat == "全部" || a.category == cat) &&
                (query.isBlank() || a.title.contains(query, true) || a.summary.contains(query, true))
        }

        items(filtered.size, key = { i -> filtered[i].id }) { i ->
            val a = filtered[i]
            ArticleCard(a = a, onClick = { onOpen(a.id) })
        }

        if (filtered.isEmpty()) {
            item(key = "nores") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconBadge(Ic.Search, Ascent.Fg3, Ascent.Surface2, diameter = 56, iconSize = 24)
                    Spacer(Modifier.height(12.dp))
                    Text("没有匹配的文章", color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("换个关键词或分类试试", color = Ascent.Fg3, fontSize = 12.sp)
                }
            }
        }

        item(key = "disc") {
            Spacer(Modifier.height(16.dp))
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Ascent.WarningSoft)
                    .border(1.dp, Ascent.Border, shape)
                    .padding(16.dp)
            ) {
                Icon(Ic.TriangleAlert, contentDescription = null, tint = Ascent.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "本手册为教学整理，不构成医疗建议，也不能替代持证教练的现场指导。" +
                        "涉及药物、救援与急救的决策必须由专业人员评估。",
                    color = Ascent.Fg2,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Ascent.Fg else Ascent.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, Ascent.Border, shape))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Ascent.Bg else Ascent.Fg2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ArticleCard(a: HandbookArticle, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(shape)
            .background(Ascent.Surface)
            .border(1.dp, Ascent.Border, shape)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ascent.AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(a.category), contentDescription = null, tint = Ascent.Accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(a.title, color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${a.category} · ${readingMinutes(a)} 分钟",
                color = Ascent.Fg3,
                fontSize = 10.sp,
                fontFamily = MonoNumbers,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(a.summary, color = Ascent.Fg2, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ArticleDetail(article: HandbookArticle, onBack: () -> Unit) {
    // 系统返回 / 全面屏侧滑手势：回到手册列表，而不是退出 App
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onBack() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("技能书", color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Ic.Bookmark, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(12.dp))

        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3AA3FF), Color(0xFF0267CE)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(article.category), contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftTag(article.category, Ascent.Warning, Ascent.WarningSoft)
            Spacer(Modifier.width(10.dp))
            MonoLabel("${readingMinutes(article)} 分钟阅读")
        }
        Spacer(Modifier.height(10.dp))
        Text(article.title, color = Ascent.Fg, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        MonoLabel(article.code)

        Spacer(Modifier.height(14.dp))
        val sumShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sumShape)
                .background(Ascent.Surface2)
                .padding(15.dp)
        ) {
            Text(article.summary, color = Ascent.Fg2, fontSize = 13.sp, lineHeight = 21.sp)
        }

        article.sections.forEach { s ->
            Spacer(Modifier.height(22.dp))
            Text(s.heading, color = Ascent.Fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(s.body, color = Ascent.Fg2, fontSize = 14.sp, lineHeight = 23.sp)
        }

        // 出处仅保留 SEVEN SUMMIT TREKS 的来源；其余出处不展示
        val shownSources = article.sources.filter { it.contains("SEVEN SUMMIT TREKS") }
        if (shownSources.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            MonoLabel("内容来源")
            Spacer(Modifier.height(10.dp))
            shownSources.forEach { src ->
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Ascent.Accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(src, color = Ascent.Fg3, fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "本文为教学整理，不构成医疗建议。",
            color = Ascent.Fg3,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(36.dp))
    }
}

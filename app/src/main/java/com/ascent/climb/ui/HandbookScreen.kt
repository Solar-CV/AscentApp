package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ALL_ARTICLES
import com.ascent.climb.data.HandbookArticle
import com.ascent.climb.data.activeTrackId
import com.ascent.climb.data.articlesOfTrack
import com.ascent.climb.data.handbookCategories

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
    // 详情页从全集里查：切换大类时若正读着一篇，不至于突然空掉
    val article = openId?.let { id -> ALL_ARTICLES.firstOrNull { it.id == id } }
    val trackId = activeTrackId()

    // 列表与详情是互斥分支，读文章时列表整棵子树会离开组合。
    // 用状态槽按分支保管，返回时滚动位置与搜索/筛选都还在。
    val pageStates = rememberSaveableStateHolder()
    pageStates.SaveableStateProvider(if (article == null) "hb-list" else "hb-detail") {
        if (article == null) {
            ArticleList(trackId = trackId, onOpen = { openId = it })
        } else {
            ArticleDetail(article = article, onBack = { openId = null })
        }
    }
}

@Composable
private fun ArticleList(trackId: String, onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val allLabel = stringResource(R.string.hb_filter_all)
    // 语言切换时 allLabel 会变，用 key 重建状态，避免筛选值停留在旧语言
    var cat by rememberSaveable(allLabel, trackId) { mutableStateOf(allLabel) }
    val articles = remember(trackId) { articlesOfTrack(trackId) }
    val categories = remember(trackId) { handbookCategories(trackId) }

    val ctx = LocalContext.current
    val filtered = articles.filter { a ->
        val title = localized(ctx, "hb.${a.id}.title", a.title)
        val summary = localized(ctx, "hb.${a.id}.summary", a.summary)
        (cat == allLabel || a.category == cat) &&
            (query.isBlank() || title.contains(query, true) || summary.contains(query, true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item(key = "title") {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text(stringResource(R.string.hb_title), color = Ascent.Fg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MonoLabel(stringResource(R.string.hb_subtitle, articles.size))
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
                        Text(stringResource(R.string.hb_search), color = Ascent.Fg3, fontSize = 14.sp)
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
            // 分类有 10 个，固定一排放不下：原先用普通 Row 会被屏幕右边缘裁掉，
            // 最后一个可见 chip 只剩一截没有文字的圆角底色（看起来像「空白长条」），
            // 后面的分类也点不到。改为横向滚动，末尾留 16dp 由 contentPadding 承担。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(allLabel, cat == allLabel) { cat = allLabel }
                categories.forEach { c ->
                    CategoryChip(categoryName(c), cat == c) { cat = c }
                }
                Spacer(Modifier.width(8.dp))
            }
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
                    Text(stringResource(R.string.hb_empty), color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.hb_empty_hint), color = Ascent.Fg3, fontSize = 12.sp)
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
                    text = stringResource(R.string.hb_disclaimer_1) +
                        stringResource(R.string.hb_disclaimer_2),
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
            Text(articleTitle(a.id, a.title), color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.hb_meta, categoryName(a.category), readingMinutes(a)),
                color = Ascent.Fg3,
                fontSize = 10.sp,
                fontFamily = MonoNumbers,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(articleSummary(a.id, a.summary), color = Ascent.Fg2, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2)
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
                Text(stringResource(R.string.hb_title), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
            SoftTag(categoryName(article.category), Ascent.Warning, Ascent.WarningSoft)
            Spacer(Modifier.width(10.dp))
            MonoLabel(stringResource(R.string.hb_read_minutes, readingMinutes(article)))
        }
        Spacer(Modifier.height(10.dp))
        Text(articleTitle(article.id, article.title), color = Ascent.Fg, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
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
            Text(articleSummary(article.id, article.summary), color = Ascent.Fg2, fontSize = 13.sp, lineHeight = 21.sp)
        }

        article.sections.forEachIndexed { i, s ->
            Spacer(Modifier.height(22.dp))
            Text(articleSectionHeading(article.id, i + 1, s.heading), color = Ascent.Fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(articleSectionBody(article.id, i + 1, s.body), color = Ascent.Fg2, fontSize = 14.sp, lineHeight = 23.sp)
        }

        // 出处仅保留 SEVEN SUMMIT TREKS 的来源；其余出处不展示
        val shownSources = article.sources.filter { it.contains("SEVEN SUMMIT TREKS") }
        if (shownSources.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            MonoLabel(stringResource(R.string.hb_source))
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
            text = stringResource(R.string.hb_article_note),
            color = Ascent.Fg3,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(36.dp))
    }
}

package com.ascent.climb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.data.PrivacyBlock
import com.ascent.climb.data.PrivacyBlockKind
import com.ascent.climb.data.PrivacyDoc
import com.ascent.climb.data.PrivacyPolicy
import com.ascent.climb.data.PrivacySection
import androidx.compose.ui.res.stringResource

/**
 * 应用内隐私政策页。
 *
 * 条款正文打包在 [com.ascent.climb.data.PrivacyPolicy] 里（简中 / 繁中 / 英文），
 * 与官网同一份内容。这里只负责排版：章节号 + 标题 + 段落 / 小标题 / 列表 / 邮箱。
 *
 * 为什么不做成网页壳：注册前必须能**离线**读到条款，WebView 断网就是白屏，
 * 而法律文本恰恰是要求「随时可查」的那一类。
 */
@Composable
fun PrivacyScreen(
    doc: PrivacyDoc,
    onBack: () -> Unit
) {
    // 系统返回 / 侧滑：回到上一层，而不是退出 App
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ascent.Bg)
    ) {
        Row(
            modifier = Modifier
                .clip(squircle(999.dp))
                .clickable { onBack() }
                .padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.set_privacy),
                color = Ascent.Fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 96.dp)
        ) {
            item(key = "head") {
                Column {
                    Text(
                        text = doc.lede,
                        color = Ascent.Fg2,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = doc.updated, color = Ascent.Fg3, fontSize = 11.5.sp)
                    Spacer(Modifier.height(16.dp))
                    // 一句话版本：放在最前面，绝大多数用户读完这段就够了
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircle(14.dp))
                            .background(Ascent.AccentSoft)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = doc.summaryLabel.uppercase(),
                            color = Ascent.AccentText,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = doc.summary,
                            color = Ascent.Fg,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            items(doc.sections, key = { it.num }) { section ->
                SectionBlock(section)
            }

            item(key = "note") {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.privacy_in_app_note, PrivacyPolicy.VERSION),
                    color = Ascent.Fg3,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SectionBlock(section: PrivacySection) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = section.num,
                color = Ascent.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.title,
                color = Ascent.Fg,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        section.blocks.forEach { block -> Block(block) }
    }
}

@Composable
private fun Block(block: PrivacyBlock) {
    when (block.kind) {
        PrivacyBlockKind.PARAGRAPH -> {
            Text(
                text = block.text,
                color = Ascent.Fg2,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        PrivacyBlockKind.HEADING -> {
            Text(
                text = block.text,
                color = Ascent.Fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
            )
        }

        PrivacyBlockKind.BULLETS -> {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                block.items.forEach { item ->
                    Row(modifier = Modifier.padding(bottom = 5.dp)) {
                        Text(text = "·", color = Ascent.Fg3, fontSize = 13.sp, lineHeight = 21.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item,
                            color = Ascent.Fg2,
                            fontSize = 13.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }

        PrivacyBlockKind.MAIL -> {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(squircle(10.dp))
                    .background(Ascent.Surface2)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = block.text,
                    color = Ascent.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

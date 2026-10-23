package com.ascent.climb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.BuildConfig
import com.ascent.climb.R
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.BANK_SOURCE
import com.ascent.climb.data.BANK_VERSION
import com.ascent.climb.data.Diagnostics
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.activeTrackId

/**
 * 反馈收件邮箱。
 *
 * - 填了 → 用 ACTION_SENDTO(mailto:) 打开邮件应用，收件人已预填，用户点「发送」即可
 * - 留空 → 退化成系统分享（ACTION_SEND），由用户自己挑应用
 *
 * 之所以不在 App 里直连 SMTP 或某个表单接口：SMTP 需要把账号密码打进 APK，
 * 谁都能扒出来；表单接口需要一个后端。等有了固定收件邮箱，只改这一行即可。
 */
private const val FEEDBACK_EMAIL = ""

/** 反馈类型。索引与界面上的顺序一致。 */
private val FEEDBACK_TYPES = listOf(
    R.string.fb_type_issue,
    R.string.fb_type_idea,
    R.string.fb_type_content
)

/**
 * 反馈与建议。
 *
 * 发送方式复用系统邮件应用，不在 App 内直连服务器：
 * 不需要额外权限（邮件应用自己有），也不会有「反馈服务器挂了」这种失败态。
 * 诊断信息默认不附带，必须用户自己打开开关。
 */
@Composable
fun FeedbackScreen(store: ProgressStore, onBack: () -> Unit) {
    val context = LocalContext.current
    var typeIndex by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var attachDiag by remember { mutableStateOf(false) }
    // 0 未发送 / 1 已唤起邮件 / 2 没有邮件应用已复制 / 3 内容为空
    var status by remember { mutableStateOf(0) }
    val crashAvailable = remember { Diagnostics.hasCrash(context) }

    BackHandler { onBack() }

    fun buildBody(): String = buildString {
        appendLine(message.trim())
        if (contact.isNotBlank()) {
            appendLine()
            appendLine("Contact: ${contact.trim()}")
        }
        if (attachDiag) {
            appendLine()
            appendLine("— Diagnostics —")
            appendLine(
                Diagnostics.environment(
                    context,
                    listOf(
                        "Bank" to "v$BANK_VERSION · $BANK_SOURCE",
                        "Track" to activeTrackId(),
                        "Done" to "${store.completed.size}",
                        "Badges" to "${store.unlockedAchievements.size}/${ACHIEVEMENTS.size}"
                    )
                )
            )
            Diagnostics.lastCrash(context)?.let {
                appendLine()
                appendLine("— Last crash —")
                appendLine(it)
            }
        }
    }

    fun send() {
        if (message.isBlank()) {
            status = 3
            return
        }
        val body = buildBody()
        val subject = "${context.getString(FEEDBACK_TYPES[typeIndex])} · ASCENT ${BuildConfig.VERSION_NAME}"
        val ok = runCatching {
            // ACTION_SENDTO + mailto: 只会命中邮件应用（不会弹出微信、记事本之类）。
            // 收件人留空时，用户自己在邮件里填；填了就预填好，点发送即可。
            val uri = Uri.parse(if (FEEDBACK_EMAIL.isNotBlank()) "mailto:$FEEDBACK_EMAIL" else "mailto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (FEEDBACK_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            // LocalContext 是包装过的 Context，不是 Activity，必须带 NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, null))
        }.isSuccess
        if (ok) {
            status = 1
        } else {
            copyToClipboard(context, body)
            status = 2
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            item(key = "head") {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onBack() }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.width(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.common_back), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.fb_title), color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.fb_sub),
                    color = Ascent.Fg2,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
            }

            item(key = "type") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FEEDBACK_TYPES.forEachIndexed { i, res ->
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = stringResource(res),
                                style = if (i == typeIndex) BtnStyle.Primary else BtnStyle.Secondary,
                                height = 42,
                                onClick = {
                                    typeIndex = i
                                    if (status == 1 || status == 2) status = 0
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item(key = "msg") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel(stringResource(R.string.fb_msg_label))
                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            if (status != 0) status = 0
                        },
                        minLines = 5,
                        maxLines = 10,
                        placeholder = { Text(stringResource(R.string.fb_msg_hint), fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "contact") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel(stringResource(R.string.fb_contact_label))
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.fb_contact_hint), fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "diag") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fb_diag),
                                color = Ascent.Fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.fb_diag_desc),
                                color = Ascent.Fg3,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = attachDiag, onCheckedChange = { attachDiag = it })
                    }
                    if (attachDiag && crashAvailable) {
                        Text(
                            text = stringResource(R.string.fb_diag_crash),
                            color = Ascent.Warning,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item(key = "send") {
                PillButton(
                    text = stringResource(R.string.fb_send),
                    style = BtnStyle.Primary,
                    height = 50,
                    enabled = message.isNotBlank(),
                    onClick = { send() }
                )
                Spacer(Modifier.height(10.dp))
                val note = when (status) {
                    1 -> stringResource(R.string.fb_sent)
                    2 -> stringResource(R.string.fb_no_mail)
                    3 -> stringResource(R.string.fb_need_text)
                    else -> stringResource(R.string.fb_send_note)
                }
                Text(
                    text = note,
                    color = if (status == 3) Ascent.Danger else Ascent.Fg3,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    runCatching {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("ASCENT feedback", text))
    }
}

/** 反馈入口卡（「我的」页账号下方）。 */
@Composable
fun FeedbackEntryCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ascent.Surface)
            .clickable { onOpen() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(Ic.MessageSquare, Ascent.Fg2, Ascent.Surface2, diameter = 46, iconSize = 22)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.fb_title), color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.fb_entry_sub), color = Ascent.Fg3, fontSize = 12.sp)
        }
        Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
    }
}

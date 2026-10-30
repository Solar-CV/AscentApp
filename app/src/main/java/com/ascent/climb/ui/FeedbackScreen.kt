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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ascent.climb.data.FeedbackSender
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.activeTrackId
import kotlinx.coroutines.launch

/**
 * 反馈收件邮箱，仅用于「改用邮件发送」这条退路。
 *
 * 主路径已经走 Formspree（见 data/FeedbackSender.kt），不依赖邮件应用。
 * 这里留空时，邮件退路的收件人由用户自己填；填了就会预填。
 */
private const val FALLBACK_EMAIL = ""

/**
 * 反馈类型。索引与界面上的顺序一致。
 */
private val FEEDBACK_TYPES = listOf(
    R.string.fb_type_issue,
    R.string.fb_type_idea,
    R.string.fb_type_content
)

/** 提交阶段。 */
private object FbPhase {
    const val IDLE = 0
    const val SENDING = 1
    const val OK = 2
    const val FAIL = 3
    const val NEED_TEXT = 4
    const val MAIL_OPENED = 5
    const val MAIL_MISSING = 6
}

/**
 * 反馈与建议。
 *
 * 主路径：直接 POST 到 Formspree（JSON），点一下就提交，不经过邮件应用；
 * 退路：网络失败时可以「改用邮件发送」，走 ACTION_SENDTO，并把正文复制进剪贴板兜底。
 *
 * 诊断信息默认不附带，必须用户自己打开开关；附带时随请求一起提交，
 * 内容口径见 fb_diag_desc（不含课程内容、做题记录、账号与任何用户输入）。
 */
@Composable
fun FeedbackScreen(store: ProgressStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typeIndex by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var attachDiag by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(FbPhase.IDLE) }
    val crashAvailable = remember { Diagnostics.hasCrash(context) }

    BackHandler { onBack() }

    /** 诊断块：环境 + 最近崩溃，拼成一段可读文本。 */
    fun diagnosticsText(): String = buildString {
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

    fun submit() {
        if (message.isBlank()) {
            phase = FbPhase.NEED_TEXT
            return
        }
        val payload = linkedMapOf(
            "type" to context.getString(FEEDBACK_TYPES[typeIndex]),
            "message" to message.trim(),
            "app" to "ASCENT ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            "_subject" to "${context.getString(FEEDBACK_TYPES[typeIndex])} · ASCENT ${BuildConfig.VERSION_NAME}"
        )
        if (contact.isNotBlank()) payload["email"] = contact.trim()
        if (attachDiag) payload["diagnostics"] = diagnosticsText()

        phase = FbPhase.SENDING
        scope.launch {
            phase = when (FeedbackSender.submit(payload)) {
                is FeedbackSender.Result.Success -> FbPhase.OK
                is FeedbackSender.Result.Failure -> FbPhase.FAIL
            }
        }
    }

    fun sendViaMail() {
        if (message.isBlank()) {
            phase = FbPhase.NEED_TEXT
            return
        }
        val body = buildString {
            appendLine(message.trim())
            if (contact.isNotBlank()) {
                appendLine()
                appendLine("Contact: ${contact.trim()}")
            }
            if (attachDiag) {
                appendLine()
                appendLine("— Diagnostics —")
                appendLine(diagnosticsText())
            }
        }
        val subject = "${context.getString(FEEDBACK_TYPES[typeIndex])} · ASCENT ${BuildConfig.VERSION_NAME}"
        val uri = Uri.parse(if (FALLBACK_EMAIL.isNotBlank()) "mailto:$FALLBACK_EMAIL" else "mailto:")
        val opened = runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (FALLBACK_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(FALLBACK_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            // LocalContext 是包装过的 Context，不是 Activity，必须带 NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, null))
        }.isSuccess
        if (opened) {
            phase = FbPhase.MAIL_OPENED
        } else {
            copyToClipboard(context, body)
            phase = FbPhase.MAIL_MISSING
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
                                    if (phase != FbPhase.IDLE && phase != FbPhase.SENDING) phase = FbPhase.IDLE
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
                            if (phase != FbPhase.IDLE && phase != FbPhase.SENDING) phase = FbPhase.IDLE
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
                        Switch(
                            checked = attachDiag,
                            onCheckedChange = {
                                attachDiag = it
                                if (phase != FbPhase.IDLE && phase != FbPhase.SENDING) phase = FbPhase.IDLE
                            }
                        )
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
                val sending = phase == FbPhase.SENDING
                PillButton(
                    text = if (sending) stringResource(R.string.fb_sending) else stringResource(R.string.fb_send),
                    style = BtnStyle.Primary,
                    height = 50,
                    enabled = !sending && message.isNotBlank(),
                    onClick = { submit() }
                )
                Spacer(Modifier.height(10.dp))
                val note = when (phase) {
                    FbPhase.OK -> stringResource(R.string.fb_ok)
                    FbPhase.FAIL -> stringResource(R.string.fb_fail)
                    FbPhase.NEED_TEXT -> stringResource(R.string.fb_need_text)
                    FbPhase.MAIL_OPENED -> stringResource(R.string.fb_sent)
                    FbPhase.MAIL_MISSING -> stringResource(R.string.fb_no_mail)
                    else -> stringResource(R.string.fb_send_note)
                }
                Text(
                    text = note,
                    color = when (phase) {
                        FbPhase.OK -> Ascent.Success
                        FbPhase.FAIL, FbPhase.NEED_TEXT -> Ascent.Danger
                        else -> Ascent.Fg3
                    },
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
                // 网络不通时的退路：走系统邮件
                if (phase == FbPhase.FAIL || phase == FbPhase.MAIL_MISSING) {
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        text = stringResource(R.string.fb_mail_alt),
                        style = BtnStyle.Ghost,
                        height = 42,
                        onClick = { sendViaMail() }
                    )
                }
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
    val a = clayAccents()
    ClaySurface(
        modifier = modifier.fillMaxWidth().clickable { onOpen() },
        radius = 20.dp,
        elevation = 7.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayChip(color = Color(0xFF2F6FD0), size = 42.dp, radius = 14.dp) {
                Icon(Ic.MessageSquare, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.fb_title), color = a.value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.fb_entry_sub), color = a.sub, fontSize = 12.sp)
            }
            Icon(Ic.ChevronRight, contentDescription = null, tint = a.sub, modifier = Modifier.size(16.dp))
        }
    }
}

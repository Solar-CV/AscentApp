package com.ascent.climb.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.data.cloud.CloudDevice
import com.ascent.climb.data.cloud.CloudProfile
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

/**
 * 已登录设备。
 *
 * 存在意义是**安全感**：用户能看见「除了我，还有谁在访问这个账号」，并且一键踢掉。
 * 所以这里不藏任何东西 —— 设备类型、版本、最后使用时间全列出来，
 * 连当前正在用的这台也列出来（标上「当前设备」），否则列表看起来会像少了一台。
 *
 * 撤销当前设备是一个真实的登出：服务端会把 access/refresh 一起作废，
 * 所以必须清本地会话并退回未登录，不能假装还登着。
 */
@Composable
internal fun DevicesScreen(
    onBack: () -> Unit,
    /** 撤销了当前设备（也就是自己）时回调：上层负责清本地登录态并回到未登录界面。 */
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { CloudProfile(context) }

    var devices by remember { mutableStateOf<List<CloudDevice>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableStateOf(0) }

    var pending by remember { mutableStateOf<CloudDevice?>(null) }
    var revoking by remember { mutableStateOf(false) }
    var revokeFailed by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(reloadTick) {
        loading = true
        failed = false
        val list = profile.devices()
        devices = list
        failed = list == null
        loading = false
    }

    pending?.let { target ->
        val self = target.current
        DangerConfirmDialog(
            title = stringResource(if (self) R.string.dv_revoke_self_title else R.string.dv_revoke_title),
            body = stringResource(if (self) R.string.dv_revoke_self_body else R.string.dv_revoke_body),
            confirmText = stringResource(R.string.dv_revoke),
            busy = revoking,
            busyText = stringResource(R.string.auth_wait),
            error = if (revokeFailed) stringResource(R.string.dv_revoke_failed) else null,
            onCancel = {
                if (!revoking) {
                    pending = null
                    revokeFailed = false
                }
            },
            onConfirm = {
                scope.launch {
                    revoking = true
                    revokeFailed = false
                    val wasSelf = profile.revokeDevice(target.id)
                    revoking = false
                    when (wasSelf) {
                        null -> revokeFailed = true
                        true -> {
                            pending = null
                            onSignedOut()
                        }
                        false -> {
                            pending = null
                            reloadTick += 1
                        }
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
        // ---------------- 头部 ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onBack() }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.common_back), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
            Text(
                stringResource(R.string.dv_title),
                color = Ascent.Fg,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dv_intro),
                color = Ascent.Fg3,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dv_loading), color = Ascent.Fg3, fontSize = 13.sp)
            }

            failed -> Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.dv_error), color = Ascent.Fg3, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Box(Modifier.width(140.dp)) {
                    PillButton(
                        text = stringResource(R.string.dv_retry),
                        style = BtnStyle.Secondary,
                        height = 42,
                        onClick = { reloadTick += 1 }
                    )
                }
            }

            devices.isNullOrEmpty() -> Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dv_empty), color = Ascent.Fg3, fontSize = 13.sp)
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices!!, key = { it.id }) { device ->
                    DeviceRow(device = device, onTap = { pending = device })
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: CloudDevice, onTap: () -> Unit) {
    val label = platformLabel(device.platform)
    // 当前设备也允许撤销：怀疑账号被别人用时，「把这台也踢掉」是合理诉求。
    // 后果由确认框讲清楚，这里不做二次拦截。
    OutlineCard(gap = 0, onClick = onTap) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Ic.Signal, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 标题只放「平台 · 版本」：它是最需要看清的一行，独占整行。
                // 「当前设备」标签挪到第二行末尾——意/西/俄的这个标签有 14–18 个字符，
                // 挤在标题行会把版本号截成 `Android · 0.26.…`。
                Text(
                    text = if (device.appVersion.isBlank()) label else "$label · ${device.appVersion}",
                    color = Ascent.Fg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 俄语「Последнее использование 12:34」在 11sp 下要 200dp 以上，
                    // 窄屏会折行；锁一行，超长时截断的是前缀，时间仍在。
                    Text(
                        stringResource(R.string.dv_last_used, formatLastUsed(device.lastUsedAt)),
                        color = Ascent.Fg3,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (device.current) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Ascent.AccentSoft)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                stringResource(R.string.dv_current),
                                color = Ascent.Accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            // 撤销：单独占一列，不参与压缩；长语言（Widerrufen / Återkalla / Отозвать）
            // 也只占一行，和列里的文字之间留 8dp 间隔。
            Text(
                stringResource(R.string.dv_revoke),
                color = Ascent.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** 平台字段是服务端原样存的，这里只负责显示友好一点，不猜未知平台。 */
private fun platformLabel(raw: String): String = when (raw.lowercase()) {
    "android" -> "Android"
    "ios" -> "iOS"
    "web" -> "Web"
    "" -> "未知设备"
    else -> raw
}

/** 今天只显示时:分，跨天补日期。和「上次同步」保持同一种表达。 */
private fun formatLastUsed(ts: Long): String {
    if (ts <= 0) return "—"
    val at = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
    val pattern = if (at.toLocalDate() == LocalDate.now()) "HH:mm" else "MM-dd HH:mm"
    return at.format(DateTimeFormatter.ofPattern(pattern))
}

package com.ascent.climb.ui

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.ascent.climb.R
import com.ascent.climb.data.ProgressStore
import kotlinx.coroutines.launch

/**
 * 编辑个人资料：头像 + 用户名。
 *
 * 头像文件存 filesDir、名字存 prefs；**登录状态下名字还会推一份到账号**（[onNameCommitted]），
 * 否则换设备登录会看到旧昵称。
 * 头像选中即生效（文件已复制），用户名在按「完成」或返回时才落库 ——
 * 不给「保存」按钮之外的第三种状态，避免用户以为改了其实没存。
 */
@Composable
fun EditProfileScreen(
    store: ProgressStore,
    onBack: () -> Unit,
    /** 名字落库后回调，由上层负责推到云端（上层的协程作用域活得更久）。 */
    onNameCommitted: (String) -> Unit = {},
    /** 头像换了或删了：true=有新图，false=已移除。上层据此推云端。 */
    onAvatarChanged: (Boolean) -> Unit = {}
) {
    // 这个工程里 LocalActivityResultRegistryOwner 一直是 null（MainActivity 的 LocalContext
    // 被语言包装过，Compose 那条 ViewTree 注入没走通），直接调 rememberLauncherForActivityResult
    // 会抛 "No ActivityResultRegistryOwner was provided"。
    // 解决办法就地补：ComponentActivity 本身实现了 ActivityResultRegistryOwner，
    // 沿 ContextWrapper 找到它再提供下去，不改动整个 App 的层级。
    // 注意用 LocalView 的 context，而不是 LocalContext：
    // LocalContext 在 MainActivity 里被 AppLocale 换成了 createConfigurationContext 的产物，
    // 它既不是 ContextWrapper 也不是 Activity，findActivity() 会直接拿到 null。
    // LocalView（AndroidComposeView）的 context 才是真正的宿主 Activity。
    val view = LocalView.current
    val owner = remember(view) { view.context.findActivity() as? ActivityResultRegistryOwner }
    if (owner != null) {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            EditProfileBody(store = store, onBack = onBack, onNameCommitted = onNameCommitted, onAvatarChanged = onAvatarChanged)
        }
    } else {
        EditProfileBody(store = store, onBack = onBack, onNameCommitted = onNameCommitted, onAvatarChanged = onAvatarChanged)
    }
}

@Composable
private fun EditProfileBody(
    store: ProgressStore,
    onBack: () -> Unit,
    onNameCommitted: (String) -> Unit = {},
    onAvatarChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(store.displayName) }
    var version by remember { mutableIntStateOf(store.avatarVersion) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.pf_default_name)
    // 裁剪页在根层渲染，这里只发起请求并等结果
    val cropper = LocalAvatarCrop.current

    // 返回与「完成」走同一条路：先把名字写进去，再退。否则输入框里的改动会静默丢失。
    fun commitAndBack() {
        store.displayName = name
        onNameCommitted(name.trim())
        onBack()
    }
    BackHandler { commitAndBack() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // 选完先进裁剪页（1:1 锁比例）。这里挂起等用户确认，取消就什么都不做，旧头像不受影响。
        scope.launch {
            val square = cropper?.crop(uri)
            if (square == null) {
                if (cropper == null) failed = true
                return@launch
            }
            busy = true
            failed = false
            if (AvatarStore.saveCropped(context, square)) {
                version += 1
                store.avatarVersion = version
                onAvatarChanged(true)
            } else {
                failed = true
            }
            busy = false
        }
    }

    fun launchPicker() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
                // 键盘弹出时窗口会缩，底部「完成」会被挤出可见区（还被 tab 栏压住）。
                // imePadding 把整列顶到键盘上方，按钮就一直在屏幕内。
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { commitAndBack() }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.width(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.common_back),
                    color = Ascent.Fg2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.pf_title),
                color = Ascent.Fg,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // 键盘弹出后窗口变矮，若整列不可滚，「完成」会被挤出可见区（它还被 tab 栏压着）。
            // 所以上半段可滚、按钮固定在最底部。
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(Modifier.height(26.dp))

            // ---------------- 头像 ----------------
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !busy) { launchPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        version = version,
                        name = name.ifBlank { defaultName },
                        size = 104.dp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PillButton(
                        text = stringResource(R.string.pf_avatar_change),
                        style = BtnStyle.Secondary,
                        height = 40,
                        fontSize = 13.sp,
                        onClick = { if (!busy) launchPicker() }
                    )
                }
                // 用文件是否存在判断，而不是 version > 0 —— 版本号现在是单调递增的「重新解码开关」，
                // 退出登录清头像时也会 +1，拿它当「有没有头像」会误显示「移除头像」。
                if (AvatarStore.exists(context)) {
                    Column(modifier = Modifier.weight(1f)) {
                        PillButton(
                            text = stringResource(R.string.pf_avatar_remove),
                            style = BtnStyle.Secondary,
                            height = 40,
                            fontSize = 13.sp,
                            onClick = {
                                AvatarStore.remove(context)
                                version += 1
                                store.avatarVersion = version
                                failed = false
                                onAvatarChanged(false)
                            }
                        )
                    }
                }
            }

            if (failed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.pf_avatar_failed),
                    color = Ascent.Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            // ---------------- 用户名 ----------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.pf_name_label),
                    color = Ascent.Fg2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.pf_name_hint), color = Ascent.Fg3, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 16) name = it },
                singleLine = true,
                placeholder = { Text(defaultName, fontSize = 14.sp) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )


            }

            Spacer(Modifier.height(12.dp))
            PillButton(
                text = stringResource(R.string.pf_done),
                onClick = { commitAndBack() }
            )
        }
    }
}

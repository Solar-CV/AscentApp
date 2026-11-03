package com.ascent.climb.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ascent.climb.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 头像裁剪。
 *
 * 为什么不放在编辑资料页里当普通子页面：那一页活在 Scaffold 的 tab 内容区，子页面只能铺满
 * 底部导航栏以上，裁剪这种全屏取景的界面会露出下面一条。所以照 3D 徽章页的做法，
 * 用**根层覆盖**（[AvatarCropOverlay] 和 AscentRoot 同级），自己按 safeDrawing 留安全区。
 *
 * 为什么不用 Dialog：实测 API 36 上 Dialog 窗口拿到的 inset 与它实际的窗口框对不上
 *（底部按钮被系统手势条压住），而根层覆盖这条路在本工程里已经验证过。
 *
 * 页面之间用 [AvatarCropController] 交接：调用方 `suspend` 等结果，用户确认后拿到
 * 正方形位图，取消拿到 null。调用方的状态（编辑页里的头像版本号等）都留在原地，
 * 不用往 MainActivity 提。
 */

/** 照片编辑用固定深色底：浅色主题下也不会因为白底刺眼而看不清取景边界。 */
private val CropBg = Color(0xFF0B0B0C)
private val CropFg = Color(0xFFF5F5F7)
private val CropFg2 = Color(0xFF8E8E93)
private val CropGrid = Color(0x33FFFFFF)

/** 相对「铺满取景框」的额外放大倍数上限。再大就只剩像素块了。 */
private const val MAX_ZOOM = 4f

/**
 * 裁剪请求的交接点。
 *
 * [crop] 挂起到用户按下「完成」（返回正方形）或取消/返回（返回 null）。
 * 根层只负责渲染，不持有业务状态。
 */
class AvatarCropController {

    internal class Pending(val uri: Uri, val onResult: (Bitmap?) -> Unit)

    internal var pending by mutableStateOf<Pending?>(null)
        private set

    suspend fun crop(uri: Uri): Bitmap? = suspendCancellableCoroutine { cont ->
        val request = Pending(uri) { result -> if (cont.isActive) cont.resume(result) }
        pending = request
        cont.invokeOnCancellation { if (pending === request) pending = null }
    }

    internal fun finish(result: Bitmap?) {
        val request = pending ?: return
        pending = null
        request.onResult(result)
    }
}

val LocalAvatarCrop = staticCompositionLocalOf<AvatarCropController?> { null }

/**
 * 全屏裁剪层。放在根 `Box` 里、AscentRoot 之后，才能盖住底部导航栏。
 * 没有请求时什么都不组合。
 */
@Composable
fun AvatarCropOverlay(controller: AvatarCropController) {
    val request = controller.pending ?: return

    BackHandler { controller.finish(null) }

    // 整页是深色的，系统栏图标必须转成白色：浅色主题下 MainActivity 把它们设成了深色，
    // 黑底上等于看不见。离开时原样还回去（照 Badge3DPoc 的做法，只在这里改）。
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(Unit) {
        val insets = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevLightStatus = insets?.isAppearanceLightStatusBars
        val prevLightNav = insets?.isAppearanceLightNavigationBars
        insets?.isAppearanceLightStatusBars = false
        insets?.isAppearanceLightNavigationBars = false
        onDispose {
            prevLightStatus?.let { insets.isAppearanceLightStatusBars = it }
            prevLightNav?.let { insets.isAppearanceLightNavigationBars = it }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CropBg)) {
        // 内容按 safeDrawing 留边；底色留在这层之外，系统栏区域也是黑的
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            CropFlow(
                uri = request.uri,
                onCancel = { controller.finish(null) },
                onCrop = { controller.finish(it) }
            )
        }
    }
}

@Composable
private fun CropFlow(uri: Uri, onCancel: () -> Unit, onCrop: (Bitmap) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(uri) { mutableStateOf(true) }

    LaunchedEffect(uri) {
        bitmap = AvatarStore.loadForEdit(context, uri)
        loading = false
    }

    val bmp = bitmap
    when {
        bmp != null -> CropBody(bitmap = bmp, onCancel = onCancel, onCrop = onCrop)
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CropFg2, strokeWidth = 2.dp)
        }
        else -> LoadFailed(onCancel)
    }
}

@Composable
private fun LoadFailed(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.pf_avatar_failed),
            color = CropFg2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.width(140.dp)) {
            PillButton(
                text = stringResource(R.string.common_back),
                style = BtnStyle.Secondary,
                height = 44,
                fontSize = 14.sp,
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun CropBody(bitmap: Bitmap, onCancel: () -> Unit, onCrop: (Bitmap) -> Unit) {
    // 位图会被「旋转」换掉，所以放进状态里而不是当参数钉死
    val state = remember(bitmap) { AvatarCropState(bitmap) }
    val image = remember(state.bitmap) { state.bitmap.asImageBitmap() }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---------------- 顶栏 ----------------
        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Ic.X,
                    contentDescription = stringResource(R.string.common_close),
                    tint = CropFg,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                stringResource(R.string.pf_crop_title),
                color = CropFg,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(76.dp)
            ) {
                PillButton(
                    text = stringResource(R.string.pf_done),
                    height = 36,
                    fontSize = 14.sp,
                    onClick = { state.crop()?.let(onCrop) ?: onCancel() }
                )
            }
        }

        // ---------------- 取景框 ----------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val side: Dp = min(maxWidth.value, maxHeight.value).dp
                Box(
                    modifier = Modifier
                        .size(side)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .onSizeChanged { state.sidePx = it.width.toFloat() }
                        .pointerInput(state.bitmap, state.sidePx) {
                            detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                                state.transform(centroid, pan, zoom)
                            }
                        }
                ) {
                    CropCanvas(state = state, image = image)
                }
            }
        }

        // ---------------- 底部工具 ----------------
        Text(
            stringResource(R.string.pf_crop_hint),
            color = CropFg2,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            CropTool(Ic.RotateCcw, stringResource(R.string.pf_crop_rotate)) { state.rotate() }
            CropTool(Ic.Maximize2, stringResource(R.string.common_reset)) { state.reset() }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CropCanvas(state: AvatarCropState, image: ImageBitmap) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val s = state.scale()
        val dw = state.bitmap.width * s
        val dh = state.bitmap.height * s
        val left = (size.width - dw) / 2f + state.pan.x
        val top = (size.height - dh) / 2f + state.pan.y

        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(state.bitmap.width, state.bitmap.height),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
        )

        // 三分线：只在取景时帮忙找构图，所以压到很淡
        val thin = 1.dp.toPx()
        for (i in 1..2) {
            val p = size.width * i / 3f
            drawLine(CropGrid, Offset(p, 0f), Offset(p, size.height), thin)
            drawLine(CropGrid, Offset(0f, p), Offset(size.width, p), thin)
        }
    }
}

@Composable
private fun CropTool(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x14FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CropFg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = CropFg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 裁剪几何。
 *
 * 坐标约定：`pan` 是「图像中心相对取景框中心的位移」（像素），`zoom` 是在
 * 「短边铺满取景框」基础上的额外倍数。这样缩放取景都不会露出空白：
 * zoom ≥ 1 时图像必然覆盖整个取景框。
 */
private class AvatarCropState(initial: Bitmap) {

    var bitmap by mutableStateOf(initial)
        private set

    var zoom by mutableFloatStateOf(1f)
        private set

    var pan by mutableStateOf(Offset.Zero)
        private set

    /** 取景框边长（像素）。布局完成后由 onSizeChanged 写入。 */
    var sidePx by mutableFloatStateOf(0f)

    /** 短边刚好铺满取景框时的缩放系数。 */
    private fun cover(): Float =
        if (sidePx <= 0f) 1f else sidePx / min(bitmap.width, bitmap.height).toFloat()

    fun scale(): Float = cover() * zoom

    /**
     * 一次手势：以双指中心为锚点缩放，再叠加平移，最后夹回边界。
     * 锚点公式保证手指按住的像素在缩放前后停在原地。
     */
    fun transform(centroid: Offset, panChange: Offset, zoomChange: Float) {
        val oldZoom = zoom
        val newZoom = (oldZoom * zoomChange).coerceIn(1f, MAX_ZOOM)
        val ratio = if (oldZoom == 0f) 1f else newZoom / oldZoom
        val c = centroid - Offset(sidePx / 2f, sidePx / 2f)
        val next = c - (c - pan) * ratio + panChange
        zoom = newZoom
        pan = clamp(next, newZoom)
    }

    private fun clamp(p: Offset, z: Float): Offset {
        val s = cover() * z
        val maxX = ((bitmap.width * s - sidePx) / 2f).coerceAtLeast(0f)
        val maxY = ((bitmap.height * s - sidePx) / 2f).coerceAtLeast(0f)
        return Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
    }

    fun reset() {
        zoom = 1f
        pan = Offset.Zero
    }

    /** 顺时针 90°。位图真的转过去，裁剪数学就不用再管角度。 */
    fun rotate() {
        val src = bitmap
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = runCatching {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }.getOrNull() ?: return
        // 旧图不 recycle：它可能还在上一帧的绘制管线里，交给 GC 更安全
        bitmap = rotated
        reset()
    }

    /**
     * 取景框对应到源图的那块正方形。
     *
     * 不用「截屏再裁」：那样会带上预览的缩放损失。这里按当前变换反算出源图坐标，
     * 直接取原始像素，输出才最干净。
     */
    fun crop(): Bitmap? {
        if (sidePx <= 0f) return null
        val s = scale()
        if (s <= 0f) return null

        val maxSide = min(bitmap.width, bitmap.height)
        val side = (sidePx / s).roundToInt().coerceIn(1, maxSide)

        val centerX = bitmap.width / 2f - pan.x / s
        val centerY = bitmap.height / 2f - pan.y / s
        // 小图被放大时取景框可能超出原图，夹回图内
        val x = (centerX - side / 2f).roundToInt().coerceIn(0, bitmap.width - side)
        val y = (centerY - side / 2f).roundToInt().coerceIn(0, bitmap.height - side)

        return runCatching { Bitmap.createBitmap(bitmap, x, y, side, side) }.getOrNull()
    }
}

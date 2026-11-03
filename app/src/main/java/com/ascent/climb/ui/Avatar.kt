package com.ascent.climb.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 本地头像存取。
 *
 * 为什么不存 Uri：相册给的 content:// 授权是临时的，重启后大概率失效；
 * 也不把图片塞 SharedPreferences（Base64 会撑爆）。所以复制一份到应用私有目录，
 * 固定文件名 `avatar.jpg`，路径不进 prefs，只有版本号进（见 ProgressStore.avatarVersion）。
 */
object AvatarStore {

    private const val FILE_NAME = "avatar.jpg"

    /** 输出边长。列表里最大用到 96dp，2.75x 屏 ≈ 264px，512 有富余。 */
    private const val OUTPUT_PX = 512

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun exists(context: Context): Boolean = file(context).isFile

    /**
     * 从相册选中的 Uri 复制一份进来。
     *
     * 处理顺序：两次解码（先读尺寸再降采样）→ 居中裁正方形 → 缩到 512 → JPEG。
     * 直接原图解码在 4000×3000 的照片上会吃掉几十 MB，低端机容易 OOM。
     *
     * @return 成功与否。失败时不动旧头像。
     */
    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

            // 先按 2 的幂降采样到「至少 2×512」，之后裁切缩放才有余量
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= OUTPUT_PX &&
                bounds.outHeight / (sample * 2) >= OUTPUT_PX
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching false

            val side = minOf(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(
                decoded,
                (decoded.width - side) / 2,
                (decoded.height - side) / 2,
                side,
                side
            )
            val scaled = if (side == OUTPUT_PX) square
            else Bitmap.createScaledBitmap(square, OUTPUT_PX, OUTPUT_PX, true)

            // 先写临时文件再改名：中途失败不会留下半张图
            val target = file(context)
            val tmp = File(target.parentFile, "$FILE_NAME.tmp")
            FileOutputStream(tmp).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (target.exists()) target.delete()
            val ok = tmp.renameTo(target)

            if (square !== decoded) square.recycle()
            if (scaled !== square) scaled.recycle()
            decoded.recycle()
            ok
        }.getOrDefault(false)
    }

    /** 删除头像文件。prefs 里的版本号由调用方清。 */
    fun remove(context: Context) {
        runCatching { file(context).delete() }
    }
}

/**
 * 读头像位图。`version` 变化时重新解码 —— 文件名固定，不这样做换了头像不会刷新。
 *
 * 解码放到 IO 线程：512px 的 JPEG 虽然快，但首帧解码仍可能掉帧，
 * 个人页又是每次切 tab 都会重新组合的地方。
 */
@Composable
fun rememberAvatarBitmap(version: Int): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by produceState<ImageBitmap?>(initialValue = null, version) {
        value = if (version <= 0) null else withContext(Dispatchers.IO) {
            runCatching {
                val f = AvatarStore.file(context)
                if (!f.isFile) null
                else BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return state
}

/**
 * 圆形头像。没有自定义头像时用底色 + 首字（比一个通用小人图标更有辨识度）。
 *
 * @param name 用来取首字；空则回落到图标
 */
@Composable
fun UserAvatar(
    version: Int,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bmp = rememberAvatarBitmap(version)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Ascent.Accent)
        )
    } else {
        val initial = name.trim().take(1).uppercase()
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Ascent.Accent),
            contentAlignment = Alignment.Center
        ) {
            if (initial.isEmpty()) {
                Icon(
                    Ic.UserRound,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.46f)
                )
            } else {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

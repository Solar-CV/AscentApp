package com.ascent.climb.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import kotlin.math.min

/**
 * 本地头像存取。
 *
 * 为什么不存 Uri：相册给的 content:// 授权是临时的，重启后大概率失效；
 * 也不把图片塞 SharedPreferences（Base64 会撑爆）。所以复制一份到应用私有目录，
 * 固定文件名 `avatar.jpg`，路径不进 prefs，只有版本号进（见 ProgressStore.avatarVersion）。
 *
 * 流程分两步，中间隔着用户手动裁剪：
 *   相册 Uri --loadForEdit--> 可编辑位图 --(裁剪页)--> 正方形 --saveCropped--> avatar.jpg
 */
object AvatarStore {

    private const val FILE_NAME = "avatar.jpg"

    /** 输出边长。列表里最大用到 96dp，2.75x 屏 ≈ 264px，512 有富余。 */
    private const val OUTPUT_PX = 512

    /**
     * 进裁剪页时的解码目标：短边至少这么多像素。
     *
     * 裁剪框内的画面会被再次放大（用户可放大到 5×），源头分辨率不够就成了糊图。
     * 1280 能让 2× 放大时 512 输出仍接近 1:1，同时 4000×3000 的原图只解到 2000×1500
     *（约 12 MB），低端机不至于 OOM。
     */
    private const val EDIT_MIN_SHORT = 1280

    /**
     * 长边上限。手机全景图能到 20000×1280，只按短边算采样率会解出上亿像素，
     * 一进裁剪页就 OOM。两条约束同时生效，取满足条件的最小采样率。
     */
    private const val EDIT_MAX_LONG = 3072

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun exists(context: Context): Boolean = file(context).isFile

    /** 读出当前头像的原始 JPEG 字节；没有文件返回 null。上传云端用。 */
    fun readBytes(context: Context): ByteArray? = runCatching {
        val f = file(context)
        if (f.isFile) f.readBytes() else null
    }.getOrNull()

    /**
     * 把云端下载的 JPEG 落盘。先写 `.tmp` 再 rename —— 中途失败不会留下半张图，
     * 和 [saveCropped] 是同一套写法。
     */
    fun writeBytes(context: Context, bytes: ByteArray): Boolean = runCatching {
        if (bytes.size < 4) return false
        val target = file(context)
        val tmp = File(target.parentFile, "$FILE_NAME.tmp")
        FileOutputStream(tmp).use { it.write(bytes) }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }.getOrDefault(false)

    /**
     * 解码成「可以交给裁剪页」的位图。
     *
     * 两次解码：先只读尺寸算采样率，再真正解码。顺序不能反 —— 直接解 4000×3000
     * 的 ARGB_8888 是 48 MB，低端机必炸。
     *
     * 还会按 EXIF 摆正：相机竖拍的照片像素其实是横的，靠 EXIF Orientation 标记旋转，
     * BitmapFactory 不认这个标记。不摆正的话，进裁剪页看到的是一张躺倒的图，
     * 用户也没法用 90° 旋转按钮把它掰回来还保持取景。
     *
     * @return null 表示读不出来（损坏文件、无权限、非图片）。
     */
    suspend fun loadForEdit(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= EDIT_MIN_SHORT ||
                maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= EDIT_MAX_LONG
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null

            applyExifOrientation(context, uri, decoded)
        }.getOrNull()
    }

    /**
     * 把裁剪结果写成头像。入参必须是正方形（裁剪页保证），这里只负责缩放与落盘。
     *
     * 先写临时文件再改名：中途失败不会留下半张图。失败时不动旧头像。
     */
    suspend fun saveCropped(context: Context, square: Bitmap): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val scaled = if (square.width == OUTPUT_PX && square.height == OUTPUT_PX) square
            else Bitmap.createScaledBitmap(square, OUTPUT_PX, OUTPUT_PX, true)

            val target = file(context)
            val tmp = File(target.parentFile, "$FILE_NAME.tmp")
            FileOutputStream(tmp).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (target.exists()) target.delete()
            val ok = tmp.renameTo(target)

            if (scaled !== square) scaled.recycle()
            ok
        }.getOrDefault(false)
    }

    /** 删除头像文件。prefs 里的版本号由调用方清。 */
    fun remove(context: Context) {
        runCatching { file(context).delete() }
    }

    /**
     * 按 EXIF Orientation 摆正。没有标记或读不出 EXIF 时原样返回。
     *
     * 换过方向的旧图会被回收：它只是解码中间产物，还没交给界面。
     */
    private fun applyExifOrientation(context: Context, uri: Uri, src: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return src
        }

        val out = runCatching {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }.getOrNull() ?: return src
        if (out !== src) src.recycle()
        return out
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

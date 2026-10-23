package com.ascent.climb.ui

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.TextureView
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * ASCENT 3D 徽章 POC。
 *
 * 只为一件事存在：在真机上回答四个问题 ——
 *   1. 这枚 glb 在 Filament 下观感如何（金属 + 珐琅是否成立）
 *   2. 首帧要多久（Engine 创建 + glb 解析 + IBL 上传）
 *   3. 持续转动的帧时间（目标 16.7ms 内）
 *   4. TextureView 透明合成能否把 3D 叠在普通页面背景上
 *
 * 不是产品代码：徽章写死 first_step，文案写死中文，引擎不释放（App 生命周期常驻）。
 */

private const val MODEL_PATH = "badges/first_step.glb"
private const val IBL_PATH = "env/studio_ibl.ktx"

/** 棚拍环境的 3 波段辐照度 SH（由 tools/poc3d/gen_studio_ibl.py 算出，linear）。 */
private val STUDIO_SH = floatArrayOf(
    1.048368f, 1.139804f, 1.285377f,
    0.569332f, 0.614961f, 0.665967f,
    0.288779f, 0.288789f, 0.285139f,
    -0.030211f, -0.003779f, 0.032231f,
    -0.078805f, -0.064596f, -0.043296f,
    0.298662f, 0.304838f, 0.301797f,
    -0.122837f, -0.131313f, -0.144427f,
    -0.107003f, -0.080241f, -0.042032f,
    -0.287974f, -0.269272f, -0.247473f,
)

/** 引擎级单例：Engine / IBL / glb 只建一次，两个展示形态共用。 */
private object Badge3D {
    var appContext: Context? = null

    /** 强制走 GLES（Mali 驱动上 Vulkan 与 Adreno 行为不同，POC 需要能对照）。 */
    fun forceGles(context: Context): Boolean =
        context.getSharedPreferences("poc3d", Context.MODE_PRIVATE)
            .getBoolean("force_gles", false)

    fun setForceGles(context: Context, value: Boolean) {
        context.getSharedPreferences("poc3d", Context.MODE_PRIVATE)
            .edit().putBoolean("force_gles", value).apply()
    }

    val engine: Engine by lazy {
        val ctx = appContext
        if (ctx != null && forceGles(ctx)) Engine.create(Engine.Backend.OPENGL)
        else Engine.create()
    }

    private val materialProvider by lazy { UbershaderProvider(engine) }
    private val loader by lazy { AssetLoader(engine, materialProvider, EntityManager.get()) }
    private val resourceLoader by lazy { ResourceLoader(engine) }

    private var asset: FilamentAsset? = null
    private var ibl: IndirectLight? = null
    private var iblTried = false

    fun indirectLight(): IndirectLight? {
        if (!iblTried) {
            iblTried = true
            ibl = runCatching {
                val ctx = appContext ?: return null
                val buf = directBuffer(ctx, IBL_PATH) ?: return null
                val tex = KTX1Loader.createTexture(
                    engine, buf, KTX1Loader.Options().apply { srgb = true }
                )
                IndirectLight.Builder()
                    .reflections(tex)
                    .irradiance(3, STUDIO_SH)
                    .intensity(30000f)
                    .build(engine)
            }.getOrNull()
        }
        return ibl
    }

    fun asset(): FilamentAsset? {
        asset?.let { return it }
        val ctx = appContext ?: return null
        val buf = directBuffer(ctx, MODEL_PATH) ?: return null
        val loaded = runCatching {
            val a = loader.createAsset(buf) ?: return null
            resourceLoader.loadResources(a)
            a.releaseSourceData()
            a
        }.getOrNull() ?: return null
        asset = loaded
        return loaded
    }

    fun backendName(): String = runCatching { engine.backend.name }.getOrDefault("?")
}

private fun directBuffer(context: Context, path: String): ByteBuffer? = runCatching {
    context.assets.open(path).use { input ->
        val bytes = input.readBytes()
        ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }
    }
}.getOrNull()

// ---------------------------------------------------------------- 矩阵工具

private fun identity(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

/** 列主序相乘：out = a * b。 */
private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(16)
    for (c in 0 until 4) {
        for (r in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) sum += a[k * 4 + r] * b[c * 4 + k]
            out[c * 4 + r] = sum
        }
    }
    return out
}

private fun rotationY(degrees: Float): FloatArray {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return floatArrayOf(
        c, 0f, -s, 0f,
        0f, 1f, 0f, 0f,
        s, 0f, c, 0f,
        0f, 0f, 0f, 1f
    )
}

private fun rotationX(degrees: Float): FloatArray {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, c, s, 0f,
        0f, -s, c, 0f,
        0f, 0f, 0f, 1f
    )
}

private fun scaling(s: Float): FloatArray = floatArrayOf(
    s, 0f, 0f, 0f,
    0f, s, 0f, 0f,
    0f, 0f, s, 0f,
    0f, 0f, 0f, 1f
)

private fun translation(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f
)

// ---------------------------------------------------------------- 渲染器

/**
 * 一个 TextureView 对应一个渲染器。Scene / View / Camera 随页面销毁，
 * Engine 与 glb、IBL 复用。
 */
private class Badge3DRenderer(
    private val textureView: TextureView,
    private val startNanos: Long,
    private val onStats: (String) -> Unit
) {
    private val engine = Badge3D.engine
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        setOpaque(false)
    }
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val camera: Camera = engine.createCamera(EntityManager.get().create())

    private var swapChain: SwapChain? = null
    private var running = false
    private var rootInstance = 0
    private var baseMatrix = identity()
    private var objectRadius = 1f

    // 交互状态
    private var yaw = 24f
    private var pitch = -10f
    private var spinVelocity = 0f
    private var cameraDistance = 2.35f
    private var pinchBase = 1f
    private var lastTouchAt = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var multiTouch = false

    // 统计
    private var firstFrameMs = -1.0
    private var lastFrameNanos = 0L
    private var prevReportNanos = 0L
    private var frameAccum = 0.0
    private var frameCount = 0
    private var reportedAt = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            renderFrame(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        // appContext 必须在 buildScene（读 assets）之前就位，否则会静默判定为加载失败
        Badge3D.appContext = textureView.context.applicationContext

        uiHelper.setRenderCallback(object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: android.view.Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                }
                swapChain = null
            }

            override fun onResized(width: Int, height: Int) {
                if (width <= 0 || height <= 0) return
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(
                    32.0,
                    width.toDouble() / height.toDouble(),
                    0.05,
                    50.0,
                    Camera.Fov.VERTICAL
                )
            }
        })

        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }

        buildScene()

        running = true
        uiHelper.attachTo(textureView)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        uiHelper.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    private fun buildScene() {
        val asset = Badge3D.asset()
        if (asset == null) {
            onStats("模型加载失败")
            return
        }

        scene.addEntities(asset.entities)
        Badge3D.indirectLight()?.let { scene.indirectLight = it }

        // 主光：左上前方的暖白，负责金属高光
        val key = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.96f, 0.90f)
            .intensity(65000f)
            .direction(-0.38f, -0.72f, -0.58f)
            .castShadows(false)
            .build(engine, key)
        scene.addEntity(key)

        // 轮廓光：右后方冷光，把外框边缘从背景里挑出来
        val rim = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.62f, 0.78f, 1.0f)
            .intensity(22000f)
            .direction(0.55f, -0.15f, 0.82f)
            .castShadows(false)
            .build(engine, rim)
        scene.addEntity(rim)

        // 归一化：把模型缩到约 1 单位大小并居中，相机距离才有统一含义
        val bbox = asset.boundingBox
        val center = bbox.center
        val half = bbox.halfExtent
        val maxHalf = max(half[0], max(half[1], half[2])).coerceAtLeast(1e-5f)
        val scale = 0.5f / maxHalf
        objectRadius = maxHalf * scale
        baseMatrix = multiply(
            scaling(scale),
            translation(-center[0], -center[1], -center[2])
        )

        val tm = engine.transformManager
        rootInstance = tm.getInstance(asset.root)
        if (rootInstance != 0) tm.setTransform(rootInstance, baseMatrix)

        view.scene = scene
        view.camera = camera
        camera.setProjection(32.0, 1.0, 0.05, 50.0, Camera.Fov.VERTICAL)
        applyCamera()
    }

    private fun applyCamera() {
        // 相机在 +Z 轴上，向原点看；徽章正面朝 +Z，因此初始是正视
        camera.setModelMatrix(translation(0f, 0f, cameraDistance))
    }

    private fun applyTransform() {
        if (rootInstance == 0) return
        val rotation = multiply(rotationY(yaw), rotationX(pitch))
        engine.transformManager.setTransform(rootInstance, multiply(rotation, baseMatrix))
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val sc = swapChain ?: return
        advance(frameTimeNanos)

        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
            if (firstFrameMs < 0.0) {
                firstFrameMs = (System.nanoTime() - startNanos) / 1e6
            }
        }
        report(frameTimeNanos)
    }

    private fun advance(frameTimeNanos: Long) {
        val now = System.currentTimeMillis()
        val dt = if (lastFrameNanos == 0L) 0.016f
        else ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = frameTimeNanos

        if (!dragging) {
            // 惯性衰减 → 静止 3 秒后自动慢转（"活着"的观感主要来自这里）
            if (abs(spinVelocity) > 0.02f) {
                yaw += spinVelocity * dt * 60f
                spinVelocity *= 0.92f
            } else if (now - lastTouchAt > 3000L) {
                yaw += 12f * dt
                spinVelocity = 0f
            }
        }
        applyTransform()
        applyCamera()
        if (yaw > 360f) yaw -= 360f
        if (yaw < -360f) yaw += 360f
    }

    private fun report(frameTimeNanos: Long) {
        if (prevReportNanos != 0L) {
            frameAccum += (frameTimeNanos - prevReportNanos) / 1e6
            frameCount++
        }
        prevReportNanos = frameTimeNanos

        val nowMs = System.currentTimeMillis()
        if (reportedAt == 0L) reportedAt = nowMs
        if (nowMs - reportedAt < 500L || frameCount == 0) return

        val ms = frameAccum / frameCount
        val fps = if (ms > 0.0) 1000.0 / ms else 0.0
        onStats(
            "%.1f fps · %.1f ms/帧 · 首帧 %.0f ms · %s".format(
                fps, ms, firstFrameMs.coerceAtLeast(0.0), Badge3D.backendName()
            )
        )
        frameAccum = 0.0
        frameCount = 0
        reportedAt = nowMs
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                multiTouch = false
                lastX = event.x
                lastY = event.y
                pinchBase = distanceOf(event)
                lastTouchAt = System.currentTimeMillis()
                spinVelocity = 0f
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                multiTouch = true
                pinchBase = distanceOf(event)
            }

            MotionEvent.ACTION_MOVE -> {
                lastTouchAt = System.currentTimeMillis()
                if (multiTouch && event.pointerCount >= 2) {
                    val d = distanceOf(event)
                    if (pinchBase > 0f && d > 0f) {
                        cameraDistance = (cameraDistance * (pinchBase / d))
                            .coerceIn(1.65f, 3.6f)
                        pinchBase = d
                    }
                } else {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    yaw -= dx * 0.42f
                    pitch = (pitch - dy * 0.26f).coerceIn(-26f, 26f)
                    spinVelocity = -dx * 0.42f
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                multiTouch = false
                lastTouchAt = System.currentTimeMillis()
            }
        }
        return true
    }

    private fun distanceOf(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
    }
}

// ---------------------------------------------------------------- Compose 封装

@Composable
private fun Badge3DView(
    modifier: Modifier = Modifier,
    onStats: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var renderer by remember { mutableStateOf<Badge3DRenderer?>(null) }

    DisposableEffect(Unit) {
        Badge3D.appContext = context.applicationContext
        onDispose { renderer?.stop() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                val t0 = System.nanoTime()
                val r = Badge3DRenderer(this, t0, onStats)
                renderer = r
                setOnTouchListener { _, event -> r.onTouch(event) }
                r.start()
            }
        },
        onRelease = { renderer?.stop() }
    )
}

/**
 * POC 页面：简略（小窗）+ 详细（全屏 + 背景）两种形态共用同一套渲染。
 *
 * 两种形态**不会同时存在**——同一时刻只有一个 TextureView、一个 swapchain。
 */
@Composable
fun Badge3DPocScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf("等待首帧…") }
    var forceGles by remember { mutableStateOf(Badge3D.forceGles(context)) }

    if (!detail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ascent.Bg)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← 返回",
                    color = Ascent.Fg2,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onBack() }
                        .padding(end = 12.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "3D 徽章 POC",
                        color = Ascent.Fg,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("简略展示 · 160dp 小窗", color = Ascent.Fg3, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Ascent.Surface)
            ) {
                Badge3DView(
                    modifier = Modifier.fillMaxSize(),
                    onStats = { stats = it }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(stats, color = Ascent.Fg3, fontSize = 11.sp)
            Text(
                "单指横拖旋转 · 竖拖俯仰 · 双指捏合缩放 · 静止 3 秒自动慢转",
                color = Ascent.Fg3,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
            POC_Button(
                text = if (forceGles) "后端：GLES（点此切回默认）" else "后端：默认/Vulkan（点此强制 GLES）",
                onClick = {
                    forceGles = !forceGles
                    Badge3D.setForceGles(context, forceGles)
                    stats = "已切换，需重启 App 生效"
                }
            )

            Spacer(Modifier.height(18.dp))
            POC_Button("进入详细展示", onClick = { detail = true })
            Spacer(Modifier.height(10.dp))
            POC_Button("返回", onClick = onBack)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // 专属背景：POC 先用同色系渐变占位，验证 3D 能否透明叠在普通页面之上
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF10202A), Color(0xFF060C11))
                        )
                    )
            )

            Badge3DView(
                modifier = Modifier.fillMaxSize(),
                onStats = { stats = it }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "FIRST STEP",
                    color = Color(0xFFE8F3F7),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text("迈出第一步 · Common", color = Color(0xFF8FB3C0), fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text(stats, color = Color(0xFF6E8E9B), fontSize = 10.sp)
                Spacer(Modifier.height(14.dp))
                POC_Button("退出详细展示", onClick = { detail = false })
            }

            Text(
                text = "← 返回",
                color = Color(0xFFBBD3DC),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { detail = false }
            )
        }
    }
}

@Composable
private fun POC_Button(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Ascent.AccentSoft)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Ascent.Fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

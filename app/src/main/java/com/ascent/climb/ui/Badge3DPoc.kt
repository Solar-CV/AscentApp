package com.ascent.climb.ui

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.google.android.filament.Filament
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
import com.google.android.filament.gltfio.Gltfio
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
    2.088282f, 2.282543f, 2.600507f,
    0.995556f, 1.079737f, 1.173898f,
    0.507665f, 0.507686f, 0.504159f,
    -0.059955f, -0.011492f, 0.053057f,
    -0.144433f, -0.119596f, -0.084267f,
    0.485929f, 0.498635f, 0.498646f,
    -0.196516f, -0.208764f, -0.228055f,
    -0.184369f, -0.136366f, -0.070259f,
    -0.449479f, -0.413325f, -0.372863f,
)

/** 引擎级单例：Engine / IBL / glb 只建一次，两个展示形态共用。 */
private object Badge3D {
    var appContext: Context? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences("poc3d", Context.MODE_PRIVATE)

    /** 默认 GLES。Mali（麒麟）的 Vulkan 驱动容易在原生层直接 abort，POC 先走稳的一条。 */
    fun useVulkan(context: Context) = prefs(context).getBoolean("use_vulkan", false)
    fun setUseVulkan(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("use_vulkan", value).apply()

    /** 环境光开关：用来隔离「崩溃是不是 IBL 引起的」。 */
    fun useIbl(context: Context) = prefs(context).getBoolean("use_ibl", true)
    fun setUseIbl(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("use_ibl", value).apply()

    /**
     * 阶段埋点。原生 abort（SIGABRT/SIGSEGV）拿不到 Java 栈，
     * 这里每一步立即 commit 到 SharedPreferences，重进页面即可看到死在哪。
     */
    fun markStage(context: Context, stage: String) {
        prefs(context).edit().putString("last_stage", stage).commit()
    }

    fun lastStage(context: Context): String =
        prefs(context).getString("last_stage", "（无记录）") ?: "（无记录）"

    val engine: Engine by lazy {
        initJni()
        val ctx = appContext
        if (ctx != null && useVulkan(ctx)) Engine.create(Engine.Backend.VULKAN)
        else Engine.create(Engine.Backend.OPENGL)
    }

    /**
     * 三个 jni 库都要显式加载，Filament 不会自动 loadLibrary：
     *   filament-jni（Filament.init）、gltfio-jni（Gltfio.init）、filament-utils-jni（无 init 入口）
     * 漏掉任何一个都会在用到时才抛 UnsatisfiedLinkError，表现为"点进去就崩"。
     */
    private fun initJni() {
        Filament.init()
        Gltfio.init()
        runCatching { System.loadLibrary("filament-utils-jni") }
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
            val ctx = appContext
            ibl = runCatching {
                if (ctx == null) return null
                if (!useIbl(ctx)) return null
                val buf = directBuffer(ctx, IBL_PATH) ?: return null
                val tex = KTX1Loader.createTexture(
                    engine, buf, KTX1Loader.Options().apply { srgb = true }
                )
                IndirectLight.Builder()
                    .reflections(tex)
                    .irradiance(3, STUDIO_SH)
                    .intensity(30000f)
                    .build(engine)
            }.onSuccess { android.util.Log.i("POC3D", "IBL ok: ${it != null}") }.onFailure {
                if (ctx != null) {
                    markStage(ctx, "X IBL 失败: ${it.javaClass.simpleName}: ${it.message}")
                    android.util.Log.e("POC3D", "IBL failed", it)
                }
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

private fun rotationZ(degrees: Float): FloatArray {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return floatArrayOf(
        c, s, 0f, 0f,
        -s, c, 0f, 0f,
        0f, 0f, 1f, 0f,
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
    private var yaw = 18f
    private var pitch = -10f
    private var spinVelocity = 0f
    private var autoSpin = true
    private var zoom = 1f
    private var lastAspect = 1f
    private var pinchBase = 1f
    private var lastTouchAt = System.currentTimeMillis()
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
        val app = textureView.context.applicationContext
        // appContext 必须在 buildScene（读 assets）之前就位，否则会静默判定为加载失败
        Badge3D.appContext = app
        Badge3D.markStage(app, "2 渲染器已建（Engine $badgeBackendLabel）")

        uiHelper.setRenderCallback(object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: android.view.Surface) {
                Badge3D.markStage(app, "6 拿到 Surface")
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
                Badge3D.markStage(app, "7 SwapChain 已建")
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
                android.util.Log.i("POC3D", "onResized ${width}x${height} aspect=${width.toFloat()/height}")
                view.viewport = Viewport(0, 0, width, height)
                lastAspect = width.toFloat() / height.toFloat()
                camera.setProjection(32.0, lastAspect.toDouble(), 0.05, 50.0, Camera.Fov.VERTICAL)
                applyCamera()
            }
        })

        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }

        runCatching { buildScene() }.onFailure {
            Badge3D.markStage(app, "X 场景构建失败: ${it.javaClass.simpleName}")
            onStats("场景构建失败：${it.message}")
            return
        }

        running = true
        uiHelper.attachTo(textureView)
        Badge3D.markStage(app, "8 已挂载，等待首帧")
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private val badgeBackendLabel: String get() = runCatching { Badge3D.backendName() }.getOrDefault("?")

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        uiHelper.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    private fun buildScene() {
        val app = textureView.context.applicationContext
        val asset = Badge3D.asset()

        // 无论模型是否加载成功，view 必须带相机，否则渲染器会在原生层直接 abort
        view.scene = scene
        view.camera = camera
        camera.setProjection(32.0, 1.0, 0.05, 50.0, Camera.Fov.VERTICAL)
        applyCamera()

        if (asset == null) {
            Badge3D.markStage(app, "X glb 加载失败")
            onStats("glb 加载失败")
            return
        }
        Badge3D.markStage(app, "3 glb 已加载")

        scene.addEntities(asset.entities)
        val ibl = Badge3D.indirectLight()
        ibl?.let { scene.indirectLight = it }
        Badge3D.markStage(app, if (ibl != null) "4 IBL 已建" else "4 IBL 跳过")

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

        // 归一化 + 姿态修正：
        // Blender 是 Z-up，glTF 导出后模型躺在 XZ 平面（正面朝 +Y），
        // 这里绕 X 轴 +90° 把它立起来，正面朝向相机（+Z），浮雕朝观众凸出。
        val bbox = asset.boundingBox
        val center = bbox.center
        val half = bbox.halfExtent
        val maxHalf = max(half[0], max(half[1], half[2])).coerceAtLeast(1e-5f)
        val scale = 0.5f / maxHalf
        objectRadius = maxHalf * scale
        android.util.Log.i(
            "POC3D",
            "bbox center=${center.toList()} half=${half.toList()} scale=$scale"
        )
        val fix = runCatching {
            ((textureView.context as? android.app.Activity)?.intent?.getStringExtra("fix")) ?: "x+"
        }.getOrDefault("x+")
        val fixMatrix = when (fix) {
            "none" -> identity()
            "x-" -> rotationX(-90f)
            "y" -> rotationY(90f)
            "z" -> rotationZ(90f)
            "x-" -> rotationX(-90f)
            else -> rotationX(90f)
        }
        android.util.Log.i("POC3D", "fix=$fix")
        baseMatrix = multiply(
            fixMatrix,
            multiply(scaling(scale), translation(-center[0], -center[1], -center[2]))
        )

        val tm = engine.transformManager
        rootInstance = tm.getInstance(asset.root)
        if (rootInstance != 0) tm.setTransform(rootInstance, baseMatrix)
        Badge3D.markStage(app, "5 场景已就绪（三角面 ${asset.entities.size} 实体）")
    }

    /**
     * 按视口宽高比自适应取景：竖屏时水平方向更窄，必须用水平约束反推距离，
     * 否则徽章会在左右被裁掉（POC 第一版就是这么翻车的）。
     */
    private fun applyCamera() {
        val halfTan = tan(Math.toRadians(32.0 / 2.0)).toFloat()
        val aspect = lastAspect.coerceIn(0.2f, 5f)
        val fitY = 0.5f / halfTan
        val fitX = 0.5f / (halfTan * aspect)
        val dist = max(fitY, fitX) * 1.18f * zoom
        camera.setModelMatrix(translation(0f, 0f, dist))
    }

    private fun applyTransform() {
        if (rootInstance == 0) return
        val rotation = multiply(rotationY(yaw), rotationX(pitch))
        engine.transformManager.setTransform(rootInstance, multiply(rotation, baseMatrix))
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val sc = swapChain ?: return
        advance(frameTimeNanos)

        if (!view.hasCamera()) return
        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
            if (firstFrameMs < 0.0) {
                firstFrameMs = (System.nanoTime() - startNanos) / 1e6
                Badge3D.markStage(
                    textureView.context.applicationContext,
                    "OK 首帧完成（%.0f ms）".format(firstFrameMs)
                )
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
            } else if (autoSpin && now - lastTouchAt > 3000L) {
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
                        zoom = (zoom * (pinchBase / d)).coerceIn(0.8f, 1.8f)
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
            val app = ctx.applicationContext
            // 埋点要在 Engine 创建（渲染器构造）之前落盘
            Badge3D.markStage(app, "1 开始创建渲染器")
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
    var useVulkan by remember { mutableStateOf(Badge3D.useVulkan(context)) }
    var useIbl by remember { mutableStateOf(Badge3D.useIbl(context)) }
    // 上一次会话留下的阶段记录：原生崩溃时 Java 栈拿不到，只能靠它定位
    val lastStage = remember { Badge3D.lastStage(context) }
    val scroll = rememberScrollState()

    if (!detail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ascent.Bg)
                .verticalScroll(scroll)
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
            Text("简略展示 · 200dp 小窗（可拖动）", color = Ascent.Fg3, fontSize = 11.sp)
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
            Text(
                "上次最后阶段：$lastStage",
                color = Ascent.Fg3,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(8.dp))
            POC_Button(
                text = if (useVulkan) "后端：Vulkan（点此改回 GLES）" else "后端：GLES（点此改 Vulkan）",
                onClick = {
                    useVulkan = !useVulkan
                    Badge3D.setUseVulkan(context, useVulkan)
                    stats = "后端已改为 ${if (useVulkan) "Vulkan" else "GLES"}，重启 App 生效"
                }
            )
            Spacer(Modifier.height(8.dp))
            POC_Button(
                text = if (useIbl) "环境光 IBL：开（点此关闭）" else "环境光 IBL：关（点此开启）",
                onClick = {
                    useIbl = !useIbl
                    Badge3D.setUseIbl(context, useIbl)
                    stats = "IBL 已${if (useIbl) "开启" else "关闭"}，重启 App 生效"
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

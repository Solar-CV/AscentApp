package com.ascent.climb.ui

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Camera
import com.ascent.climb.R
import com.ascent.climb.data.achievementById
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

/**
 * 成就 id → glb 资源。没有登记的成就自动回退「图片假 3D」。
 * 当前 28 枚有模型，其余 12 枚仍用成就图标。
 */
private val MODEL_OF_ACHIEVEMENT: Map<String, String> = mapOf(
    "ams_guardian" to "badges/ams_guardian.glb",
    "avalanche_eye" to "badges/avalanche_eye.glb",
    "basecamp_grad" to "badges/basecamp_grad.glb",
    "crampon_ready" to "badges/crampon_ready.glb",
    "curious_dev" to "badges/curious_dev.glb",
    "first_steps" to "badges/first_steps.glb",
    "knot_tied" to "badges/knot_tied.glb",
    "peak_scout" to "badges/peak_scout.glb",
    "perfect_1" to "badges/perfect_1.glb",
    "perfect_15" to "badges/perfect_15.glb",
    "perfect_30" to "badges/perfect_30.glb",
    "perfect_5" to "badges/perfect_5.glb",
    "retreat_wise" to "badges/retreat_wise.glb",
    "review_20" to "badges/review_20.glb",
    "review_clear" to "badges/review_clear.glb",
    "review_first" to "badges/review_first.glb",
    "self_arrest" to "badges/self_arrest.glb",
    "seven_k_grad" to "badges/seven_k_grad.glb",
    "six_k_grad" to "badges/six_k_grad.glb",
    "streak_14" to "badges/streak_14.glb",
    "streak_3" to "badges/streak_3.glb",
    "streak_30" to "badges/streak_30.glb",
    "streak_7" to "badges/streak_7.glb",
    "tech_grad" to "badges/tech_grad.glb",
    "trail_camp" to "badges/trail_camp.glb",
    "trail_finish" to "badges/trail_finish.glb",
    "trail_first" to "badges/trail_first.glb",
    "trail_high" to "badges/trail_high.glb",)

/** 默认模型（POC Activity / 调试直连时用）。 */
private const val MODEL_PATH = "badges/first_steps.glb"

/** 提升 glTF 自带点/聚光灯的强度，使其在当前场景曝光下可见。 */
private const val PUNCTUAL_LIGHT_GAIN = 5f
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

    private val assetCache = HashMap<String, FilamentAsset>()
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
                    .intensity(38000f)
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

    fun asset(path: String): FilamentAsset? {
        assetCache[path]?.let { return it }
        val ctx = appContext ?: return null
        val buf = directBuffer(ctx, path) ?: return null
        val loaded = runCatching {
            val a = loader.createAsset(buf) ?: return null
            resourceLoader.loadResources(a)
            a.releaseSourceData()
            a
        }.getOrNull() ?: return null
        assetCache[path] = loaded
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
    private val modelPath: String,
    private val startNanos: Long,
    private val onStats: (String) -> Unit,
    private val onReady: () -> Unit
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
    private var spinPhase = 0f
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
    private var slowFrames = 0
    private var bloomDropped = false
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
        // 透明底：关掉后处理（tonemap 会把 alpha 写成 1），并按半透明合成
        view.blendMode = View.BlendMode.TRANSLUCENT

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
        val asset = Badge3D.asset(modelPath)

        // 无论模型是否加载成功，view 必须带相机，否则渲染器会在原生层直接 abort
        view.scene = scene
        view.camera = camera
        // 泛光：自发光（灯笼 / 星点 / 镜片）才真正"发光"，否则只是一块亮色
        view.bloomOptions = View.BloomOptions().apply {
            enabled = true
            // 泛光只用来让自发光（灯笼 / 星点 / 镜片）溢出成光晕，
            // 层级与分辨率都压到最低：低端 GPU 上这是最贵的一步
            strength = 0.05f
            threshold = true
            levels = 3
            resolution = 180
            quality = View.QualityLevel.LOW
        }
        camera.setProjection(32.0, 1.0, 0.05, 50.0, Camera.Fov.VERTICAL)
        applyCamera()

        if (asset == null) {
            Badge3D.markStage(app, "X glb 加载失败")
            onStats("glb 加载失败")
            return
        }
        Badge3D.markStage(app, "3 glb 已加载")

        scene.addEntities(asset.entities)
        // glTF 的 KHR_lights_punctual（例如 peak_scout 观测镜的聚光）不会随
        // 根实体自动进场景，必须单独把光源实体加进去，否则模型自带的光就丢了。
        runCatching {
            val lights = asset.lightEntities
            android.util.Log.i(
                "POC3D",
                "entities=${asset.entities.size} lights=${lights.size} renderables=${asset.renderableEntities.size}"
            )
            val lm = engine.lightManager
            lights.forEach { entity ->
                scene.addEntity(entity)
                // 关键：模型里的聚光灯是按 Blender 场景曝光的（如 54 cd），
                // 而本场景主光是 66000 lux，量级差几个数量级，不放大等于没开。
                val inst = lm.getInstance(entity)
                if (inst != 0) {
                    val raw = lm.getIntensity(inst)
                    lm.setIntensity(inst, raw * PUNCTUAL_LIGHT_GAIN)
                    val pos = lm.getPosition(inst, FloatArray(3))
                    val dir = lm.getDirection(inst, FloatArray(3))
                    // 位置/朝向直接决定光斑能不能落到徽章上，打点便于真机排查
                    android.util.Log.i(
                        "POC3D",
                        "light type=${lm.getType(inst)} raw=$raw -> ${raw * PUNCTUAL_LIGHT_GAIN} " +
                            "pos=${pos.toList()} dir=${dir.toList()}"
                    )
                }
            }
        }.onFailure { android.util.Log.e("POC3D", "light entity failure", it) }
        val ibl = Badge3D.indirectLight()
        ibl?.let { scene.indirectLight = it }
        Badge3D.markStage(app, if (ibl != null) "4 IBL 已建" else "4 IBL 跳过")

        // 主光：左上前方的暖白，负责金属高光
        val key = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.96f, 0.90f)
            .intensity(66000f)
            .direction(-0.38f, -0.72f, -0.58f)
            .castShadows(false)
            .build(engine, key)
        scene.addEntity(key)

        // 轮廓光：右后方冷光，把外框边缘从背景里挑出来
        val rim = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.62f, 0.78f, 1.0f)
            .intensity(26000f)
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
                onReady()
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
                // 自动展示只在正面 ±22° 摆动：模型背面是一块素板，转过去不好看；
                // 手动拖动仍可看完整 360°。
                spinPhase += dt
                yaw = 18f + sin(spinPhase * 0.9f) * 22f
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
        // 低端机兜底：帧时间持续拉垮就先砍泛光（它是这条链路里最贵的一步），
        // 宁可少一层光晕，也不要为了光晕掉到 30fps 以下。
        if (!bloomDropped && ms > 28.0) {
            slowFrames++
            if (slowFrames >= 2) {
                bloomDropped = true
                view.bloomOptions = View.BloomOptions().apply { enabled = false }
                android.util.Log.i("POC3D", "bloom disabled for perf, ${ms}ms/frame")
            }
        } else if (ms <= 28.0) {
            slowFrames = 0
        }
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
                    // 反向手感：向右/向下拖动，徽章朝同方向转
                    yaw += dx * 0.42f
                    pitch = (pitch + dy * 0.26f).coerceIn(-26f, 26f)
                    spinVelocity = dx * 0.42f
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

private val INK = Color(0xFF14232A)
private val INK2 = Color(0xFF455963)
private val INK3 = Color(0xFF6E808A)

/** 真实 3D 视图：一个 TextureView + 一套渲染器。 */
@Composable
private fun Badge3DView(
    modelPath: String,
    modifier: Modifier = Modifier,
    onStats: (String) -> Unit = {},
    onReady: () -> Unit = {}
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
                val r = Badge3DRenderer(this, modelPath, t0, onStats, onReady)
                renderer = r
                setOnTouchListener { _, event -> r.onTouch(event) }
                r.start()
            }
        },
        onRelease = { renderer?.stop() }
    )
}

/**
 * 图片假 3D：没有 glb 的成就先用这张定稿图做透视旋转。
 *
 * 用 graphicsLayer 的 rotationX/rotationY + cameraDistance 得到真透视，
 * 不用建模型也不需要引擎；摆动限制在 ±55°，避免转出背面（背面会是镜像）。
 */
@Composable
private fun FakeBadge3D(res: Int, modifier: Modifier = Modifier) {
    var yaw by remember { mutableFloatStateOf(16f) }
    var pitch by remember { mutableFloatStateOf(-8f) }
    var auto by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        var t0 = 0L
        while (true) {
            withFrameNanos { now ->
                if (t0 == 0L) t0 = now
                if (auto && System.currentTimeMillis() - lastTouch > 3000L) {
                    val t = (now - t0) / 1e9f
                    yaw = 16f + sin(t * 0.8f) * 24f
                }
            }
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    auto = false
                    lastTouch = System.currentTimeMillis()
                },
                onDragEnd = { lastTouch = System.currentTimeMillis() },
                onDragCancel = { lastTouch = System.currentTimeMillis() },
                onDrag = { change, drag ->
                    change.consume()
                    // 与真实 3D 同一套反向手感
                    yaw = (yaw + drag.x * 0.35f).coerceIn(-55f, 55f)
                    pitch = (pitch + drag.y * 0.22f).coerceIn(-30f, 30f)
                }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                // 定稿图自带深色底，直接转会像贴了块黑方片；
                // 圆角 + 投影把它变成一张收藏卡片，和亮背景才不打架。
                .shadow(20.dp, RoundedCornerShape(26.dp))
                .graphicsLayer {
                    rotationY = yaw
                    rotationX = pitch
                    cameraDistance = 6f * density
                }
                .clip(RoundedCornerShape(26.dp))
        )
    }
}

/**
 * 统一的徽章展示入口：有 glb 走实时 3D，没有则用定稿图做假 3D。
 * 解锁弹窗、成就测试页、展示页都走这一个函数，避免三处各写一套。
 */
/** 该成就是否已有 3D 模型（决定要不要垫浅色底盘）。 */
fun badgeHasModel(achievementId: String): Boolean = MODEL_OF_ACHIEVEMENT.containsKey(achievementId)

@Composable
fun Badge3DAuto(
    achievementId: String,
    modifier: Modifier = Modifier,
    onStats: (String) -> Unit = {},
    onReady: () -> Unit = {}
) {
    val modelPath = MODEL_OF_ACHIEVEMENT[achievementId]
    if (modelPath != null) {
        Badge3DView(modelPath, modifier, onStats, onReady)
        return
    }
    val res = badgeRes(achievementId) ?: return
    LaunchedEffect(Unit) { onReady() }
    FakeBadge3D(res, modifier)
}

/**
 * 3D 徽章展示页：同一个入口既服务「已有 glb 的成就」，也服务「只有定稿图的成就」。
 *
 * 背景是重模糊的亮色风景（res/drawable-nodpi/badge_bg.jpg），
 * 3D 视图用透明 TextureView 叠在上面，因此背景与徽章是两层独立合成。
 */
@Composable
fun Badge3DStageScreen(
    achievementId: String,
    onBack: () -> Unit
) {
    val ach = remember(achievementId) { achievementById(achievementId) }
    val modelPath = MODEL_OF_ACHIEVEMENT[achievementId]
    var stats by remember { mutableStateOf("") }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.badge_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 极淡白纱：让暗色金属与深色文字都压得住
        Box(modifier = Modifier.fillMaxSize().background(Color(0x12FFFFFF)))

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← 返回",
                    color = INK2,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                SoftTag(
                    text = if (modelPath != null) "真实 3D" else "图片 3D",
                    foreground = if (modelPath != null) Color(0xFF17603F) else Color(0xFF4A5560),
                    background = if (modelPath != null) Color(0x2E1D9E75) else Color(0x24142028)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (modelPath != null) {
                    Badge3DView(
                        modelPath = modelPath,
                        modifier = Modifier.fillMaxSize(),
                        onStats = { stats = it }
                    )
                } else {
                    val res = badgeRes(achievementId)
                    if (res != null) {
                        FakeBadge3D(
                            res = res,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 36.dp, vertical = 24.dp)
                        )
                    } else {
                        Text("该成就暂无图形资源", color = INK2, fontSize = 13.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 26.dp)
            ) {
                Text(
                    text = ach?.let { achievementTitle(it.id, it.title) } ?: achievementId,
                    color = INK,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                if (ach != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(rarityColors(ach.rarity).face)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(rarityName(ach.rarity), color = INK2, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = ach.conditionText,
                        color = INK2,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                if (modelPath != null && stats.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stats, color = INK2, fontSize = 10.sp)
                }
            }
        }
    }
}

package com.ascent.climb.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * 自动天气：粗定位 → Open-Meteo 当前天气 → 映射成 [WeatherMood]。
 *
 * 三条自我约束：
 *  1. **只在用户主动开启「自动天气」时才定位**，并且只请求一次单次定位，不留监听。
 *  2. 只用 `ACCESS_COARSE_LOCATION`（约 3 km），天气按网格取值，精度足够。
 *  3. 结果连同坐标一起缓存在本机，180 分钟内不重复请求；坐标只作为缓存键，
 *     不写账号、不上传服务端。
 *
 * 数据来源必须署名（CC BY 4.0）：界面上那行 `Weather data by Open-Meteo.com`
 * 不是装饰，删掉就违反许可。
 */
object WeatherAuto {

    private const val PREFS = "ascent_weather"
    private const val KEY_MOOD = "mood"
    private const val KEY_AT = "at"
    private const val KEY_CODE = "code"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"

    /** 缓存有效期：天气主题不需要分钟级准确，3 小时足够；也避免频繁把坐标发出去。 */
    private const val TTL_MS = 180 * 60 * 1000L

    /** 位置移动超过这个距离就重新取（约 5 km）。 */
    private const val MOVE_DEG = 0.05

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cached(context: Context): WeatherMood? {
        val p = prefs(context)
        val key = p.getString(KEY_MOOD, null) ?: return null
        if (System.currentTimeMillis() - p.getLong(KEY_AT, 0L) > TTL_MS) return null
        return WeatherMood.of(key)
    }

    fun lastUpdated(context: Context): Long = prefs(context).getLong(KEY_AT, 0L)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * 取一次天气。缓存还新、位置也没怎么动就直接返回缓存，不发请求。
     * 任何一步失败都返回 null —— 调用方保留上一次的天气，不要弹错误。
     *
     * [force] 为真时跳过 180 分钟的缓存判断（调试标签点击时用，验证真实返回值）；
     * 位置仍会重新读一次，请求也照发。
     */
    suspend fun refresh(context: Context, force: Boolean = false): WeatherMood? {
        if (!hasPermission(context)) return cached(context)

        val here = locate(context) ?: return cached(context)

        val p = prefs(context)
        val cachedAt = p.getLong(KEY_AT, 0L)
        val oldLat = p.getString(KEY_LAT, null)?.toDoubleOrNull()
        val oldLon = p.getString(KEY_LON, null)?.toDoubleOrNull()
        val dLat = if (oldLat == null) 999.0 else kotlin.math.abs(here.first - oldLat)
        val dLon = if (oldLon == null) 999.0 else kotlin.math.abs(here.second - oldLon)
        val fresh = System.currentTimeMillis() - cachedAt < TTL_MS
        if (!force && fresh && dLat < MOVE_DEG && dLon < MOVE_DEG) return cached(context)

        val code = fetchCode(here.first, here.second) ?: return cached(context)
        val mood = moodOf(code)

        p.edit()
            .putString(KEY_MOOD, mood.key)
            .putLong(KEY_AT, System.currentTimeMillis())
            .putInt(KEY_CODE, code)
            .putString(KEY_LAT, (here.first * 100).toInt().div(100.0).toString())
            .putString(KEY_LON, (here.second * 100).toInt().div(100.0).toString())
            .apply()
        return mood
    }

    /** WMO 天气代码 → 主题。阴/雾归到 OVERCAST，雷暴单列。 */
    fun moodOf(code: Int): WeatherMood = when {
        code in 0..2 -> WeatherMood.CLEAR
        code == 3 || code == 45 || code == 48 -> WeatherMood.OVERCAST
        code in 51..67 || code in 80..82 -> WeatherMood.RAIN
        code in 95..99 -> WeatherMood.STORM
        code in 71..77 || code == 85 || code == 86 -> WeatherMood.SNOW
        else -> WeatherMood.OVERCAST
    }

    // ------------------------------------------------------------ 定位

    private suspend fun locate(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 先用最近一次已知位置（通常来自基站/WiFi，粗定位足够），新鲜的直接用。
        val last = runCatching {
            listOfNotNull(
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ).maxByOrNull { it.time }
        }.getOrNull()
        if (last != null && System.currentTimeMillis() - last.time < 6 * 60 * 60 * 1000L) {
            return last.latitude to last.longitude
        }

        // 没有可用的上次位置，就请求一次单次定位，最多等 8 秒。
        return suspendCancellableCoroutine { cont ->
            val manager = lm
            val handler = Handler(Looper.getMainLooper())
            var done = false
            var listener: android.location.LocationListener? = null

            fun finish(loc: Location?) {
                if (done) return
                done = true
                listener?.let { l -> runCatching { manager.removeUpdates(l) } }
                if (cont.isActive) cont.resume(loc?.let { it.latitude to it.longitude })
            }

            val provider = when {
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider == null) {
                finish(null)
                return@suspendCancellableCoroutine
            }

            @Suppress("DEPRECATION")
            val cb = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) = finish(location)
                override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) = finish(null)
            }
            listener = cb

            runCatching {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, cb, Looper.getMainLooper())
            }.onFailure { finish(null) }

            handler.postDelayed({ finish(null) }, 8_000L)
            cont.invokeOnCancellation { finish(null) }
        }
    }

    // ------------------------------------------------------------ 请求

    private suspend fun fetchCode(lat: Double, lon: Double): Int? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=weather_code&timezone=auto"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AscentRoam-Android")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).getJSONObject("current").optInt("weather_code", -1).takeIf { it >= 0 }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }
}

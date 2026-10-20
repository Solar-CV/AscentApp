package com.ascent.climb.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ascent.climb.R
import com.ascent.climb.data.bank.BankMsg

// 题库状态提示的文案解析。
//
// 数据层（BankRepository）只产出 BankMsg 语义码，不拼接中文；
// 这里把语义码翻译成当前语言的字符串资源。这样切换语言后，
// 「题库」区块里的提示也会跟着变，不会残留旧语言。

/** 语义码 -> 文案（Compose 版本，跟随 LocalContext 的语言）。 */
@Composable
fun bankMsgText(msg: BankMsg?): String {
    val m = msg ?: return ""
    return when (m.code) {
        "legacy" -> m.s1
        "restored" -> stringResource(R.string.bank_restored)
        "init_missing" -> stringResource(R.string.bank_not_init)
        "no_url" -> stringResource(R.string.bank_no_url)
        "need_app" -> stringResource(R.string.bank_need_app, m.n1)
        "latest" -> stringResource(R.string.bank_latest_v, m.n1)
        "download_fail" -> stringResource(R.string.bank_download_fail)
        "sha_fail" -> stringResource(R.string.bank_sha_fail)
        "format_fail" -> stringResource(R.string.bank_format_fail)
        "schema_high" -> stringResource(R.string.bank_schema_high, m.n1, m.n2)
        "updated" -> stringResource(R.string.bank_updated_v, m.n1)
        "cache_fail" -> stringResource(R.string.bank_cache_fail, m.s1)
        else -> m.s1
    }
}

/** 语义码 -> 文案（非 Composable 场景，如协程里写状态前先取文本）。 */
fun bankMsgText(context: Context, msg: BankMsg?): String {
    val m = msg ?: return ""
    return when (m.code) {
        "legacy" -> m.s1
        "restored" -> context.getString(R.string.bank_restored)
        "init_missing" -> context.getString(R.string.bank_not_init)
        "no_url" -> context.getString(R.string.bank_no_url)
        "need_app" -> context.getString(R.string.bank_need_app, m.n1)
        "latest" -> context.getString(R.string.bank_latest_v, m.n1)
        "download_fail" -> context.getString(R.string.bank_download_fail)
        "sha_fail" -> context.getString(R.string.bank_sha_fail)
        "format_fail" -> context.getString(R.string.bank_format_fail)
        "schema_high" -> context.getString(R.string.bank_schema_high, m.n1, m.n2)
        "updated" -> context.getString(R.string.bank_updated_v, m.n1)
        "cache_fail" -> context.getString(R.string.bank_cache_fail, m.s1)
        else -> m.s1
    }
}

/** 题库来源标签（内置 / 在线 vN）。 */
@Composable
fun bankSourceText(kind: String, version: Int): String =
    if (kind == "online") stringResource(R.string.bank_online_v, version)
    else stringResource(R.string.bank_bundled)

/** 把一次更新检查的结果归一为语义码，供界面解析。 */
fun bankMsgOf(r: com.ascent.climb.data.bank.UpdateCheck): BankMsg = when (r) {
    is com.ascent.climb.data.bank.UpdateCheck.Downloaded -> BankMsg("updated", r.version)
    is com.ascent.climb.data.bank.UpdateCheck.UpToDate -> BankMsg("latest", r.version)
    is com.ascent.climb.data.bank.UpdateCheck.NeedAppUpgrade -> BankMsg("need_app", r.requiredAppVersion)
    is com.ascent.climb.data.bank.UpdateCheck.Failed -> r.msg ?: BankMsg("legacy", s1 = r.message)
}

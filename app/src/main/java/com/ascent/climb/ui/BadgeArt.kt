package com.ascent.climb.ui

import com.ascent.climb.R

// 成就 id -> 徽章位图资源。
//
// 资源为 AI 渲染定稿图（256px WebP，位于 res/drawable-nodpi/），
// 由 badge-samples/clean/*.png 转换而来，命名规则 badge_<achievement_id>。
//
// 成就业务定义（Achievement.glyph 等字段）未改动，
// 该映射只服务于 UI 展示；缺失时返回 null，由调用方回退到占位徽章。
fun badgeRes(id: String): Int? = when (id) {
    "altitude_6000" -> R.drawable.badge_altitude_6000
    "ams_guardian" -> R.drawable.badge_ams_guardian
    "avalanche_eye" -> R.drawable.badge_avalanche_eye
    "basecamp_grad" -> R.drawable.badge_basecamp_grad
    "comeback" -> R.drawable.badge_comeback
    "crampon_ready" -> R.drawable.badge_crampon_ready
    "curious_dev" -> R.drawable.badge_curious_dev
    "days_21" -> R.drawable.badge_days_21
    "days_7" -> R.drawable.badge_days_7
    "eight_k_grad" -> R.drawable.badge_eight_k_grad
    "final_exam" -> R.drawable.badge_final_exam
    "first_steps" -> R.drawable.badge_first_steps
    "five_zones" -> R.drawable.badge_five_zones
    "fourteen_peaks" -> R.drawable.badge_fourteen_peaks
    "full_route" -> R.drawable.badge_full_route
    "knot_tied" -> R.drawable.badge_knot_tied
    "peak_scout" -> R.drawable.badge_peak_scout
    "perfect_1" -> R.drawable.badge_perfect_1
    "perfect_15" -> R.drawable.badge_perfect_15
    "perfect_30" -> R.drawable.badge_perfect_30
    "perfect_5" -> R.drawable.badge_perfect_5
    "retreat_wise" -> R.drawable.badge_retreat_wise
    "review_20" -> R.drawable.badge_review_20
    "review_clear" -> R.drawable.badge_review_clear
    "review_first" -> R.drawable.badge_review_first
    "runs_20" -> R.drawable.badge_runs_20
    "safety_first" -> R.drawable.badge_safety_first
    "self_arrest" -> R.drawable.badge_self_arrest
    "seven_k_grad" -> R.drawable.badge_seven_k_grad
    "six_k_grad" -> R.drawable.badge_six_k_grad
    "streak_14" -> R.drawable.badge_streak_14
    "streak_3" -> R.drawable.badge_streak_3
    "streak_30" -> R.drawable.badge_streak_30
    "streak_7" -> R.drawable.badge_streak_7
    "tech_grad" -> R.drawable.badge_tech_grad

    // v0.19.27 新增的徒步线成就：徽章还没画，先共用一张占位图。
    // 定稿后把下面几行换成各自的 badge_<id>.webp 即可，业务定义不用动。
    "trail_first" -> R.drawable.badge_trail_first
    "trail_camp" -> R.drawable.badge_trail_camp
    "trail_high" -> R.drawable.badge_trail_high
    "trail_finish" -> R.drawable.badge_placeholder_agi
    "two_tracks" -> R.drawable.badge_placeholder_agi

    else -> null
}

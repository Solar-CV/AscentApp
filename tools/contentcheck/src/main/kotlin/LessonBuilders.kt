package com.ascent.climb.data

/**
 * 新课程批次共用的题目构造器（简中首发批次使用）。
 *
 * 之所以单独成文件：新增课程分布在多个文件里，逐个文件重复定义会走样；
 * 这里统一一份，命名带 `nb` 前缀（New Batch），避免与各文件里的私有 helper 冲突。
 */

internal fun nbq(
    prompt: String,
    options: List<String>,
    correct: Int,
    explanation: String,
    safety: Boolean = false,
    context: String? = null
) = Question(
    prompt = prompt,
    options = options,
    correct = correct,
    explanation = explanation,
    safetyCritical = safety,
    context = context
)

internal fun nbMulti(
    prompt: String,
    options: List<String>,
    correctSet: List<Int>,
    explanation: String,
    safety: Boolean = false
) = Question(
    prompt = prompt,
    options = options,
    correct = -1,
    explanation = explanation,
    safetyCritical = safety,
    type = QType.Multi,
    correctSet = correctSet
)

internal fun nbOrder(
    prompt: String,
    options: List<String>,
    correctOrder: List<Int>,
    explanation: String,
    safety: Boolean = false
) = Question(
    prompt = prompt,
    options = options,
    explanation = explanation,
    safetyCritical = safety,
    type = QType.Order,
    correctOrder = correctOrder
)

internal fun nbMatch(
    prompt: String,
    left: List<String>,
    right: List<String>,
    correctOrder: List<Int>,
    explanation: String,
    safety: Boolean = false
) = Question(
    prompt = prompt,
    explanation = explanation,
    safetyCritical = safety,
    type = QType.Match,
    pairsLeft = left,
    pairsRight = right,
    correctOrder = correctOrder
)

internal fun nbFill(
    prompt: String,
    answers: List<String>,
    explanation: String,
    safety: Boolean = false
) = Question(
    prompt = prompt,
    explanation = explanation,
    safetyCritical = safety,
    type = QType.Fill,
    answers = answers
)

internal fun nbCalc(
    prompt: String,
    answer: Double,
    tolerance: Double,
    unit: String,
    explanation: String,
    safety: Boolean = false
) = Question(
    prompt = prompt,
    explanation = explanation,
    safetyCritical = safety,
    type = QType.Calc,
    answers = listOf(answer.toString()),
    tolerance = tolerance,
    unit = unit
)

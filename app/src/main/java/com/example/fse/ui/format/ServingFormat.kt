package com.example.fse.ui.format

import kotlin.math.abs
import kotlin.math.roundToInt

private val gramsRegex = Regex("""(\d+(?:[.,]\d+)?)\s*g\b""", RegexOption.IGNORE_CASE)

fun extractGrams(description: String): Double? {
    val match = gramsRegex.find(description) ?: return null
    return match.groupValues[1].replace(',', '.').toDoubleOrNull()
}

fun formatEntryAmount(servingDescription: String, multiplier: Double): String {
    val grams = extractGrams(servingDescription)
    if (grams != null) {
        val totalGrams = grams * multiplier
        return "${formatAmountNumber(totalGrams)} g"
    }
    return if (isOne(multiplier)) servingDescription else "$servingDescription ×${formatAmountNumber(multiplier)}"
}

fun formatAmountNumber(value: Double): String {
    val roundedInt = value.roundToInt()
    return if (abs(value - roundedInt) < 0.05) roundedInt.toString() else String.format("%.1f", value)
}

private fun isOne(value: Double): Boolean = abs(value - 1.0) < 0.001

package com.fitgymtrack.app.utils

/**
 * Utility per formattare i pesi con frazioni
 */
object WeightFormatter {
    /**
     * Formatta il peso per la visualizzazione
     */
    fun formatWeight(weight: Float): String {
        val wholeNumber = weight.toInt()
        val fraction = weight - wholeNumber

        return when {
            fraction == 0f -> wholeNumber.toString()
            fraction == 0.125f -> "$wholeNumber.125"
            fraction == 0.25f -> "$wholeNumber.25"
            fraction == 0.375f -> "$wholeNumber.375"
            fraction == 0.5f -> "$wholeNumber.5"
            fraction == 0.625f -> "$wholeNumber.625"
            fraction == 0.75f -> "$wholeNumber.75"
            fraction == 0.875f -> "$wholeNumber.875"
            else -> String.format("%.2f", weight)
        }
    }
}
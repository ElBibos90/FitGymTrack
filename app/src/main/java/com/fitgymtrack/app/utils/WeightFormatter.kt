package com.fitgymtrack.app.utils

import java.util.Locale

/**
 * Utility per formattare i pesi
 */
object WeightFormatter {

    /**
     * Formatta un peso per la visualizzazione
     * @param weight Il peso da formattare
     * @return Stringa formattata del peso
     */
    fun formatWeight(weight: Float): String {
        return when {
            weight == weight.toInt().toFloat() -> {
                // Se è un numero intero, mostra senza decimali
                weight.toInt().toString()
            }
            weight % 0.5f == 0f -> {
                // Se è un multiplo di 0.5, mostra con un decimale
                String.format(Locale.getDefault(),"%.1f", weight)
            }
            weight % 0.25f == 0f -> {
                // Se è un multiplo di 0.25, mostra con due decimali
                String.format(Locale.getDefault(),"%.2f", weight)
            }
            else -> {
                // Per tutti gli altri casi, mostra con due decimali
                String.format(Locale.getDefault(),"%.2f", weight)
            }
        }
    }

    /**
     * Formatta un peso per la visualizzazione con unità
     * @param weight Il peso da formattare
     * @return Stringa formattata del peso con "kg"
     */
    fun formatWeightWithUnit(weight: Float): String {
        return "${formatWeight(weight)} kg"
    }

    /**
     * Converte una stringa in peso (Float)
     * @param weightString La stringa del peso
     * @return Il peso come Float, o null se non valido
     */
    fun parseWeight(weightString: String): Float? {
        return try {
            weightString.replace(",", ".").toFloatOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Arrotonda un peso al valore più vicino utilizzabile in palestra
     * @param weight Il peso da arrotondare
     * @return Il peso arrotondato
     */
    fun roundToUsableWeight(weight: Float): Float {
        return when {
            weight < 5f -> {
                // Per pesi sotto i 5kg, arrotonda a 0.25kg
                (weight * 4).toInt() / 4f
            }
            weight < 20f -> {
                // Per pesi sotto i 20kg, arrotonda a 0.5kg
                (weight * 2).toInt() / 2f
            }
            weight < 50f -> {
                // Per pesi sotto i 50kg, arrotonda a 1kg
                weight.toInt().toFloat()
            }
            else -> {
                // Per pesi sopra i 50kg, arrotonda a 2.5kg
                ((weight / 2.5f).toInt() * 2.5f)
            }
        }
    }

    /**
     * Calcola il prossimo incremento di peso suggerito
     * @param currentWeight Il peso attuale
     * @return L'incremento suggerito
     */
    fun getWeightIncrement(currentWeight: Float): Float {
        return when {
            currentWeight < 10f -> 0.5f
            currentWeight < 30f -> 1.25f
            currentWeight < 60f -> 2.5f
            currentWeight < 100f -> 5f
            else -> 10f
        }
    }

    /**
     * Calcola il peso target per la progressione
     * @param currentWeight Il peso attuale
     * @return Il peso target suggerito
     */
    fun calculateProgressionWeight(currentWeight: Float): Float {
        val increment = getWeightIncrement(currentWeight)
        return roundToUsableWeight(currentWeight + increment)
    }
}
package com.fitgymtrack.app.enums

/**
 * Tipi di notifiche supportate dall'app
 */
enum class NotificationType {
    // === LOCALI - Generate dall'app ===
    SUBSCRIPTION_EXPIRY,        // "Il tuo abbonamento scade tra X giorni"
    SUBSCRIPTION_EXPIRED,       // "Abbonamento scaduto, sei tornato al piano Free"
    LIMIT_REACHED,             // "Hai raggiunto il limite di schede/esercizi per il piano Free"

    // === REMOTE - Ricevute dal server ===
    APP_UPDATE,                // "Nuova versione disponibile con funzionalità X"
    DIRECT_MESSAGE,            // Messaggi diretti dallo sviluppatore

    // === FUTURE - Da implementare ===
    WORKOUT_COMPLETED,         // "Hai completato l'allenamento X!"
    ACHIEVEMENT,               // "Complimenti! Hai raggiunto 10 allenamenti questo mese"
    REMINDER;                  // "Non ti alleni da 3 giorni, che ne dici di una sessione?"

    /**
     * Restituisce true se la notifica è generata localmente dall'app
     */
    fun isLocal(): Boolean {
        return when (this) {
            SUBSCRIPTION_EXPIRY, SUBSCRIPTION_EXPIRED, LIMIT_REACHED,
            WORKOUT_COMPLETED, ACHIEVEMENT, REMINDER -> true
            APP_UPDATE, DIRECT_MESSAGE -> false
        }
    }

    /**
     * Restituisce true se la notifica richiede una connessione internet per essere gestita
     */
    fun requiresNetwork(): Boolean {
        return when (this) {
            APP_UPDATE, DIRECT_MESSAGE -> true
            else -> false
        }
    }

    /**
     * Restituisce l'icona predefinita per questo tipo di notifica
     */
    fun getDefaultIcon(): String {
        return when (this) {
            SUBSCRIPTION_EXPIRY, SUBSCRIPTION_EXPIRED -> "crown"
            LIMIT_REACHED -> "warning"
            APP_UPDATE -> "download"
            DIRECT_MESSAGE -> "message"
            WORKOUT_COMPLETED -> "fitness_center"
            ACHIEVEMENT -> "star"
            REMINDER -> "alarm"
        }
    }
}

/**
 * Priorità delle notifiche
 */
enum class NotificationPriority(val level: Int) {
    LOW(1),           // Suggerimenti, tips
    NORMAL(2),        // Notifiche standard
    HIGH(3),          // Scadenze, limiti raggiunti
    URGENT(4);        // Messaggi critici sviluppatore, app updates critici

    companion object {
        fun fromInt(value: Int): NotificationPriority {
            return entries.firstOrNull { it.level == value } ?: NORMAL
        }
    }
}

/**
 * Sorgente della notifica
 */
enum class NotificationSource {
    LOCAL,            // Generata dall'app stessa
    REMOTE;           // Ricevuta dal server

    fun getDisplayName(): String {
        return when (this) {
            LOCAL -> "App"
            REMOTE -> "Server"
        }
    }
}
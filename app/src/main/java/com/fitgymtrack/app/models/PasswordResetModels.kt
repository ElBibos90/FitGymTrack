package com.fitgymtrack.app.models

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)

data class PasswordResetConfirmRequest(
    val token: String,
    val code: String,
    val newPassword: String
)

data class PasswordResetConfirmResponse(
    val success: Boolean,
    val message: String
)
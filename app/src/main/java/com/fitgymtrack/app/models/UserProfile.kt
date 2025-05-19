package com.fitgymtrack.app.models

data class UserProfile(
    val height: Int? = null,
    val weight: Double? = null,
    val age: Int? = null,
    val gender: String? = null,
    val experienceLevel: String? = null,
    val fitnessGoals: String? = null,
    val injuries: String? = null,
    val preferences: String? = null,
    val notes: String? = null
)

data class UserSubscriptionInfo(
    val planId: Int,
    val planName: String,
    val isActive: Boolean,
    val expiryDate: String? = null
)
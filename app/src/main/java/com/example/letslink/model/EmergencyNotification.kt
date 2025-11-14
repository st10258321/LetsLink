package com.example.letslink.model

// Add this to your existing model files or create a new one
data class EmergencyNotification(
    val notificationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val messageType: String = "", // "hitting_on_me", "bathroom", "going_home"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val groupId: String = "",
    val eventId: String = "",
    val location: String? = null
)
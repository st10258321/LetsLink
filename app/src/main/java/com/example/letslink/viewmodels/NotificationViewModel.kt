package com.example.letslink.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.letslink.model.EmergencyNotification
import com.example.letslink.online_database.fb_NotificationsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationsRepo: fb_NotificationsRepo
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<EmergencyNotification>>(emptyList())
    val notifications: StateFlow<List<EmergencyNotification>> = _notifications.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult: StateFlow<SendResult?> = _sendResult.asStateFlow()

    fun sendEmergencyNotification(
        type: String,
        customMessage: String? = null,
        groupId: String? = null,
        eventId: String? = null
    ) {
        _isSending.value = true
        _sendResult.value = null

        viewModelScope.launch {
            val success = notificationsRepo.sendEmergencyNotification(type, customMessage, groupId, eventId)
            _isSending.value = false
            _sendResult.value = SendResult(success, if (success) "Notification sent!" else "Failed to send notification")

            // Clear result after 3 seconds
            if (success) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _sendResult.value = null
                }
            }
        }
    }

    fun loadRecentNotifications(groupId: String? = null) {
        viewModelScope.launch {
            try {
                val recentNotifications = notificationsRepo.getRecentNotifications(groupId)
                _notifications.value = recentNotifications
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    data class SendResult(val success: Boolean, val message: String)
}
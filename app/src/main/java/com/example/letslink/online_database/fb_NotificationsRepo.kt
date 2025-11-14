package com.example.letslink.online_database

import android.content.Context
import android.util.Log
import com.example.letslink.model.EmergencyNotification
import com.example.letslink.local_database.LetsLinkDB

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class fb_NotificationsRepo(
    private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val userDao = LetsLinkDB.getDatabase(context).userDao()

    suspend fun sendEmergencyNotification(
        messageType: String,
        customMessage: String? = null,
        groupId: String? = null,
        eventId: String? = null
    ): Boolean {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("fb_NotificationsRepo", "No authenticated user")
                return false
            }

            // Get current user details
            val localUser = userDao.getUserByEmail(currentUser.email!!)
            if (localUser == null) {
                Log.e("fb_NotificationsRepo", "Local user not found")
                return false
            }

            // Determine the message based on type
            val message = customMessage ?: when (messageType) {
                "hitting_on_me" -> "Someone is hitting on me and I feel uncomfortable"
                "bathroom" -> "I'm going to the bathroom"
                "going_home" -> "I want to go home"
                else -> "I need assistance"
            }

            // Create notification
            val notification = EmergencyNotification(
                notificationId = database.child("notifications").push().key ?: "",
                senderId = localUser.userId,
                senderName = localUser.firstName,
                messageType = messageType,
                message = message,
                timestamp = System.currentTimeMillis(),
                groupId = groupId ?: "",
                eventId = eventId ?: ""
            )

            // Send to Firebase
            database.child("notifications").child(notification.notificationId).setValue(notification).await()

            // If group ID is provided, send to all group members
            if (!groupId.isNullOrEmpty()) {
                sendNotificationToGroupMembers(notification, groupId)
            }

            Log.d("fb_NotificationsRepo", "Emergency notification sent: $messageType")
            return true

        } catch (e: Exception) {
            Log.e("fb_NotificationsRepo", "Failed to send notification: ${e.message}")
            return false
        }
    }

    private suspend fun sendNotificationToGroupMembers(notification: EmergencyNotification, groupId: String) {
        try {
            // Get group members from Firebase
            val groupSnapshot = database.child("groups").child(groupId).get().await()

            // Handle different possible data structures
            val members = when {
                groupSnapshot.child("members").exists() -> {
                    // If members is a map of user IDs
                    groupSnapshot.child("members").children.mapNotNull { it.key }
                }
                groupSnapshot.child("memberIds").exists() -> {
                    // If members is stored as an array/list
                    groupSnapshot.child("memberIds").children.mapNotNull { it.getValue(String::class.java) }
                }
                groupSnapshot.child("users").exists() -> {
                    // If members are stored under "users"
                    groupSnapshot.child("users").children.mapNotNull { it.key }
                }
                else -> {
                    Log.e("fb_NotificationsRepo", "No members found in group structure")
                    emptyList()
                }
            }

            Log.d("fb_NotificationsRepo", "Found ${members.size} members in group $groupId")

            // Send FCM to each member
            members.forEach { memberId ->
                if (memberId != null && memberId != notification.senderId) { // Don't send to self
                    Log.d("fb_NotificationsRepo", "Sending notification to member: $memberId")
                    sendFCMNotification(memberId, notification)
                }
            }
        } catch (e: Exception) {
            Log.e("fb_NotificationsRepo", "Error sending to group members: ${e.message}")
        }
    }

    private suspend fun sendFCMNotification(memberId: String, notification: EmergencyNotification) {
        try {
            // Get member's FCM token
            val userSnapshot = database.child("users").child(memberId).get().await()
            val fcmToken = userSnapshot.child("fcmToken").getValue(String::class.java)

            if (!fcmToken.isNullOrEmpty()) {
                // Send FCM message (you'll need to implement your FCM server call here)
                // For now, we'll just log it
                Log.d("FCM", "Would send to token: $fcmToken - Message: ${notification.message}")

                // You can implement actual FCM sending using Retrofit to your backend
                // or use Firebase Cloud Functions
            }
        } catch (e: Exception) {
            Log.e("fb_NotificationsRepo", "Error sending FCM: ${e.message}")
        }
    }

    suspend fun getRecentNotifications(groupId: String? = null): List<EmergencyNotification> {
        return try {
            val notificationsRef = if (!groupId.isNullOrEmpty()) {
                database.child("notifications")
                    .orderByChild("groupId")
                    .equalTo(groupId)
            } else {
                database.child("notifications")
            }

            val snapshot = notificationsRef
                .orderByChild("timestamp")
                .limitToLast(20)
                .get()
                .await()

            snapshot.children.mapNotNull { it.getValue(EmergencyNotification::class.java) }
                .sortedByDescending { it.timestamp }

        } catch (e: Exception) {
            Log.e("fb_NotificationsRepo", "Error getting notifications: ${e.message}")
            emptyList()
        }
    }
}
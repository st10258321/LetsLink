package com.example.letslink.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.letslink.R
import com.example.letslink.databinding.ActivityUserSafetyBinding
import com.example.letslink.model.EmergencyNotification
import com.example.letslink.online_database.fb_NotificationsRepo
import com.example.letslink.viewmodels.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserSafety : AppCompatActivity() {

    private lateinit var binding: ActivityUserSafetyBinding
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(fb_NotificationsRepo(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityUserSafetyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupObservers()
        loadRecentNotifications()
    }

    private fun setupClickListeners() {
        // Simple approach - just make the main buttons work for now
        binding.sendAlertButton.setOnClickListener {
            // Show a simple alert for testing
            sendQuickAlert("emergency", "Emergency alert - please check on me!")
        }

        binding.cancelButton.setOnClickListener {
            Toast.makeText(this, "Alert cancelled", Toast.LENGTH_SHORT).show()
        }

        // For the cards, we'll use a simple approach to find them
        setupCardClicks()
    }

    private fun setupCardClicks() {
        try {
            // Find the GridLayout by iterating through views
            val rootView = binding.root
            val gridLayout = findGridLayout(rootView)

            if (gridLayout != null) {
                // Set up click listeners for each card in the grid
                for (i in 0 until gridLayout.childCount) {
                    val cardView = gridLayout.getChildAt(i)
                    cardView.setOnClickListener {
                        handleCardClick(i)
                    }
                }
            } else {
                Log.e("UserSafety", "GridLayout not found")
            }
        } catch (e: Exception) {
            Log.e("UserSafety", "Error setting up card clicks: ${e.message}")
        }
    }

    private fun findGridLayout(view: android.view.View): androidx.gridlayout.widget.GridLayout? {
        if (view is androidx.gridlayout.widget.GridLayout) {
            return view
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findGridLayout(child)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    private fun handleCardClick(position: Int) {
        when (position) {
            0 -> sendQuickAlert("bathroom") // Bathroom
            1 -> sendQuickAlert("going_home") // Going Home
            2 -> sendQuickAlert("hitting_on_me") // Help (Someone is hitting on me)
            3 -> sendQuickAlert("assistance", "I need assistance") // Need Assistance
            4 -> sendQuickAlert("medical", "Medical emergency - need immediate help!") // Medical
            5 -> sendQuickAlert("security", "I feel unsafe and need security assistance") // Security
            else -> sendQuickAlert("general", "I need help")
        }

        // Show feedback
        val messages = arrayOf(
            "Bathroom alert sent!",
            "Going home alert sent!",
            "Help alert sent!",
            "Assistance alert sent!",
            "Medical alert sent!",
            "Security alert sent!"
        )

        if (position < messages.size) {
            Toast.makeText(this, messages[position], Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            notificationViewModel.isSending.collectLatest { isSending ->
                binding.sendAlertButton.isEnabled = !isSending
                if (isSending) {
                    binding.sendAlertButton.text = "Sending..."
                } else {
                    binding.sendAlertButton.text = "SEND ALERT"
                }
            }
        }

        lifecycleScope.launch {
            notificationViewModel.sendResult.collectLatest { result ->
                result?.let {
                    Toast.makeText(
                        this@UserSafety,
                        result.message,
                        if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        lifecycleScope.launch {
            notificationViewModel.notifications.collectLatest { notifications ->
                updateRecentAlertsUI(notifications)
            }
        }
    }

    private fun sendQuickAlert(type: String, customMessage: String? = null) {
        // You might want to get the current event/group from intent or shared preferences
        val currentGroupId = intent.getStringExtra("GROUP_ID") // Pass this when starting activity
        val currentEventId = intent.getStringExtra("EVENT_ID") // Pass this when starting activity

        notificationViewModel.sendEmergencyNotification(type, customMessage, currentGroupId, currentEventId)
    }

    private fun loadRecentNotifications() {
        val currentGroupId = intent.getStringExtra("GROUP_ID")
        notificationViewModel.loadRecentNotifications(currentGroupId)
    }

    private fun updateRecentAlertsUI(notifications: List<EmergencyNotification>) {
        // Update your recent alerts section with actual data
        // For now, we'll just log them
        notifications.forEach { notification ->
            Log.d("RecentAlerts", "${notification.senderName}: ${notification.message}")
        }
    }
}

// Factory for ViewModel
class NotificationViewModelFactory(
    private val notificationsRepo: fb_NotificationsRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(notificationsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
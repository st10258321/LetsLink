package com.example.api_test

import android.util.Log

data class UserState (
    val firstName: String ="",
    val password: String ="",
    val email : String ="",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
) {
    fun isValid(): Boolean {
        if (firstName.isBlank()) {
            Log.d("Logging", "First name is required")
            return false
        }

        if (password.isBlank()) {
            Log.d("Logging", "Password is required")
            return false
        }

        if (email.isBlank()) {
            Log.d("Logging", "Email is required")
            return false
        }
        return if (email.contains("@")) {
            firstName.isNotBlank() && password.isNotBlank() && email.isNotBlank()
        } else {
            Log.d("Logging", "email does not have @")
            false
        }
    }
}

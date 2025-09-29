package com.example.letslink

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

open class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }

    fun saveUserSession(userId: UUID, email: String, name: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_ID, userId.toString())
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_NAME, name)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    open fun getUserId(): UUID? {
        val userIdString = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        return try {
            UUID.fromString(userIdString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != null
    }

    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

}
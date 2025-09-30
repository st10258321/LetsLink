package com.example.letslink.model

sealed interface LoginEvent {
    data class checkEmail(val email: String) : LoginEvent
    data class checkPassword(val password: String) : LoginEvent
    object Login : LoginEvent
}
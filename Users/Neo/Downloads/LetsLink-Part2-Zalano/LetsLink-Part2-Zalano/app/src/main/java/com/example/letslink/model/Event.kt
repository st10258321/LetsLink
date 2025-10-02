package com.example.letslink.model

data class Event(
    val eventId : String,
    val ownerId : String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val date: String
)
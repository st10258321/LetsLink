package com.example.letslink.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class User (
    @PrimaryKey(autoGenerate = true)
    val userId : Int =0,
    val firstName :String,
    val password : String,
    val dateOfBirth : String,
    val email: String,
    var fcmToken: String?
)
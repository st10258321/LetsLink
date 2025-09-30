package com.example.letslink.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class User (
    @PrimaryKey(autoGenerate = true)
    var userId : Int =0,
    var firstName :String,
    val password : String,
    val dateOfBirth : String,
    var email: String,
    var fcmToken: String?
)
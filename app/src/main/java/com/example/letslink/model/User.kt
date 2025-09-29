package com.example.letslink.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class User(
    @PrimaryKey
    val userId : UUID = UUID.randomUUID(),
    val firstName :String,
    val password : String,
    val dateOfBirth : String,
    val email: String
)
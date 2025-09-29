package com.example.api_test
import java.util.UUID
import android.provider.ContactsContract.CommonDataKinds.Email
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey
    val userId :UUID = UUID.randomUUID(),
    val firstName :String,
    val password : String,
    val email: String
)
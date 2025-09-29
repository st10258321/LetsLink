package com.example.api_test
import com.google.gson.annotations.SerializedName

data class GroupRequest(
    @SerializedName("groupId")
    val groupId: String
)


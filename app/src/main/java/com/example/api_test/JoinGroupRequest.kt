package com.example.api_test

import java.util.UUID

data class JoinGroupRequest(
    val groupId: String,
    val userId: UUID?
)
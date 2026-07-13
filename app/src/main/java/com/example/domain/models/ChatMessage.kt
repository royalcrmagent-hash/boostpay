package com.example.domain.models

data class ChatMessage(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

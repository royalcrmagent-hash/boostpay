package com.example.domain.models

import com.google.firebase.Timestamp

data class Transaction(
    val sender: String = "",
    val senderName: String = "",
    val receiver: String = "",
    val receiverName: String = "",
    val amount: Double = 0.0,
    val durationSeconds: Int? = null,
    val timestamp: Timestamp? = null,
    val status: String = "completed"
)

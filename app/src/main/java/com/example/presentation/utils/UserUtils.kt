package com.example.presentation.utils

fun getUserStatus(rank: Int?, balance: Double): String {
    return when {
        rank == 1 -> "LEGEND"
        rank != null && rank <= 3 -> "MYTHIC"
        rank != null && rank <= 10 -> "ELITE"
        rank != null && rank <= 50 -> "PRO"
        balance >= 5000.0 -> "WHALE"
        balance >= 1000.0 -> "TYCOON"
        balance >= 500.0 -> "VIP"
        balance >= 100.0 -> "HUSTLER"
        else -> "ROOKIE"
    }
}

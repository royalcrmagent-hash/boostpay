package com.example.domain.models

data class CurrencyData(
    val code: String, 
    val symbol: String, 
    val name: String = "",
    val flag: String = ""
)

package com.example.core.network

import retrofit2.http.GET

data class CurrencyResponse(
    val result: String = "",
    val rates: Map<String, Double>? = null,
    val conversion_rates: Map<String, Double>? = null
)

interface CurrencyService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): CurrencyResponse

    @GET("v4/latest/USD")
    suspend fun getLatestRatesFallback(): CurrencyResponse
}

fun getFlagEmoji(currencyCode: String): String {
    val currencyToCountry = mapOf(
        "USD" to "US", "EUR" to "EU", "GBP" to "GB", "JPY" to "JP", "INR" to "IN",
        "BDT" to "BD", "CNY" to "CN", "RUB" to "RU", "KRW" to "KR", "TRY" to "TR",
        "PKR" to "PK", "AED" to "AE", "SAR" to "SA", "CAD" to "CA", "AUD" to "AU",
        "SGD" to "SG", "CHF" to "CH", "MYR" to "MY", "THB" to "TH", "IDR" to "ID",
        "PHP" to "PH", "VND" to "VN", "BRL" to "BR", "MXN" to "MX", "ZAR" to "ZA",
        "EGP" to "EG", "NGN" to "NG", "GHS" to "GH", "KES" to "KE", "LKR" to "LK",
        "NPR" to "NP", "AFN" to "AF", "IQD" to "IQ", "KWD" to "KW", "QAR" to "QA",
        "OMR" to "OM", "BHD" to "BH", "JOD" to "JO", "ILS" to "IL", "PLN" to "PL",
        "SEK" to "SE", "NOK" to "NO", "DKK" to "DK", "HUF" to "HU", "CZK" to "CZ",
        "RON" to "RO", "BGN" to "BG", "ARS" to "AR", "CLP" to "CL", "COP" to "CO",
        "PEN" to "PE", "UYU" to "UY", "NZD" to "NZ", "HKD" to "HK", "TWD" to "TW",
        "UAH" to "UA", "KZT" to "KZ", "DZD" to "DZ", "MAD" to "MA", "TND" to "TN",
        "MVR" to "MV", "MUR" to "MU", "SCR" to "SC", "MOP" to "MO", "KHR" to "KH",
        "LAK" to "LA", "MMK" to "MM", "MNT" to "MN", "BND" to "BN", "FJD" to "FJ",
        "PGK" to "PG", "SBD" to "SB", "VUV" to "VU", "TOP" to "TO", "WST" to "WS"
    )
    
    val countryCode = currencyToCountry[currencyCode] ?: if (currencyCode.length >= 2) currencyCode.take(2) else "US"
    
    return try {
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) {
        "🏳️"
    }
}

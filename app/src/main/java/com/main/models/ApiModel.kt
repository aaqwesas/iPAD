package com.main.models

data class TokenResponse(
    val token: String,
    val message: String
)

data class TokenVerifyRequest(
    val token: String
)

data class VerifyTokenResponse(
    val valid: Boolean,
    val message: String
)

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val change_percent: Double,
    val volume: Int,
    val timestamp: String
)

data class OHLC(
    val symbol: String,
    val price: Double,
    val change: Double,
    val change_percent: Double,
    val volume: Int,
    val timestamp: String,
    val open_price: Double,
    val high_price: Double,
    val low_price: Double,
    val close_price: Double
)

data class OHLC_history(
    val symbol: String,
    val timestamp: String,
    val open_price: Double,
    val high_price: Double,
    val low_price: Double,
    val close_price: Double,
    val volume: Int
)

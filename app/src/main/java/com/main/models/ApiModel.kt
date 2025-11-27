package com.main.models

data class RegisterResponse(
    val message: String,
    val is_new: Boolean
    // val email: String? = null  // optional, if you return it
)

data class RegisterRequest(
    val email: String
)

data class FCMUpdateRequest(
    val email: String,
    val fcm_token: String
)

data class CompanyNameResponse(
    val companyName: String
)


data class SimpleResponse(val status: String)

data class CreateAlertRequest(
    val email: String,
    val symbol: String,
    val target: Double,
    val condition: String  // "above", "below", "rises_above", "drops_below"
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
    val date: String,
    val open_price: Double,
    val high_price: Double,
    val low_price: Double,
    val close_price: Double,
    val volume: Int
)


data class AddHistoryResponse(
    val message: String,
    val id: Int?,
    val value: Float,
    val timestamp: String
)


data class PortfolioHistoryResponse(
    val value: Float,
    val timestamp: String
)


data class PortfolioValueResponse(
    val value: Float
)
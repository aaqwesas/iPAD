package com.main.Data

data class AddHoldingRequest(
    val stock_ticker: String,
    val quantity: Float
)
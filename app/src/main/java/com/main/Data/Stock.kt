package com.main.Data

data class Stock(
    val ticker: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val iconResId: Int = 0,
    val volume: Int = 0,
    val marketCap: Double = 0.0
)
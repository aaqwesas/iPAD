package com.main.models

data class HistoricalBar(
    val date: String,          // "YYYY-MM-DD"
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class HistoricalResponse(
    val symbol: String,
    val bars: List<HistoricalBar>
)
package com.main

data class StockData(
    val currentPrice: Double,
    val previousClose: Double,
    val volume: Long,
    val changePercent: Double
)

object StockRepository {
    val stocks = mapOf(
        "AAPL" to StockData(95.00, 100.00, 500000, -2.0),
        "GOOGL" to StockData(2600.00, 2720.00, 300000, -1.5)
    )

    val stockNames: List<String>
        get() = stocks.keys.toList()
}
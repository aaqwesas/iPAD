package com.main.Data

import com.main.models.HistoricalBar

interface HistoricalRepository {
    suspend fun get(
        symbol: String,
        limit: Int? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<HistoricalBar>
}
package com.main.Data

import com.main.models.HistoricalBar
import kotlinx.coroutines.delay
import org.json.JSONObject

class FakeHistoricalRepository : HistoricalRepository {

    private val sampleJson = """
        {
          "symbol": "TSLA",
          "bars": [
            {"date":"2024-10-01","open":245.1,"high":250.0,"low":244.3,"close":248.9,"volume":51234567},
            {"date":"2024-10-02","open":248.9,"high":252.2,"low":247.8,"close":250.7,"volume":47654321},
            {"date":"2024-10-03","open":250.7,"high":253.4,"low":249.2,"close":252.9,"volume":42345678},
            {"date":"2024-10-04","open":252.9,"high":255.1,"low":251.0,"close":254.6,"volume":40123456},
            {"date":"2024-10-07","open":254.6,"high":256.8,"low":253.0,"close":255.3,"volume":38901234},
            {"date":"2024-10-08","open":255.3,"high":257.2,"low":254.1,"close":256.7,"volume":37567890},
            {"date":"2024-10-09","open":256.7,"high":259.5,"low":255.8,"close":258.2,"volume":36234567},
            {"date":"2024-10-10","open":258.2,"high":260.0,"low":257.0,"close":259.1,"volume":35123456},
            {"date":"2024-10-11","open":259.1,"high":262.3,"low":258.5,"close":261.7,"volume":34789012},
            {"date":"2024-10-14","open":261.7,"high":264.0,"low":260.2,"close":263.5,"volume":33987654}
          ]
        }
    """.trimIndent()

    override suspend fun get(
        symbol: String,
        limit: Int?,
        startDate: String?,
        endDate: String?
    ): List<HistoricalBar> {
        delay(300) // simulate network

        val root = JSONObject(sampleJson)
        val bars = root.getJSONArray("bars")
        val list = ArrayList<HistoricalBar>(bars.length())
        for (i in 0 until bars.length()) {
            val o = bars.getJSONObject(i)
            list.add(
                HistoricalBar(
                    date = o.getString("date"),
                    open = o.getDouble("open"),
                    high = o.getDouble("high"),
                    low = o.getDouble("low"),
                    close = o.getDouble("close"),
                    volume = o.getLong("volume")
                )
            )
        }

        return if (limit != null && limit < list.size) list.takeLast(limit) else list
    }
}
package com.main.api

import com.main.Data.AddHoldingRequest
import com.main.Data.PortfolioResponse
import com.main.Data.PortfolioUpdate
import com.main.Data.UserHoldingResponse
import retrofit2.Response
import retrofit2.http.*
import com.main.models.RegisterResponse
import com.main.models.RegisterRequest
import com.main.models.TokenVerifyRequest
import com.main.models.FCMUpdateRequest
import com.main.models.CreateAlertRequest
import com.main.models.SimpleResponse
import com.main.models.Stock
import com.main.models.OHLC
import com.main.models.OHLC_history
import com.main.models.VerifyTokenResponse

interface StockApiService {
    @POST("api/generate-token")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/verify-token")
    suspend fun verifyToken(@Body tokenData: TokenVerifyRequest): Response<VerifyTokenResponse>

    @POST("api/set-fcm-token")
    suspend fun setFcmToken(@Body request: FCMUpdateRequest): Response<SimpleResponse>

    @POST("api/alerts")
    suspend fun createAlert(@Body request: CreateAlertRequest): Response<SimpleResponse>

    @GET("api/stocks")
    suspend fun getStocks(): Response<List<Stock>>

    @GET("api/stocks/{symbol}")
    suspend fun getStock(@Path("symbol") symbol: String): Response<OHLC>

//    @POST("api/stocks")
//    suspend fun insertStock(@Body stock: Stock): Response<>

    @GET("api/stocks/history/{symbol}")
    suspend fun getStockHistory(@Path("symbol") symbol: String): Response<List<OHLC_history>>

    @GET("api/stocks/history/weekly/{symbol}")
    suspend fun getStockHistoryWeekly(@Path("symbol") symbol: String): Response<List<OHLC_history>>

    @GET("api/health")
    suspend fun checkhealth(): Response<Map<String, String>>

    @GET("api/symbols")
    suspend fun getSymbols(): Response<List<String>>

    // Portfolio endpoints - updated to use token instead of user_id
    @GET("users/{token}/holdings")
    suspend fun getUserHoldings(@Path("token") token: String): Response<List<UserHoldingResponse>>

    @GET("users/{token}/portfolio")
    suspend fun getPortfolioValue(@Path("token") token: String): Response<PortfolioResponse>

    @PUT("users/{token}/portfolio")
    suspend fun updatePortfolioValue(
        @Path("token") token: String,
        @Body update: PortfolioUpdate
    ): Response<PortfolioResponse>

    @POST("users/{token}/holdings")
    suspend fun addUserHolding(
        @Path("token") token: String,
        @Body request: AddHoldingRequest
    ): Response<UserHoldingResponse>
}
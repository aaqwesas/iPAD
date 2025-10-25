package com.main.api


import retrofit2.Response
import retrofit2.http.*
import com.main.models.TokenResponse
import com.main.models.TokenVerifyRequest
import com.main.models.Stock
import com.main.models.VerifyTokenResponse

interface StockApiService {
    @POST("api/generate-token")
    suspend fun generateToken(): Response<TokenResponse>

    @POST("api/verify-token")
    suspend fun verifyToken(@Body tokenData: TokenVerifyRequest): Response<VerifyTokenResponse>

    @GET("api/stocks")
    suspend fun getStocks(): Response<List<Stock>>

    @GET("api/stocks/{symbol}")
    suspend fun getStock(@Path("symbol") symbol: String): Response<Stock>
}
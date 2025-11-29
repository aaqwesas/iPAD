import com.main.Data.AddHoldingRequest
import com.main.Data.PortfolioUpdate
import com.main.api.StockApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.main.models.*

@ExperimentalCoroutinesApi
class StockApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: StockApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(StockApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `registerUser returns success for new user`() = runTest {
        val mockResponse = """
            {
                "message": "Token generated successfully",
                "is_new": true
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = RegisterRequest("test@example.com")

        val response = apiService.registerUser(request)

        assertTrue(response.isSuccessful)
        assertEquals("Token generated successfully", response.body()!!.message)
        assertTrue(response.body()!!.is_new)
    }

    @Test
    fun `registerUser returns success for existing user`() = runTest {
        val mockResponse = """
            {
                "message": "Welcome back",
                "is_new": false
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = RegisterRequest("existing@example.com")

        val response = apiService.registerUser(request)

        assertTrue(response.isSuccessful)
        assertEquals("Welcome back", response.body()!!.message)
        assertFalse(response.body()!!.is_new)
    }

    @Test
    fun `verifyToken returns valid for correct token`() = runTest {
        val mockResponse = """
            {
                "valid": true,
                "message": "Token is valid"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = TokenVerifyRequest("valid-token")

        val response = apiService.verifyToken(request)

        assertTrue(response.isSuccessful)
        assertTrue(response.body()!!.valid)
        assertEquals("Token is valid", response.body()!!.message)
    }

    @Test
    fun `verifyToken returns invalid for expired token`() = runTest {
        val mockResponse = """
            {
                "valid": false,
                "message": "Token has expired"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody(mockResponse))

        val request = TokenVerifyRequest("expired-token")

        val response = apiService.verifyToken(request)

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `setFcmToken returns success`() = runTest {
        val mockResponse = """{"status": "success"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = FCMUpdateRequest("test@example.com", "fcm-token-123")

        val response = apiService.setFcmToken(request)

        assertTrue(response.isSuccessful)
        assertEquals("success", response.body()!!.status)
    }

    @Test
    fun `createAlert returns success for valid alert`() = runTest {
        val mockResponse = """{"status": "success"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = CreateAlertRequest(
            email = "test@example.com",
            symbol = "AAPL",
            target = 150.0,
            condition = "above"
        )

        val response = apiService.createAlert(request)

        assertTrue(response.isSuccessful)
        assertEquals("success", response.body()!!.status)
    }

    @Test
    fun `get_company_name returns company name for valid symbol`() = runTest {
        val mockResponse = """{"companyName": "Apple Inc."}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.get_company_name("AAPL")

        assertTrue(response.isSuccessful)
        assertEquals("Apple Inc.", response.body()!!.companyName)
    }

    @Test
    fun `getStocks returns list of stocks`() = runTest {
        val mockResponse = """
            [
                {
                    "symbol": "AAPL",
                    "name": "Apple Inc.",
                    "price": 150.0,
                    "change": 2.5,
                    "change_percent": 1.69,
                    "volume": 1000000,
                    "timestamp": "2024-01-01T10:00:00"
                },
                {
                    "symbol": "GOOGL",
                    "name": "Alphabet Inc.",
                    "price": 2800.0,
                    "change": -15.0,
                    "change_percent": -0.53,
                    "volume": 500000,
                    "timestamp": "2024-01-01T10:00:00"
                }
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getStocks()

        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()!!.size)
        assertEquals("AAPL", response.body()!![0].symbol)
        assertEquals(150.0, response.body()!![0].price, 0.01)
    }

    @Test
    fun `getStock returns OHLC data`() = runTest {
        val mockResponse = """
            {
                "symbol": "AAPL",
                "price": 150.0,
                "change": 2.5,
                "change_percent": 1.69,
                "volume": 1000000,
                "timestamp": "2024-01-01T10:00:00",
                "open_price": 148.5,
                "high_price": 151.0,
                "low_price": 148.0,
                "close_price": 150.0
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getStock("AAPL")

        assertTrue(response.isSuccessful)
        assertEquals("AAPL", response.body()!!.symbol)
        assertEquals(150.0, response.body()!!.price, 0.01)
        assertEquals(148.5, response.body()!!.open_price, 0.01)
    }

    @Test
    fun `getStockHistory returns historical data`() = runTest {
        val mockResponse = """
            [
                {
                    "symbol": "AAPL",
                    "date": "2024-01-01",
                    "open_price": 148.5,
                    "high_price": 151.0,
                    "low_price": 148.0,
                    "close_price": 150.0,
                    "volume": 1000000
                },
                {
                    "symbol": "AAPL",
                    "date": "2024-01-02",
                    "open_price": 150.5,
                    "high_price": 152.0,
                    "low_price": 149.5,
                    "close_price": 151.5,
                    "volume": 1200000
                }
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getStockHistory("AAPL")

        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()!!.size)
        assertEquals("2024-01-01", response.body()!![0].date)
        assertEquals(148.5, response.body()!![0].open_price, 0.01)
    }

    @Test
    fun `getStockHistoryWeekly returns weekly data`() = runTest {
        val mockResponse = """
            [
                {
                    "symbol": "AAPL",
                    "date": "2024-01-01",
                    "open_price": 148.5,
                    "high_price": 151.0,
                    "low_price": 148.0,
                    "close_price": 150.0,
                    "volume": 5000000
                }
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getStockHistoryWeekly("AAPL")

        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()!!.size)
        assertEquals(5000000, response.body()!![0].volume)
    }

    @Test
    fun `checkhealth returns healthy status`() = runTest {
        val mockResponse = """{"status": "healthy"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.checkhealth()

        assertTrue(response.isSuccessful)
        assertEquals("healthy", response.body()!!["status"])
    }

    @Test
    fun `getSymbols returns list of stock symbols`() = runTest {
        val mockResponse = """["AAPL", "GOOGL", "MSFT", "TSLA"]"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getSymbols()

        assertTrue(response.isSuccessful)
        assertEquals(4, response.body()!!.size)
        assertEquals("AAPL", response.body()!![0])
        assertTrue(response.body()!!.contains("TSLA"))
    }

    @Test
    fun `getUserHoldings returns user portfolio`() = runTest {
        val mockResponse = """
            [
                {"stock_ticker": "AAPL", "quantity": 10.0},
                {"stock_ticker": "GOOGL", "quantity": 5.0}
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getUserHoldings("user-token-123")

        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()!!.size)
        assertEquals("AAPL", response.body()!![0].stock_ticker)
        assertEquals(10.0f, response.body()!![0].quantity)
    }

    @Test
    fun `getUserHoldings returns empty for new user`() = runTest {
        val mockResponse = "[]"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getUserHoldings("new-user-token")

        assertTrue(response.isSuccessful)
        assertTrue(response.body()!!.isEmpty())
    }

    @Test
    fun `getPortfolioValue returns current portfolio value`() = runTest {
        val mockResponse = """
        {
            "value": 15000.50
        }
    """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getPortfolioValue("user-token-123")

        assertTrue(response.isSuccessful)
        assertEquals(15000.50f, response.body()!!.value, 0.1f)
    }

    @Test
    fun `updatePortfolioValue updates successfully`() = runTest {
        val mockResponse = """{"value": 16000.75}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val update = PortfolioUpdate(value = 16000.75f)

        val response = apiService.updatePortfolioValue("user-token-123", update)
        val expected = response.body()!!.value

        assertTrue(response.isSuccessful)
        assertNotNull(expected)
        assertEquals(16000.75f, expected)
    }

    @Test
    fun `addUserHolding executes buy trade successfully`() = runTest {
        val mockResponse = """
            {"stock_ticker": "AAPL", "quantity": 15.0}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val request = AddHoldingRequest(stock_ticker = "AAPL", quantity = 5.0f)

        val response = apiService.addUserHolding("user-token-123", request)

        assertTrue(response.isSuccessful)
        assertEquals("AAPL", response.body()!!.stock_ticker)
        assertEquals(15.0f, response.body()!!.quantity)
    }

    @Test
    fun `addUserHolding handles insufficient funds`() = runTest {
        val mockResponse = """
            {"error": "Insufficient funds"}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(mockResponse))

        val request = AddHoldingRequest(stock_ticker = "AAPL", quantity = 1000.0f)

        val response = apiService.addUserHolding("user-token-123", request)

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }

    @Test
    fun `getPortfolioPercentageChange returns performance`() = runTest {
        val mockResponse = "15.5"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getPortfolioPercentageChange("user-token-123")

        assertTrue(response.isSuccessful)
        assertEquals(15.5, response.body()!!, 0.01)
    }

    @Test
    fun `getUserHolding returns specific stock holding`() = runTest {
        val mockResponse = """{"stock_ticker": "AAPL", "quantity": 10.0}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getUserHolding("user-token-123", "AAPL")

        assertTrue(response.isSuccessful)
        assertEquals("AAPL", response.body()!!.stock_ticker)
        assertEquals(10.0f, response.body()!!.quantity)
    }

    @Test
    fun `addPortfolioHistory adds history record`() = runTest {
        val mockResponse = """
            {
                "message": "Portfolio history added successfully",
                "id": 123,
                "value": 15000.0,
                "timestamp": "2024-01-01T10:00:00"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.addPortfolioHistory("user-token-123", 15000.0f)

        assertTrue(response.isSuccessful)
        assertEquals("Portfolio history added successfully", response.body()!!.message)
        assertEquals(123, response.body()!!.id)
        assertEquals(15000.0f, response.body()!!.value)
    }

    @Test
    fun `getPortfolioHistory returns portfolio history`() = runTest {
        val mockResponse = """
            [
                {"value": 14000.0, "timestamp": "2024-01-01T10:00:00"},
                {"value": 14500.0, "timestamp": "2024-01-02T10:00:00"},
                {"value": 15000.0, "timestamp": "2024-01-03T10:00:00"}
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getPortfolioHistory("user-token-123", 30)

        assertTrue(response.isSuccessful)
        assertEquals(3, response.body()!!.size)
        assertEquals(14000.0f, response.body()!![0].value)
        assertEquals("2024-01-01T10:00:00", response.body()!![0].timestamp)
    }

    @Test
    fun `getPortfolioHistory uses default limit`() = runTest {
        val mockResponse = "[]"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val response = apiService.getPortfolioHistory("user-token-123")

        assertTrue(response.isSuccessful)
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("users/user-token-123/history"))
    }

    @Test
    fun `api handles 500 internal server error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val response = apiService.getUserHoldings("user-token-123")

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `api handles 404 not found`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val response = apiService.getUserHolding("user-token-123", "INVALID")

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}
package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.model.Account
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class UntisClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: UntisClient
    private var fakeClock: Long = 1793994791000L // epoch millis
    private val account = Account(
        host = "localhost",
        school = "demo-schule",
        user = "Muster_Max_20080101",
        key = "JBSWY3DPEHPK3PXP",
        schoolNumber = "1234567"
    )

    private fun resp(code: Int = 200, body: String = ""): MockResponse =
        MockResponse.Builder().code(code).body(body).build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = UntisClient(
            account = account,
            http = OkHttpClient(),
            baseUrl = server.url("/"),
            clock = { fakeClock }
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun testLoginSendsRightData() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcResponse = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""
        server.enqueue(resp(200, rpcResponse))

        val masterData = client.login()
        assertEquals(1L, masterData.personId)

        val request = server.takeRequest()
        val url = request.url
        assertEquals("/WebUntis/jsonrpc_intern.do", url.encodedPath)
        assertEquals("getUserData2017", url.queryParameter("m"))
        assertEquals("demo-schule", url.queryParameter("school"))
        assertEquals("i2.2", url.queryParameter("v"))

        val body = request.body?.utf8().orEmpty()
        assertTrue("Body should contain user", body.contains(""""user":"Muster_Max_20080101""""))
        val expectedOtp = Totp.code(account.key, fakeClock / 1000L)
        assertTrue("Body should contain expected otp $expectedOtp", body.contains(""""otp":$expectedOtp"""))
    }

    @Test
    fun testTimetableSendsAuthAndResources() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcLogin = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "fake-jwt-token-123"))
        server.enqueue(resp(200, Fixtures.load("timetable_day_foyer.json")))

        val days = client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))
        assertEquals(1, days.size)

        // Request 1: login
        val req1 = server.takeRequest()
        assertEquals("/WebUntis/jsonrpc_intern.do", req1.url.encodedPath)

        // Request 2: token
        val req2 = server.takeRequest()
        assertEquals("/WebUntis/api/token/new", req2.url.encodedPath)

        // Request 3: timetable
        val req3 = server.takeRequest()
        assertEquals("/WebUntis/api/rest/view/v1/timetable/entries", req3.url.encodedPath)
        assertEquals("Bearer fake-jwt-token-123", req3.headers["Authorization"])
        assertEquals("1", req3.url.queryParameter("resources"))
        assertEquals("STUDENT", req3.url.queryParameter("resourceType"))
        assertEquals("2", req3.url.queryParameter("format"))
        assertEquals("MY_TIMETABLE", req3.url.queryParameter("timetableType"))
    }

    @Test
    fun testTokenReusedWithin12MinAndRefreshedAfter() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcLogin = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "initial-token"))
        server.enqueue(resp(200, Fixtures.load("timetable_day_foyer.json")))

        fakeClock = 100_000L
        client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))

        server.takeRequest() // login
        server.takeRequest() // token
        server.takeRequest() // timetable

        // Advance by 11 minutes (token still valid)
        fakeClock += 11 * 60 * 1000L
        server.enqueue(resp(200, Fixtures.load("timetable_day_foyer.json")))
        client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))

        val req4 = server.takeRequest()
        assertEquals("/WebUntis/api/rest/view/v1/timetable/entries", req4.url.encodedPath)
        assertEquals("Bearer initial-token", req4.headers["Authorization"])

        // Advance past 12 minutes (token expired)
        fakeClock += 2 * 60 * 1000L
        server.enqueue(resp(200, "refreshed-token"))
        server.enqueue(resp(200, Fixtures.load("timetable_day_foyer.json")))
        client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))

        val reqToken = server.takeRequest()
        assertEquals("/WebUntis/api/token/new", reqToken.url.encodedPath)

        val reqTt = server.takeRequest()
        assertEquals("/WebUntis/api/rest/view/v1/timetable/entries", reqTt.url.encodedPath)
        assertEquals("Bearer refreshed-token", reqTt.headers["Authorization"])
    }

    @Test
    fun test401RetriesWithReloginAndSucceeds() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcLogin = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""

        // Initial setup
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "token-1"))
        server.enqueue(resp(401, "Unauthorized")) // REST call returns 401

        // Retry flow: re-login, token/new, retried timetable
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "token-2"))
        server.enqueue(resp(200, Fixtures.load("timetable_day_foyer.json")))

        val days = client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))
        assertEquals(1, days.size)

        server.takeRequest() // initial login
        server.takeRequest() // initial token
        server.takeRequest() // initial timetable (failed with 401)
        server.takeRequest() // re-login
        server.takeRequest() // new token
        val finalReq = server.takeRequest() // retried timetable
        assertEquals("Bearer token-2", finalReq.headers["Authorization"])
    }

    @Test
    fun testSecond401ThrowsHttpException() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcLogin = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""

        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "token-1"))
        server.enqueue(resp(401, "Unauthorized")) // first 401
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "token-2"))
        server.enqueue(resp(401, "Still Unauthorized")) // second 401

        try {
            client.timetable(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))
            fail("Expected UntisException.Http")
        } catch (e: UntisException.Http) {
            assertEquals(401, e.code)
        }
    }

    @Test
    fun testRpcError8504ThrowsBadCredentials() = runBlocking {
        val errorJson = """{"id": "1", "jsonrpc": "2.0", "error": {"code": -8504, "message": "bad credentials"}}"""
        server.enqueue(resp(200, errorJson))

        try {
            client.login()
            fail("Expected UntisException.BadCredentials")
        } catch (e: UntisException.BadCredentials) {
            // expected
        }
    }

    @Test
    fun testAbsencesSendsAuthAndParameters() = runBlocking {
        val userdataJson = Fixtures.load("userdata.json")
        val rpcLogin = """{"id": "1", "jsonrpc": "2.0", "result": $userdataJson}"""
        server.enqueue(resp(200, rpcLogin))
        server.enqueue(resp(200, "fake-jwt-token-123"))
        server.enqueue(resp(200, Fixtures.load("absences_2026.json")))

        val absences = client.absences(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 11, 6))
        assertEquals(14, absences.size)

        server.takeRequest() // login
        server.takeRequest() // token
        val reqAbs = server.takeRequest()
        assertEquals("/WebUntis/api/classreg/absences/students", reqAbs.url.encodedPath)
        assertEquals("Bearer fake-jwt-token-123", reqAbs.headers["Authorization"])
        assertEquals("20260831", reqAbs.url.queryParameter("startDate"))
        assertEquals("20261106", reqAbs.url.queryParameter("endDate"))
        assertEquals("1", reqAbs.url.queryParameter("studentId"))
        assertEquals("-1", reqAbs.url.queryParameter("excuseStatusId"))
    }

    @Test
    fun testSchoolSearchParsesFixture() = runBlocking {
        server.enqueue(resp(200, Fixtures.load("schoolsearch.json")))

        val schools = UntisClient.searchSchools(OkHttpClient(), "demo-schule", server.url("/"))
        assertEquals(3, schools.size)
        val school = schools[0]
        assertEquals("demo.webuntis.com", school.host)
        assertEquals("demo-schule", school.loginName)
        assertEquals("1234567", school.schoolNumber)
        assertEquals("Musterschule Beispielstadt", school.displayName)

        val req = server.takeRequest()
        assertEquals("/ms/schoolquery2", req.url.encodedPath)
        val body = req.body?.utf8().orEmpty()
        assertTrue("Body should have search query", body.contains(""""search":"demo-schule""""))
    }
}

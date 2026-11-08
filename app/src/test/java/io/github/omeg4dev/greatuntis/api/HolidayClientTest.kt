package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.Fixtures
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class HolidayClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: HolidayClient

    private fun resp(code: Int = 200, body: String = ""): MockResponse =
        MockResponse.Builder().code(code).body(body).build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = HolidayClient(
            http = OkHttpClient(),
            baseUrl = server.url("/"),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun testFetchParsesRecordedFixturesAndSendsExpectedRequests() = runBlocking {
        val schoolHolidaysJson = Fixtures.load("openholidays_SchoolHolidays_NW.json")
        val publicHolidaysJson = Fixtures.load("openholidays_PublicHolidays_NW.json")

        server.enqueue(resp(200, schoolHolidaysJson))
        server.enqueue(resp(200, publicHolidaysJson))

        val from = LocalDate.of(2025, 8, 1)
        val to = LocalDate.of(2027, 7, 31)
        val holidays = client.fetch("NW", from, to)

        val req1 = server.takeRequest()
        assertEquals("/SchoolHolidays", req1.url.encodedPath)
        assertEquals("DE", req1.url.queryParameter("countryIsoCode"))
        assertEquals("DE-NW", req1.url.queryParameter("subdivisionCode"))
        assertEquals("DE", req1.url.queryParameter("languageIsoCode"))
        assertEquals("2025-08-01", req1.url.queryParameter("validFrom"))
        assertEquals("2027-07-31", req1.url.queryParameter("validTo"))
        assertEquals("application/json", req1.headers["Accept"])
        assertEquals(true, req1.headers["User-Agent"]?.startsWith("GreatUntis/"))

        val req2 = server.takeRequest()
        assertEquals("/PublicHolidays", req2.url.encodedPath)
        assertEquals("DE", req2.url.queryParameter("countryIsoCode"))
        assertEquals("DE-NW", req2.url.queryParameter("subdivisionCode"))
        assertEquals("DE", req2.url.queryParameter("languageIsoCode"))
        assertEquals("2025-08-01", req2.url.queryParameter("validFrom"))
        assertEquals("2027-07-31", req2.url.queryParameter("validTo"))
        assertEquals("application/json", req2.headers["Accept"])

        val schoolCount = holidays.count { it.type == ApiHolidayType.SCHOOL }
        val publicCount = holidays.count { it.type == ApiHolidayType.PUBLIC }
        assertEquals(11, schoolCount)
        assertEquals(22, publicCount)
        assertEquals(33, holidays.size)

        // Result sorted by start
        for (i in 0 until holidays.size - 1) {
            assertTrue(
                "Holidays must be sorted by start",
                holidays[i].start <= holidays[i + 1].start,
            )
        }

        // Herbstferien 2026 = 2026-10-17…2026-10-31
        val herbst2026 = holidays.firstOrNull {
            it.name == "Herbstferien" && it.start.year == 2026 && it.type == ApiHolidayType.SCHOOL
        }
        assertNotNull("Herbstferien 2026 must be present", herbst2026)
        assertEquals(LocalDate.of(2026, 10, 17), herbst2026?.start)
        assertEquals(LocalDate.of(2026, 10, 31), herbst2026?.end)
    }

    @Test
    fun testHttp500ThrowsIOException() = runBlocking {
        server.enqueue(resp(500, "Internal Server Error"))

        try {
            client.fetch("NW", LocalDate.of(2025, 8, 1), LocalDate.of(2027, 7, 31))
            fail("Expected IOException on HTTP 500")
        } catch (e: IOException) {
            assertTrue("Expected HTTP 500 error message, got: ${e.message}", e.message?.contains("HTTP 500") == true)
        }
    }
}

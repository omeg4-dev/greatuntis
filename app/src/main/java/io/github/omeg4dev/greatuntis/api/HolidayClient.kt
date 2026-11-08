package io.github.omeg4dev.greatuntis.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate

enum class ApiHolidayType { SCHOOL, PUBLIC }

data class ApiHoliday(
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
    val type: ApiHolidayType,
)

/**
 * Client for OpenHolidays API (https://openholidaysapi.org).
 *
 * Note: The span is limited by the API to 3 years; callers must pass at most that.
 */
class HolidayClient(
    private val http: OkHttpClient,
    private val baseUrl: HttpUrl = "https://openholidaysapi.org".toHttpUrl(),
) {
    /**
     * state = "NW", "BY", …
     * Throws IOException on network/HTTP errors.
     * Callers must pass a date range of at most 3 years due to API limitations.
     */
    suspend fun fetch(state: String, from: LocalDate, to: LocalDate): List<ApiHoliday> =
        withContext(Dispatchers.IO) {
            val schoolHolidays = fetchEndpoint("/SchoolHolidays", state, from, to, ApiHolidayType.SCHOOL)
            val publicHolidays = fetchEndpoint("/PublicHolidays", state, from, to, ApiHolidayType.PUBLIC)
            (schoolHolidays + publicHolidays).sortedWith(compareBy({ it.start }, { it.end }, { it.name }))
        }

    private fun fetchEndpoint(
        endpoint: String,
        state: String,
        from: LocalDate,
        to: LocalDate,
        type: ApiHolidayType,
    ): List<ApiHoliday> {
        val path = endpoint.trimStart('/')
        val url = baseUrl.resolve(path)
            ?.newBuilder()
            ?.addQueryParameter("countryIsoCode", "DE")
            ?.addQueryParameter("subdivisionCode", "DE-$state")
            ?.addQueryParameter("languageIsoCode", "DE")
            ?.addQueryParameter("validFrom", from.toString())
            ?.addQueryParameter("validTo", to.toString())
            ?.build()
            ?: throw IOException("Invalid URL: $baseUrl $endpoint")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", HOLIDAY_USER_AGENT)
            .get()
            .build()

        val responseBody = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            response.body.string()
        }

        val dtoList = try {
            holidayJson.decodeFromString<List<OpenHolidayItemDto>>(responseBody)
        } catch (e: Exception) {
            throw IOException("Failed to parse holiday JSON: ${e.message}", e)
        }

        return dtoList.mapNotNull { item ->
            if (item.regionalScope == "Local") return@mapNotNull null

            val holidayName = item.name.firstOrNull { it.language.equals("DE", ignoreCase = true) }?.text
                ?: item.name.firstOrNull()?.text
                ?: ""

            ApiHoliday(
                name = holidayName,
                start = LocalDate.parse(item.startDate),
                end = LocalDate.parse(item.endDate),
                type = type,
            )
        }
    }

    companion object {
        private val holidayJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }

    @Serializable
    private data class OpenHolidayItemDto(
        val startDate: String,
        val endDate: String,
        val name: List<OpenHolidayNameDto> = emptyList(),
        val regionalScope: String? = null,
    )

    @Serializable
    private data class OpenHolidayNameDto(
        val language: String? = null,
        val text: String = "",
    )
}

/** openholidaysapi.org answers 418 to OkHttp's default User-Agent, so every request names the app. */
internal val HOLIDAY_USER_AGENT =
    "GreatUntis/${io.github.omeg4dev.greatuntis.BuildConfig.VERSION_NAME} (+https://github.com/omeg4-dev/greatuntis)"

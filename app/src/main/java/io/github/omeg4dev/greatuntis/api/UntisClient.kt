package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

sealed class UntisException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class BadCredentials(message: String = "Bad credentials") : UntisException(message)
    class Network(cause: IOException) : UntisException(cause.message ?: "Network error", cause)
    class Http(val code: Int) : UntisException("HTTP $code")
    class Protocol(message: String) : UntisException(message)
}

class UntisClient(
    private val account: Account,
    http: OkHttpClient,
    private val baseUrl: HttpUrl = "https://${account.host}".toHttpUrl(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val client: OkHttpClient = http.newBuilder()
        .cookieJar(InMemoryCookieJar())
        .build()

    @Volatile
    private var personId: Long? = null

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenAt: Long = 0L

    suspend fun login(): MasterData = withContext(Dispatchers.IO) {
        val otp = Totp.code(account.key, clock() / 1000L)
        val authObj = buildJsonObject {
            put("clientTime", clock())
            put("user", account.user)
            put("otp", otp)
        }
        val requestBodyJson = buildJsonObject {
            put("id", "1")
            put("jsonrpc", "2.0")
            put("method", "getUserData2017")
            putJsonArray("params") {
                add(buildJsonObject {
                    put("auth", authObj)
                })
            }
        }.toString()

        val url = baseUrl.resolve("/WebUntis/jsonrpc_intern.do")
            ?.newBuilder()
            ?.addQueryParameter("m", "getUserData2017")
            ?.addQueryParameter("school", account.school)
            ?.addQueryParameter("v", "i2.2")
            ?.build() ?: throw UntisException.Protocol("Invalid baseUrl: $baseUrl")

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val responseBodyString = execute(request)

        val rpcResponse = try {
            UntisJson.decodeFromString<JsonRpcResponse<UserDataResultDto>>(responseBodyString)
        } catch (e: Exception) {
            throw UntisException.Protocol("Failed to parse getUserData2017 response: ${e.message}")
        }

        if (rpcResponse.error != null) {
            if (rpcResponse.error.code == -8504) {
                throw UntisException.BadCredentials(rpcResponse.error.message)
            } else {
                throw UntisException.Protocol("RPC error ${rpcResponse.error.code}: ${rpcResponse.error.message}")
            }
        }

        val result = rpcResponse.result
            ?: throw UntisException.Protocol("No result in getUserData2017 response")

        val masterData = toMasterData(result)
        personId = masterData.personId
        token = null
        tokenAt = 0L
        masterData
    }

    private suspend fun getBearerToken(): String {
        val currentToken = token
        val currentTokenAt = tokenAt
        val now = clock()
        if (currentToken != null && (now - currentTokenAt) <= 12 * 60 * 1000L) {
            return currentToken
        }

        val tokenUrl = baseUrl.resolve("/WebUntis/api/token/new")
            ?: throw UntisException.Protocol("Invalid token url from $baseUrl")
        val request = Request.Builder().url(tokenUrl).get().build()

        val resp = executeRaw(request)
        if (resp.code == 401 || resp.code == 403) {
            resp.close()
            login()
            val retryResp = executeRaw(request)
            if (!retryResp.isSuccessful) {
                val code = retryResp.code
                retryResp.close()
                throw UntisException.Http(code)
            }
            val newToken = retryResp.body.string().trim()
            token = newToken
            tokenAt = clock()
            return newToken
        } else if (!resp.isSuccessful) {
            val code = resp.code
            resp.close()
            throw UntisException.Http(code)
        }

        val newToken = resp.body.string().trim()
        token = newToken
        tokenAt = clock()
        return newToken
    }

    private suspend fun executeRaw(request: Request): okhttp3.Response = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw UntisException.Network(e)
        }
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val response = executeRaw(request)
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw UntisException.Http(resp.code)
            }
            resp.body.string()
        }
    }

    private suspend fun executeRestWithRetry(buildRequest: (token: String, pid: Long) -> Request): String = withContext(Dispatchers.IO) {
        if (personId == null) {
            login()
        }
        var pid = personId!!
        var currentToken = getBearerToken()
        var request = buildRequest(currentToken, pid)
        var response = executeRaw(request)

        if (response.code == 401 || response.code == 403) {
            response.close()
            login()
            pid = personId!!
            currentToken = getBearerToken()
            request = buildRequest(currentToken, pid)
            response = executeRaw(request)
        }

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw UntisException.Http(resp.code)
            }
            resp.body.string()
        }
    }

    suspend fun timetable(start: LocalDate, end: LocalDate): List<Day> = withContext(Dispatchers.IO) {
        val bodyString = executeRestWithRetry { currentToken, pid ->
            val url = baseUrl.resolve("/WebUntis/api/rest/view/v1/timetable/entries")
                ?.newBuilder()
                ?.addQueryParameter("start", start.toString())
                ?.addQueryParameter("end", end.toString())
                ?.addQueryParameter("format", "2")
                ?.addQueryParameter("resourceType", "STUDENT")
                ?.addQueryParameter("resources", pid.toString())
                ?.addQueryParameter("periodTypes", "")
                ?.addQueryParameter("timetableType", "MY_TIMETABLE")
                ?.build() ?: throw UntisException.Protocol("Invalid timetable url")

            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $currentToken")
                .get()
                .build()
        }

        try {
            val dto = UntisJson.decodeFromString<TimetableResponseDto>(bodyString)
            toDays(dto)
        } catch (e: Exception) {
            throw UntisException.Protocol("Failed to parse timetable response: ${e.message}")
        }
    }

    suspend fun absences(start: LocalDate, end: LocalDate): List<Absence> = withContext(Dispatchers.IO) {
        val formatter = DateTimeFormatter.BASIC_ISO_DATE
        val bodyString = executeRestWithRetry { currentToken, pid ->
            val url = baseUrl.resolve("/WebUntis/api/classreg/absences/students")
                ?.newBuilder()
                ?.addQueryParameter("startDate", start.format(formatter))
                ?.addQueryParameter("endDate", end.format(formatter))
                ?.addQueryParameter("studentId", pid.toString())
                ?.addQueryParameter("excuseStatusId", "-1")
                ?.build() ?: throw UntisException.Protocol("Invalid absences url")

            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $currentToken")
                .get()
                .build()
        }

        try {
            val dto = UntisJson.decodeFromString<AbsenceResponseDto>(bodyString)
            toAbsences(dto)
        } catch (e: Exception) {
            throw UntisException.Protocol("Failed to parse absences response: ${e.message}")
        }
    }

    companion object {
        suspend fun searchSchools(
            http: OkHttpClient,
            query: String,
            baseUrl: HttpUrl = "https://mobile.webuntis.com".toHttpUrl(),
        ): List<SchoolInfo> = withContext(Dispatchers.IO) {
            val searchObj = buildJsonObject {
                put("search", query)
            }
            val requestBodyJson = buildJsonObject {
                put("id", "1")
                put("jsonrpc", "2.0")
                put("method", "searchSchool")
                putJsonArray("params") {
                    add(searchObj)
                }
            }.toString()

            val url = baseUrl.resolve("/ms/schoolquery2")
                ?: throw UntisException.Protocol("Invalid baseUrl: $baseUrl")

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            val responseString = try {
                http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw UntisException.Http(resp.code)
                    }
                    resp.body.string()
                }
            } catch (e: IOException) {
                throw UntisException.Network(e)
            }

            val rpcResponse = try {
                UntisJson.decodeFromString<SchoolSearchResponseDto>(responseString)
            } catch (e: Exception) {
                throw UntisException.Protocol("Failed to parse school search response: ${e.message}")
            }

            toSchools(rpcResponse)
        }
    }

    private class InMemoryCookieJar : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            for (cookie in cookies) {
                list.removeAll { it.name == cookie.name }
                list.add(cookie)
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            return list.toList()
        }
    }
}

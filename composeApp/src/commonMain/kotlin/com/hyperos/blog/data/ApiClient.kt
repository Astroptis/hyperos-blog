package com.hyperos.blog.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiClient(
    @PublishedApi internal val baseUrls: List<String>,
    private val engine: HttpClientEngine,
) {
    @PublishedApi
    internal val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    @PublishedApi
    internal var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    suspend inline fun <reified T> get(path: String): ApiResponse<T> = request("GET", path)
    suspend inline fun <reified T> post(path: String, body: Any? = null): ApiResponse<T> = request("POST", path, body)
    suspend inline fun <reified T> put(path: String, body: Any? = null): ApiResponse<T> = request("PUT", path, body)
    suspend inline fun <reified T> delete(path: String): ApiResponse<T> = request("DELETE", path)

    @PublishedApi
    internal suspend inline fun <reified T> request(method: String, path: String, body: Any? = null): ApiResponse<T> {
        val baseList = baseUrls.ifEmpty { listOf("") }
        for (baseUrl in baseList) {
            try {
                val response = client.request("$baseUrl$path") {
                    this.method = HttpMethod(method)
                    contentType(ContentType.Application.Json)
                    if (body != null) setBody(body)
                    authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
                val status = response.status.value
                val text = response.body<String>()
                if (status == 401) return ApiResponse(ok = false, error = "Unauthorized")
                if (status >= 500) continue
                val json = Json { ignoreUnknownKeys = true }
                return json.decodeFromString<ApiResponse<T>>(text)
            } catch (e: Throwable) {
                // 网络错误，尝试下一个 baseUrl
            }
        }
        return ApiResponse(ok = false, error = "Network error")
    }
}
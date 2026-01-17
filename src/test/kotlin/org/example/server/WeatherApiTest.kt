package org.example.server

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class WeatherApiTest {

    @Test
    fun `can retrieve forecast`() = runTest {
        val result = HttpClient {
            defaultRequest {
                url("https://api.weather.gov")
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }.getForecast(37.7749, -122.4194)

        println(result)
    }
}
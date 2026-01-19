package org.example.server

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val weatherApiHttpClient = HttpClient {
    defaultRequest {
        url("https://api.weather.gov")
        headers {
            append("Accept", "application/geo+json")
            append("User-Agent", "WeatherApiClient/1.0")
        }
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

val headlineHttpClient = HttpClient {

}

fun runMcpServer() {
    val server = Server(
        serverInfo = Implementation(
            name = "weather",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    server.addTools(
        listOf(
            getGetAlertsTool(),
            getGetForecastTool()
        )
    )

    val transport = StdioServerTransport(
        inputStream = System.`in`.asInput(),
        outputStream = System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
    }
}

private fun getGetAlertsTool(): RegisteredTool = RegisteredTool(
    Tool(
        name = "get_alerts",
        description = """
                Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)
            """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("state") {
                    put("type", "string")
                    put("description", "Two-letter US state code (e.g. CA, NY")
                }
            },
            required = listOf("state")
        )
    )
) { request ->
    val state = request.arguments["state"]
        ?.jsonPrimitive
        ?.content ?: return@RegisteredTool CallToolResult(
        content = listOf(TextContent("The 'state' parameter is required."))
    )

    val alerts = weatherApiHttpClient.getAlerts(state)
    CallToolResult(content = alerts.map { TextContent(it) })
}

private fun getGetForecastTool(): RegisteredTool = RegisteredTool(
    Tool(
        name = "get_forecast",
        description = """
                    Get weather forecast for a specific latitude/longitude
                """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("latitude") { put("type", "number") }
                putJsonObject("longitude") { put("type", "number") }
            },
            required = listOf("latitude", "longitude")
        ),
    )
) { request ->
    val latitude = request.arguments["latitude"]?.jsonPrimitive?.doubleOrNull
    val longitude = request.arguments["longitude"]?.jsonPrimitive?.doubleOrNull

    if (latitude == null || longitude == null) {
        return@RegisteredTool CallToolResult(
            content = listOf(TextContent("The 'latitude' and 'longitude' parameters are required."))
        )
    }
    val forecast = weatherApiHttpClient.getForecast(latitude, longitude)
    CallToolResult(content = forecast.map { TextContent(it) })
}

fun main() = runMcpServer()

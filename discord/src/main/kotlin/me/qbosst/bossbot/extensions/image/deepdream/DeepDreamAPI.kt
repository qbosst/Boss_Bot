package me.qbosst.bossbot.extensions.image.deepdream

import com.kotlindiscord.kord.extensions.CommandException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging

private const val baseUrl = "https://api.deepai.org/api/deepdream"

private val client = HttpClient(CIO) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }

    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }

    expectSuccess = false
}

class DeepDreamAPI(private val token: String) {

    suspend fun process(imageUrl: String): String {
        val response: HttpResponse = client.submitForm(
            url = baseUrl,
            formParameters = Parameters.build { append("image", imageUrl) }
        ) {
            header("api-key", token)
            method = HttpMethod.Post
        }

        return when(response.status) {
            HttpStatusCode.OK ->
                response.receive<DeepDreamResponse.Ok>().outputUrl
            HttpStatusCode.Unauthorized ->
                throw CommandException("Cannot access DeepDream services right now. Please try again later.")
            else ->
                throw CommandException("An unknown error has occurred.")
        }
    }
}
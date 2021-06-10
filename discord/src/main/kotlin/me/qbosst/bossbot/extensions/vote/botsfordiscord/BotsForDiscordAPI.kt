package me.qbosst.bossbot.extensions.vote.botsfordiscord

import com.kotlindiscord.kord.extensions.utils.env
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.qbosst.bossbot.extensions.vote.VoteAPI

private const val baseURL = "https://botsfordiscord.com/api"

class BotsForDiscordAPI(token: String, id: Long): VoteAPI(token, id) {
    override val name: String get() = "BotsForDiscord"
    override val voteLink: String get() = "https://botsfordiscord.com/bot/$botId"
    override val markdownVoteLink: String get() = "[Bots For Discord]($voteLink)"

    override suspend fun postStats(guildCount: Int) {
        client.post<HttpResponse>("$baseURL/bot/$botId") {
            body = buildJsonObject {
                put("server_count", guildCount)
            }
        }
    }

    override suspend fun setup() {
        route("/botsfordiscord") {
            post {
                val authToken = env("botsfordiscord.auth")!!

                if(call.request.headers["Authorization"] == authToken) {
                    val response: VoteWebhook = call.receive()
                    call.respond(HttpStatusCode.OK)
                    sendVoteEvent(response.userId)
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
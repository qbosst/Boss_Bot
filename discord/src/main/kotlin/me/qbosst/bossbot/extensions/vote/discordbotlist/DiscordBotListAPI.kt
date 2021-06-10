package me.qbosst.bossbot.extensions.vote.discordbotlist

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

private const val baseURL = "https://discordbotlist.com/api/v1"

class DiscordBotListAPI(token: String, botId: Long): VoteAPI(token, botId) {
    override val name: String get() = "DiscordBotList"
    override val voteLink: String get() = "https://discordbotlist.com/bots/$botId"
    override val markdownVoteLink: String get() = "[Discord Bot List]($voteLink)"

    override suspend fun postStats(guildCount: Int) {
        client.post<HttpResponse>("$baseURL/bots/$botId/stats") {
            body = buildJsonObject {
                put("guilds", guildCount)
            }
        }
    }

    override suspend fun setup() {
        route("/discordbotlist") {
            post {
                val response: VoteWebhook = call.receive()
                call.respond(HttpStatusCode.OK)
                sendVoteEvent(response.id)
            }
        }
    }
}
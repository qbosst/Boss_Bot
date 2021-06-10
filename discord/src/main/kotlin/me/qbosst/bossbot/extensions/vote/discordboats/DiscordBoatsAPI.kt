package me.qbosst.bossbot.extensions.vote.discordboats

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

private const val baseURL = "https://discord.boats/api"

class DiscordBoatsAPI(token: String, botId: Long): VoteAPI(token, botId) {
    override val name: String get() = "DiscordBoats"
    override val voteLink: String get() = "https://discord.boats/bot/$botId"
    override val markdownVoteLink: String get() = "[Discord Boats]($voteLink)"

    override suspend fun postStats(guildCount: Int) {
        client.post<HttpResponse>("$baseURL/bot/$botId") {
            body = buildJsonObject {
                put("server_count", guildCount)
            }
        }
    }

    override suspend fun setup() {
        route("/discordboats") {
            post {
                val response: VoteWebhook = call.receive()
                call.respond(HttpStatusCode.OK)
                sendVoteEvent(response.user.id)
            }
        }
    }
}
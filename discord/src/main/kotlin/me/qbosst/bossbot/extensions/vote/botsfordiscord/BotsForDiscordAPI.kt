package me.qbosst.bossbot.extensions.vote.botsfordiscord

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.qbosst.bossbot.extensions.vote.VoteAPI

private const val baseURL = "https://botsfordiscord.com/api"

class BotsForDiscordAPI(token: String, id: Long): VoteAPI(token, id) {
    override val voteLink: String get() = "https://botsfordiscord.com/bot/$botId"
    override val markdownVoteLink: String get() = "[Bots For Discord]($voteLink)"

    override suspend fun postStats(guildCount: Int) {
        client.post<HttpResponse>("$baseURL/bot/$botId") {
            body = buildJsonObject {
                put("server_count", guildCount)
            }
        }
    }
}
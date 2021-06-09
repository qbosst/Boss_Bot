package me.qbosst.bossbot.extensions.vote.topgg

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.qbosst.bossbot.extensions.vote.VoteAPI

private const val baseURL = "https://top.gg/api"

class TopGgAPI(token: String, botId: Long): VoteAPI(token, botId) {
    override val voteLink: String get() = "https://top.gg/bot/$botId"
    override val markdownVoteLink: String get() = "[Top.gg]($voteLink)"

    override suspend fun postStats(guildCount: Int) {
        client.post<HttpResponse>("$baseURL/bots/$botId/stats") {
            body = buildJsonObject {
                put("server_count", guildCount)
            }
        }
    }
}
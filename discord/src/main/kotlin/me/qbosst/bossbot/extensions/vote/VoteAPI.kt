package me.qbosst.bossbot.extensions.vote

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*

abstract class VoteAPI(
    private val token: String,
    val botId: Long
) {
    protected val client = HttpClient(CIO) {
        defaultRequest {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
        }
    }

    abstract val voteLink: String

    abstract val markdownVoteLink: String

    abstract suspend fun postStats(guildCount: Int)
}
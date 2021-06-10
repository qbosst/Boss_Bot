package me.qbosst.bossbot.extensions.vote

import com.kotlindiscord.kord.extensions.ExtensibleBot
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import me.qbosst.bossbot.events.UserVoteEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class VoteAPI(
    private val token: String,
    val botId: Long,
): KoinComponent {
    private val bot: ExtensibleBot by inject()

    protected val client = HttpClient(CIO) {
        defaultRequest {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    abstract val name: String

    abstract val voteLink: String

    abstract val markdownVoteLink: String

    var route: Pair<String, Route.() -> Unit>? = null

    abstract suspend fun postStats(guildCount: Int)

    open suspend fun setup() {}

    @ContextDsl
    fun route(path: String, builder: Route.() -> Unit) { this.route = path to builder }

    suspend fun sendVoteEvent(userId: Long) = bot.send(UserVoteEvent(bot, userId, name))
}
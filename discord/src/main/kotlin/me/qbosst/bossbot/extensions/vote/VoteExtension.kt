package me.qbosst.bossbot.extensions.vote

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.count
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.database.dao.insertOrUpdate
import me.qbosst.bossbot.events.UserVoteEvent
import me.qbosst.bossbot.extensions.vote.botsfordiscord.BotsForDiscordAPI
import me.qbosst.bossbot.extensions.vote.discordboats.DiscordBoatsAPI
import me.qbosst.bossbot.extensions.vote.discordbotlist.DiscordBotListAPI
import me.qbosst.bossbot.extensions.vote.topgg.TopGgAPI
import me.qbosst.bossbot.util.hybridCommand
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration

class VoteExtension: Extension() {
    override val name: String get() = "vote"

    private lateinit var topGgApi: TopGgAPI
    private lateinit var discordBotListApi: DiscordBotListAPI
    private lateinit var discordBoatsApi: DiscordBoatsAPI
    private lateinit var botsForDiscordApi: BotsForDiscordAPI
    private var autoUpdateGuildCounts: Boolean = false /* can't test this rn */

    private val apis: List<VoteAPI> by lazy {
        listOf(
            topGgApi,
            discordBotListApi,
            discordBoatsApi,
            botsForDiscordApi
        )
    }

    private suspend fun updateGuildCounts(kord: Kord) {
        val guildCount = kord.guilds.count()

        val jobs = coroutineScope {
            apis.map { api ->
                async { api.postStats(guildCount) }
            }
        }

        jobs.awaitAll()
    }

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                val botId = kord.selfId.value

                topGgApi = TopGgAPI("", botId)
                discordBotListApi = DiscordBotListAPI("", botId)
                discordBoatsApi = DiscordBoatsAPI("", botId)
                botsForDiscordApi = BotsForDiscordAPI("", botId)

                if(autoUpdateGuildCounts) {
                    coroutineScope {
                        launch {
                            while (true) {
                                updateGuildCounts(kord)
                                delay(Duration.hours(1))
                            }
                        }
                    }
                }
            }
        }

        event<UserVoteEvent> {
            action {
                val user = event.getUser()

                newSuspendedTransaction {
                    user?.getUserDAO(this).insertOrUpdate(this, event.userId) {
                        tokens += 150
                    }
                }
            }
        }

        hybridCommand {
            name = "vote"
            description = "Displays the websites where you can vote for Boss Bot"

            action {
                publicFollowUp {
                    allowedMentions {}

                    embed {
                        description = apis.joinToString("\n") { it.markdownVoteLink }
                    }
                }
            }
        }
    }
}
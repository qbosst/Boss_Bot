package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.converters.optionalChannel
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import me.qbosst.bossbot.Constants
import me.qbosst.bossbot.database.models.GuildSettings
import me.qbosst.bossbot.database.models.getOrRetrieveSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.util.cache.MessageCache.Companion.files
import me.qbosst.bossbot.util.ext.deleteAll
import me.qbosst.bossbot.util.ext.reply
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class LoggerExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "logger"

    class SetMessageLogsArgs: Arguments() {
        val logChannel by optionalChannel("new log channel", "", outputError = true)
    }

    override suspend fun setup() {
        group {
            name = "set"

            check(::anyGuild)

            action {
                val settings = newSuspendedTransaction {
                    guildFor(event)!!.getOrRetrieveSettings()
                }
                println(settings)
            }

            command(::SetMessageLogsArgs) {
                name = "messagelogs"
                aliases = arrayOf("messagelog", "msglogs", "msglog")

                action {
                    val guild = event.getGuild()!!
                    val settings = guild.getOrRetrieveSettings()
                    transaction {
                        if(settings == null) {
                            GuildSettings.new(guild.id.value) {
                                messageLogsChannelId = arguments.logChannel?.id?.value
                            }
                        } else {
                            settings.messageLogsChannelId = arguments.logChannel?.id?.value
                        }
                    }
                }
            }
        }

        event<MessageDeleteEvent> {
            // make sure we only get guild message delete events.
            check(::anyGuild)

            action {
                val message = event.message
                val files = message?.data?.files ?: listOf()

                kotlin.run {
                    val logger = event.logger() ?: return@run

                    // convert files into a map of input streams, for the message to use
                    val streams = files.map { file -> file.name to file.inputStream() }

                    logger.createMessage {
                        embed {
                            author {
                                this.name = "Message Deleted"
                                this.icon = message?.author?.avatar?.url
                            }

                            field("Channel", true) { event.channel.mention }
                            field("Author", true) { "todo" }
                            if(streams.isNotEmpty()) {
                                field("Attachments", true) { streams.size.toString() }
                            }
                            field("Content", true) { message?.content ?: "N/A" }
                            footer {
                                text = buildString {
                                    val userId = message?.author?.id?.value
                                    if(userId != null) {
                                        append("User ID: $userId | ")
                                    }
                                    append("Message ID: ${event.messageId.value}")
                                }
                            }

                            color = Color(Constants.PASTEL_RED)
                            timestamp = Instant.now()
                        }

                        // add deleted message attachments to log message
                        streams.forEach { (fileName, stream) ->
                            addFile(fileName, stream)
                        }
                    }

                    // once the message has been sent, we close the input streams so that it can be deleted
                    streams.forEach { (_, stream) -> stream.close() }
                }

                // delete files from storage
                files.deleteAll()
            }
        }
    }

    private suspend fun Event.logger(): MessageChannelBehavior? {
        val guild = guildFor(this) ?: return null
        val settings = guild.getOrRetrieveSettings() ?: return null

        return when(this) {
            // message events
            is MessageUpdateEvent, is MessageDeleteEvent ->
                settings.messageLogsChannelId?.let { id -> guild.getChannelOfOrNull<GuildMessageChannel>(Snowflake(id)) }
            else -> null
        }
    }
}
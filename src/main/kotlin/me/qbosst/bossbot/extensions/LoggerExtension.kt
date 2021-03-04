package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.ChannelConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.StringCoalescingConverter
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorIsBot
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
import me.qbosst.bossbot.commands.ConfigCommandArgs
import me.qbosst.bossbot.commands.ConfigSubCommand
import me.qbosst.bossbot.commands.converters.SingleToCoalescingConverter
import me.qbosst.bossbot.database.dao.GuildSettings
import me.qbosst.bossbot.database.dao.getSettings
import me.qbosst.bossbot.database.dao.insertOrUpdate
import me.qbosst.bossbot.util.Colour
import me.qbosst.bossbot.util.cache.files
import me.qbosst.bossbot.util.ext.*
import me.qbosst.bossbot.util.isNotBot
import me.qbosst.bossbot.util.kColour
import java.time.Instant

class LoggerExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "logger"

    private val defaultPrefix: String get() = bot.settings.messageCommandsBuilder.defaultPrefix

    override suspend fun setup() {
        event<MessageDeleteEvent> {
            check(::anyGuild, ::isNotBot)

            action {
                val message = event.message
                val files = message?.data?.files

                run {
                    val logger = event.logger() ?: return@run

                    val streams = files?.map { file -> file.name to file.inputStream() }

                    logger.createMessage {

                        val author = message?.author

                        // create embed, containing details about the deleted message
                        embed {

                            author {
                                this.name = "Message Deleted"
                                this.icon = message?.author?.avatar?.url
                            }

                            // the channel the message was deleted in
                            field("Channel", true) { event.channel.mention }

                            // the author of the message
                            field("Author", true) { if(author != null) "${author.tag} ${author.mention}" else "N/A" }

                            if(!streams.isNullOrEmpty()) {
                                field("Attachments", true) { streams.size.toString() }
                            }

                            // add a field to display the content of the message (if any)
                            val originalContent = message?.content?.maxLength(EmbedBuilder.Field.Limits.value)
                            if(originalContent == null || originalContent.isNotEmpty()) {
                                field("Content", true) { originalContent ?: "N/A" }
                            }

                            footer {
                                text = (author?.id?.value?.let { "User ID: $it | " } ?: "") + "Message ID: ${event.messageId.value}"
                            }

                            color = Colour.LIGHT_CORAL.kColour
                            timestamp = Instant.now()
                        }

                        // add deleted message attachments to log message
                        streams?.forEach { (fileName, stream) ->
                            addFile(fileName, stream)
                        }
                    }

                    // once the message has been sent, we close the input streams so we can delete the files
                    streams?.forEach { (_, stream) -> stream.close() }
                }

                // delete the files from storage
                files?.deleteAll()
            }
        }

        event<MessageUpdateEvent> {

            check(::anyGuild, ::isNotBot)

            action {
                val logger = event.logger() ?: return@action

                val newMessage = event.message.asMessage()
                val oldMessage = event.old
                val author = newMessage.author!!


                logger.createEmbed {
                    author {
                        name = "Message Edited"
                        icon = author.avatar.url
                    }

                    field("Channel", true) { event.channel.mention }
                    field("Author", true) { author.mention }
                    field("Message", true) { "[Jump to Message](${newMessage.jumpUrl})" }
                    field("Message Content Before", true) { oldMessage?.content?.zeroWidthIfBlank() ?: "N/A" }
                    field("Message Content After", true) { newMessage.content.zeroWidthIfBlank() }

                    footer {
                        text = "User ID: ${author.id.value} | Message ID: ${newMessage.id.value}"
                    }

                    timestamp = Instant.now()
                    color = Colour.SANDY_BROWN.kColour
                }
            }
        }

        group {
            name = "settings"

            check(::anyGuild)

            action {
                val guild = event.getGuild()!!
                val settings = guild.getSettings()

                message.replyEmbed {
                    field("Prefix", true) { (settings.prefix ?: defaultPrefix).wrap("`") }
                    field("Message Logs Channel", true) { settings.messageLogsChannelId?.channelMention() ?: "N/A" }
                }
            }

            configCommand("prefix", "The new prefix", StringCoalescingConverter()) {
                name = "prefix"

                getSetting { this?.prefix }

                updateSetting { guildId, guildSettings, newPrefix ->
                    guildSettings.insertOrUpdate(guildId) {
                        prefix = newPrefix
                    }
                }

                formatValue { (it ?: defaultPrefix).wrap("`") }
            }

            configCommand("message logs", "", SingleToCoalescingConverter(ChannelConverter())) {
                name = "messagelogs"

                getSetting { ctx ->
                    this?.messageLogsChannelId?.snowflake()?.let { id -> ctx.getGuild()?.getChannelOrNull(id) }
                }

                updateSetting { guildId, guildSettings, newChannel ->
                    guildSettings.insertOrUpdate(guildId) {
                        messageLogsChannelId = newChannel?.id?.value
                    }
                }

                formatValue { it?.mention ?: "`N/A`" }
            }
        }
    }

    private suspend fun <V: Any> GroupCommand<out Arguments>.configCommand(
        displayName: String,
        description: String,
        converter: CoalescingConverter<V>,
        body: ConfigSubCommand<Long, GuildSettings, V>.() -> Unit,
    ): MessageSubCommand<ConfigCommandArgs<V>> {
        val commandObj = ConfigSubCommand<Long, GuildSettings, V>(displayName, description, converter, extension, this)

        commandObj.apply {
            body()

            check(::anyGuild)

            getConfig { getGuild()?.getSettings() }

            getPrimaryKey { event.guildId!!.value }
        }


        return command(commandObj) as MessageSubCommand<ConfigCommandArgs<V>>
    }

    private suspend fun Event.logger(): MessageChannelBehavior? {
        val guild = guildFor(this) ?: return null
        val settings = guild.getSettings()

        return when(this) {
            is MessageUpdateEvent, is MessageDeleteEvent ->
                settings.messageLogsChannelId?.snowflake()?.let { guild.getChannelOfOrNull<GuildMessageChannel>(it) }
            else ->
                null
        }
    }
}

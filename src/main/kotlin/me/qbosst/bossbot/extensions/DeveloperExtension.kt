package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.converters.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.users
import dev.kord.core.behavior.channel.withTyping
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import me.qbosst.bossbot.util.ext.reply

class DeveloperExtension(bot: ExtensibleBot, val developers: Collection<Long>): Extension(bot) {
    private val json = Json {
        prettyPrint = true
    }

    override val name: String = "developer"

    override suspend fun setup() {
        command(botStatisticsCommand)
        command(readDirectMessagesCommand)
    }

    private val botStatisticsCommand: suspend MessageCommand.() -> Unit = {
        name = "botstatistics"
        aliases = arrayOf("botstats")

        action {
            message.reply(false) {
                embed {
                    color = message.getGuildOrNull()?.getMember(message.kord.selfId)?.getTopRole()?.color
                    field("Guilds", inline = true) { message.kord.guilds.count().toString() }
                    field("Cached Users", inline = true) { message.kord.users.count().toString() }
                }
            }
        }
    }

    private val readDirectMessagesCommand: suspend MessageCommand.() -> Unit = {
        class Args: Arguments() {
            val target by user("target", "The user to read the DM history with")
            val amount by defaultingInt("amount", "The amount of messages to read", defaultValue = 100)
        }

        name = "readdms"


        signature(::Args)
        action {
            with(parse(::Args)) {
                when {
                    target.id == message.kord.selfId ->
                        message.reply(false) {
                            content = "I cannot check DM history with myself."
                        }
                    target.isBot ->
                        message.reply(false) {
                            content = "I cannot check DM history with other bots."
                        }
                    else -> {
                        val channel = target.getDmChannel()
                        val lastMessage = channel.getLastMessage()
                        if(lastMessage == null) {
                            message.reply(false) {
                                content = "I do not have any DMs with ${target.tag}"
                            }
                        } else {
                            message.channel.withTyping {
                                val messages = buildJsonArray {
                                    add(json.encodeToJsonElement(lastMessage.data))
                                    channel.getMessagesBefore(lastMessage.id, amount)
                                        .collect { message -> add(json.encodeToJsonElement(message)) }
                                }

                                message.reply(false) {
                                    addFile(
                                        "${target.tag}_dms.json",
                                        json.encodeToString(messages).byteInputStream()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun command(body: suspend MessageCommand.() -> Unit) = super.command(body)
        .apply {
            check { event -> event.message.data.authorId.value in developers }
        }

    override suspend fun command(commandObj: MessageCommand) = super.command(commandObj)
        .apply {
            check { event -> event.message.data.authorId.value in developers }
        }

}
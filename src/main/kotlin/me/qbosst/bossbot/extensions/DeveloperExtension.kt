package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.converters.defaultingNumber
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.users
import dev.kord.core.behavior.channel.withTyping
import dev.kord.rest.json.request.AllowedMentionType
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import me.qbosst.bossbot.util.ext.reply

class DeveloperExtension(
    bot: ExtensibleBot,
    val developers: Collection<Long>,
): Extension(bot) {

    private val json = Json {
        prettyPrint = true
    }

    override val name: String = "dev"

    override suspend fun setup() {
        command(readDirectMessagesCommand)
        command(botStatisticsCommand)
        command(testCommand)
    }

    private val botStatisticsCommand: suspend Command.() -> Unit = {
        name = "botstatistics"
        aliases = arrayOf("botstats")

        action {
            message.reply(false) {
                embed {
                    color = message.getGuildOrNull()?.getMember(message.kord.selfId)?.getTopRole()?.color
                    field {
                        name = "Guilds"
                        value = message.kord.guilds.count().toString()
                        inline = true
                    }

                    field {
                        name = "Cached Users"
                        value = message.kord.users.count().toString()
                        inline = true
                    }
                }
            }
        }
    }

    private val readDirectMessagesCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val target by user("target")
            val amount by defaultingNumber("amount", 100)
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
                                    channel.getMessagesBefore(lastMessage.id, amount.toInt())
                                        .collect { message -> add(json.encodeToJsonElement(message.data)) }
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

    private val testCommand: suspend Command.() -> Unit = {
        name = "test"

        action {
            message.reply(false) {
                content = "@everyone"
            }
        }
    }

    override suspend fun command(body: suspend Command.() -> Unit): Command =
        super.command(body)
            .apply {
                check { event -> event.message.data.authorId.value in developers }
            }

    override suspend fun command(commandObj: Command): Command =
        super.command(commandObj)
            .apply {
                check { event -> event.message.data.authorId.value in developers }
            }
}
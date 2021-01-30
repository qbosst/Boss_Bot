package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.users
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.event.message.MessageUpdateEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import me.qbosst.bossbot.util.defaultCheck
import me.qbosst.bossbot.util.ext.reply

@OptIn(KordPreview::class)
class DeveloperExtension(bot: ExtensibleBot, val developers: Collection<Long>): Extension(bot) {
    private val json = Json {
        prettyPrint = true
    }

    override val name: String = "developer"

    class ReadDirectMessagesArgs: Arguments() {
        val target by user("target", "")
        val amount by defaultingInt("amount", "", 100)
    }

    override suspend fun setup() {
        command {
            name = "botstatistics"
            aliases = arrayOf("botstats")

            check(::defaultCheck)

            action {
                message.reply(false) {
                    embed {
                        color = guild?.getMember(message.kord.selfId)?.getTopRole()?.color
                        field("Guilds", true) { message.kord.guilds.count().toString() }
                        field("Cached Users", true) { message.kord.users.count().toString() }
                    }
                }
            }
        }

        command(::ReadDirectMessagesArgs) {
            name = "readdirectmessages"
            aliases = arrayOf("readdms")

            check(::defaultCheck)

            action {
                val target = arguments.target
                when {
                    target.id == message.kord.selfId -> {
                        message.reply(false) {
                            content = "I cannot check DM history with myself."
                        }
                    }
                    target.isBot -> {
                        message.reply(false) {
                            content = "I cannot check DM history with other bots."
                        }
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
                                    add(json.encodeToJsonElement(lastMessage))
                                    channel.getMessagesBefore(lastMessage.id, arguments.amount)
                                        .collect { message -> add(json.encodeToJsonElement(message)) }
                                }

                                message.reply(false) {
                                    addFile("${target.tag}_dms.json", json.encodeToString(messages).byteInputStream())
                                }
                            }
                        }
                    }
                }
            }
        }

        // puts a check to see if the author is a developer for every command in this extension.
        commands.forEach { command ->
            command.checkList.add(0) { event -> event.message.data.authorId.value in developers }
        }
    }
}
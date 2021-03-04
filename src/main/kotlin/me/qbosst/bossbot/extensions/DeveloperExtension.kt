package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.users
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.entity.ReactionEmoji
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import me.qbosst.bossbot.util.ext.maxLength
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.replyEmbed
import me.qbosst.bossbot.util.ext.wrap
import javax.script.ScriptEngineManager

class DeveloperExtension(bot: ExtensibleBot, val developerIds: List<Long>): Extension(bot) {
    override val name: String = "developer"

    private val engine by lazy { ScriptEngineManager().getEngineByExtension("kts") }

    private val prettyJson = Json {
        prettyPrint = true
    }

    class EvalArgs: Arguments() {
        val code by coalescedString("code", "")
    }

    class ReadDMsArgs: Arguments() {
        val target by user("target", "the user's DMs that you want to read")
        val amount by defaultingInt("amount", "the amount of messages to read", 100)
    }

    override suspend fun setup() {
        check { it.message.data.authorId.value in developerIds }

        command(::EvalArgs) {
            name = "eval"

            action {
                channel.withTyping {

                    engine.apply {
                        put("event", event)
                        put("kord", kord)
                        put("guild", guild)
                        put("message", message)
                        put("member", member)
                        put("user", user)
                        put("gateway", event.gateway)
                        put("channel", channel)
                    }

                    val result = try { engine.eval(arguments.code) } catch (e: Exception) { e }

                    message.reply(false) {
                        content = "Result: $result".wrap("```").maxLength(2000, "...```")
                    }
                }
            }
        }

        command(::ReadDMsArgs) {
            name = "readdms"

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
                        val dmChannel = target.getDmChannelOrNull()
                        val lastMessage = dmChannel?.getLastMessage()

                        when {
                            dmChannel == null -> {
                                message.reply(false) {
                                    content = "I could not get the DM channel with ${target.tag}. Maybe they have me blocked or DMs off..."
                                }
                            }
                            lastMessage == null -> {
                                message.reply(false) {
                                    content = "I do not have any DMs with ${target.tag}"
                                }
                            }
                            else -> {
                                message.channel.withTyping {
                                    val messages = buildJsonArray {
                                        add(prettyJson.encodeToJsonElement(lastMessage))
                                        channel.getMessagesBefore(lastMessage.id, arguments.amount)
                                            .collect { message -> add(prettyJson.encodeToJsonElement(message)) }
                                    }

                                    prettyJson.encodeToString(messages).byteInputStream().use {
                                        message.reply(false) {
                                            addFile("${target.tag}_dms.json", it)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        command {
            name = "botstatistics"
            aliases = arrayOf("botstats")

            action {
                val self = event.kord.getSelf()

                val guilds = event.kord.guilds.count()
                val cachedUsers = event.kord.users.count()

                val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
                val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

                message.replyEmbed {
                    title = "${self.tag} statistics"

                    field("Memory Usage", true) { "${usedMb}MB / ${totalMb}MB" }
                    field("Guilds", true) { guilds.toString() }
                    field("Cached Users", true) { cachedUsers.toString() }
                }
            }
        }

        command {
            name = "shutdown"

            action {
                event.message.reply(false) {
                    content = "Bye... :("
                }

                event.kord.shutdown()
            }
        }
    }
}
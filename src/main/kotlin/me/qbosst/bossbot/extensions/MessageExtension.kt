package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.EmbedRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.toEmbedBuilder
import me.qbosst.bossbot.util.nextColour
import java.time.Instant
import kotlin.random.Random

class MessageExtension(bot: ExtensibleBot): Extension(bot) {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override val name: String = "message"

    class EmbedArgs: Arguments() {
        val code by coalescedString("code", "")
    }

    override suspend fun setup() {
        group(::EmbedArgs) {
            name = "embed"

            action {
                val embed = try {
                    json.decodeFromString<EmbedRequest>(arguments.code)
                } catch (e: SerializationException) {
                    message.reply(false) {
                        this.content = "```Could not parse JSON: ${e.localizedMessage.lines().first()}```"
                    }
                    return@action
                }

                channel.createMessage {
                    this.embed = embed.toEmbedBuilder()
                }
            }

            command {
                name = "example"
                aliases = arrayOf("template")

                action {
                    val element = EmbedBuilder().apply {
                        description = "This is the description!"
                        title = "This is the title!"
                        timestamp = Instant.now()
                        field("First Field", inline = true) { "This is the first field!" }
                        footer {
                            text = "Footer Text!"
                            icon = message.author!!.avatar.url
                        }
                        author {
                            name = "Author: ${message.author!!.tag}"
                            icon = message.author!!.avatar.url
                            url = message.author!!.avatar.url
                        }
                        image = message.author!!.avatar.defaultUrl
                        thumbnail {
                            url = message.kord.getSelf().avatar.url
                        }
                        color = Random.nextColour()
                    }.toRequest()

                    message.reply(false) {
                        addFile("template_embed.json", json.encodeToString(element).byteInputStream())
                    }
                }
            }
        }
    }
}
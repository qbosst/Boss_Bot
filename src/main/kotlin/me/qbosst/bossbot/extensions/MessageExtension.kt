package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.kColor
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.EmbedRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.toEmbedBuilder
import java.time.Instant
import kotlin.random.Random

class MessageExtension(bot: ExtensibleBot): Extension(bot) {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override val name: String = "message"

    override suspend fun setup() {
        group(embedGroup)
    }

    private val embedGroup: suspend GroupCommand.() -> Unit = {
        name = "embed"

        action {
            val content = when {
                args.isNotEmpty() -> args.joinToString(" ")

                else -> throw ParseException("Please provide JSON")
            }

            val embed = try {
                json.decodeFromString<EmbedRequest>(content)
            } catch (e: SerializationException) {
                message.reply(false) {
                    this.content = "```Could not parse JSON: ${e.localizedMessage.lines().first()}```"
                }
                return@action
            }
            channel!!.createMessage {
                this.embed = embed.toEmbedBuilder()
            }
        }

        command(embedExampleCommand)
    }

    private val embedExampleCommand: suspend MessageCommand.() -> Unit = {
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
                color = Random.nextColour().kColor
            }.toRequest()

            message.reply(false) {
                addFile("template_embed.json", json.encodeToString(element).byteInputStream())
            }
        }
    }
}
package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.long
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.json.request.MessageCreateRequest
import dev.kord.rest.request.KtorRequestException
import dev.kord.rest.route.Route
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.util.ext.isEmpty
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.nextColour
import java.time.Instant
import kotlin.random.Random

class MessageExtension(bot: ExtensibleBot): Extension(bot) {

    override val name: String = "message"

    private val prettyPrintJson = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // lol the naming
    private val uglyPrintJson = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    class EmbedArgs: Arguments() {
        val code by coalescedString("code", "")
    }

    class EmbedTemplateArgs: Arguments() {
        val prettyPrint by defaultingBoolean("pretty print", "", true)
    }

    class EmbedGetArgs: Arguments() {
        val id by long("message id", "")
        val prettyPrint by defaultingBoolean("pretty print", "", true)
    }

    override suspend fun setup() {
        group(::EmbedArgs) {
            name = "embed"

            action {
                try {
                    // deserialize json to embed
                    val request = prettyPrintJson.decodeFromString<EmbedRequest>(arguments.code)

                    // check if embed is empty, if so throw error
                    if(request.isEmpty()) {
                        throw IllegalArgumentException("Cannot send an empty embed.")
                    }

                    // send embed
                    event.kord.rest.unsafe(Route.MessagePost) {
                        keys[Route.ChannelId] = channel.id
                        val multipartRequest = MessageCreateRequest(embed = request.optional())
                        body(MessageCreateRequest.serializer(), multipartRequest)
                    }

                } catch (e: Exception) {
                    val error = when(e) {
                        is SerializationException ->
                            "```Could not parse JSON: ${e.localizedMessage.lines().first()}```"
                        is IllegalArgumentException, is KtorRequestException ->
                            "```Could not send embed: ${e.localizedMessage}```"
                        else -> throw e
                    }

                    message.reply(false) {
                        content = error
                    }
                }
            }

            command(::EmbedTemplateArgs) {
                name = "example"
                aliases = arrayOf("template")

                action {
                    val self = message.kord.getSelf()
                    val embed = buildEmbed {
                        description = "This is the description!"
                        title = "This is the title!"
                        timestamp = Instant.now()
                        field("First Field", inline = true) { "This is the first field!" }
                        field("Second Field", inline = false) { "You can have more than one fields" }
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
                            url = self.avatar.url
                        }
                        color = Random.nextColour()
                    }

                    val json = if(arguments.prettyPrint) prettyPrintJson else uglyPrintJson

                    json.encodeToString(embed).byteInputStream().use {
                        message.reply(false) {
                            addFile("template_embed.json", it)
                        }
                    }
                }
            }

            command(::EmbedGetArgs) {
                name = "get"

                action {
                    val requestedMessage = channel.getMessageOrNull(Snowflake(arguments.id))
                    if(requestedMessage == null) {
                        message.reply(false) {
                            content = "Could not find message"
                        }
                    } else {
                        val json = if(arguments.prettyPrint) prettyPrintJson else uglyPrintJson

                        json.encodeToString(requestedMessage.data.embeds).byteInputStream().use {
                            message.reply(false) {
                                addFile("${arguments.id}_embeds.json", it)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun buildEmbed(builder: EmbedBuilder.() -> Unit) = EmbedBuilder().apply(builder).toRequest()
    }
}
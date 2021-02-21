package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.long
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.DiscordMessage
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.json.request.MessageCreateRequest
import dev.kord.rest.json.request.MultipartMessageCreateRequest
import dev.kord.rest.request.KtorRequestException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.util.Colour
import me.qbosst.bossbot.util.ext.*
import me.qbosst.bossbot.util.kColour
import java.time.Instant

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
                    val request = prettyPrintJson.decodeFromString<EmbedRequest>(arguments.code)

                    // validates the embed
                    request.validate()

                    // create request message
                    val multipartRequest = MultipartMessageCreateRequest(request = MessageCreateRequest(embed = request.optional()))

                    // send message
                    event.kord.rest.channel.createMessage(channel.id, multipartRequest)

                } catch (e: Exception) {
                    val error = when(e) {
                        is SerializationException ->
                            "Could not parse JSON: ${e.localizedMessage.lines().first()}".wrap("```")
                        is IllegalArgumentException ->
                            "Invalid embed received: ${e.localizedMessage}".wrap("```")
                        is KtorRequestException ->
                            "Could not send embed: ${e.localizedMessage}".wrap("```")
                        else ->
                            "An unknown error has occurred... please make sure you provide valid input."
                    }

                    message.reply(false) {
                        content = error.maxLength(2000)
                    }
                }
            }

            command(::EmbedTemplateArgs) {
                name = "template"

                aliases = arrayOf("example")

                action {
                    val self = message.kord.getSelf()

                    val embed = EmbedBuilder().apply {
                        title = "This is the title! Titles cannot be longer than ${EmbedBuilder.Limits.title} characters."
                        description = "This is the description! Descriptions cannot be longer than ${EmbedBuilder.Limits.description} characters."
                        timestamp = Instant.now()
                        color = Colour.random(false).kColour

                        field("First Field", inline = true) { "This is the first field!"}
                        field("Second Field", inline = false) { "You can have up to ${EmbedBuilder.Limits.fieldCount} fields!"}

                        footer {
                            text = "Footers cannot be longer than ${EmbedBuilder.Footer.Limits.text} characters."
                            icon = message.author!!.avatar.url
                        }
                        author {
                            name = "Author: ${message.author!!.tag}. Embed author texts cannot be longer than ${EmbedBuilder.Author.Limits.name} characters."
                            icon = message.author!!.avatar.url
                            url = message.author!!.avatar.url
                        }
                        image = message.author!!.avatar.defaultUrl
                        thumbnail {
                            url = self.avatar.url
                        }
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
                    val requestedMessage = channel.getMessageOrNull(arguments.id.snowflake())
                    if(requestedMessage == null) {
                        message.reply(false) {
                            content = "Could not find message by id: ${arguments.id}."
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
}
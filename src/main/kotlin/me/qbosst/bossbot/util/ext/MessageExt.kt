package me.qbosst.bossbot.util.ext

import dev.kord.common.Color
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.*
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import dev.kord.rest.json.request.*
import dev.kord.rest.request.RestRequestException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Request to reply to this message, setting [MessageCreateBuilder.messageReference] to this message [id][MessageBehavior.id].
 *
 * @param mentionUser Whether to mention the user in the reply
 *
 * @throws [RestRequestException] if something went wrong during the request.
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun MessageBehavior.reply(
    mentionUser: Boolean = false,
    builder: MessageCreateBuilder.() -> Unit
): Message {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return channel.createMessage {
        builder()
        messageReference = this@reply.id
        allowedMentions {
            repliedUser = mentionUser
        }
    }
}

fun EmbedRequest.toEmbedBuilder(): EmbedBuilder {
    fun EmbedFooterRequest.toFooterBuilder(): EmbedBuilder.Footer = EmbedBuilder.Footer().apply {
        this.text = this@toFooterBuilder.text
        this.icon = this@toFooterBuilder.iconUrl
    }

    fun EmbedThumbnailRequest.toThumbnailBuilder(): EmbedBuilder.Thumbnail? {
        val url = this.url
        if(url == null) {
            return null
        } else {
            return EmbedBuilder.Thumbnail().apply {
                this.url = url
            }
        }
    }

    fun EmbedAuthorRequest.toAuthorBuilder(): EmbedBuilder.Author = EmbedBuilder.Author().apply {
        this.name = this@toAuthorBuilder.name.value
        this.url = this@toAuthorBuilder.url.value
        this.icon = this@toAuthorBuilder.iconUrl.value
    }

    fun List<EmbedFieldRequest>.toFieldBuilder(): MutableList<EmbedBuilder.Field> = map { field ->
        EmbedBuilder.Field().apply {
            this.name = field.name
            this.value = field.value
            this.inline = field.inline.value
        }
    }.toMutableList()

    val builder = EmbedBuilder()

    builder.title = this.title.value
    builder.description = this.description.value
    builder.url = this.url.value
    builder.title = this.timestamp.value
    builder.color = this.color.value
    builder.image = this.image.value?.url
    builder.footer = this.footer.value?.toFooterBuilder()
    builder.thumbnail = this.thumbnail.value?.toThumbnailBuilder()
    builder.author = this.author.value?.toAuthorBuilder()
    builder.fields = this.fields.value?.toFieldBuilder() ?: mutableListOf()

    return builder
}

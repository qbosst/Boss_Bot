package me.qbosst.bossbot.util.ext

import dev.kord.common.entity.DiscordPartialMessage
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.request.RestRequestException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

val UserData.tag: String get() = "${username}#${discriminator}"

/**
 * Replies to the message with an embed
 *
 * @param mention whether to mention the user in the reply
 * @param builder dsl function for building an embed
 *
 * @return the message that was sent
 */
@OptIn(ExperimentalContracts::class)
suspend fun MessageBehavior.replyEmbed(mention: Boolean, builder: EmbedBuilder.() -> Unit): Message {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return channel.createMessage {
        embed(builder)
        messageReference = this@replyEmbed.id
        allowedMentions {
            repliedUser = mention
        }
    }
}

/**
 * Replies to the message with an embed. Replies will not mention the user.
 *
 * @param builder dsl function for building an embed.
 *
 * @return the message that was sent
 */
suspend fun MessageBehavior.replyEmbed(builder: EmbedBuilder.() -> Unit): Message = replyEmbed(false, builder)

@OptIn(ExperimentalContracts::class)
suspend fun MessageBehavior.reply(mention: Boolean, builder: MessageCreateBuilder.() -> Unit): Message {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return channel.createMessage {
        builder()
        messageReference = this@reply.id
        allowedMentions {
            repliedUser = mention
        }
    }
}

/**
 * Jump URL for the message
 */
val Message.jumpUrl: String
    get() = "https://discord.com/channels/${data.guildId.value ?: "@me"}/${channelId.value}/${id.value}"

/**
 * Jump URL for the message
 */
val DiscordPartialMessage.jumpUrl: String
    get() = "https://discord.com/channels/${guildId.value ?: "@me"}/${channelId.value}/${id.value}"


package me.qbosst.bossbot.util.ext

import dev.kord.common.entity.DiscordPartialMessage
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

val Message.jumpUrl: String
    get() = "https://discord.com/channels/${data.guildId.value ?: "@me"}/${data.channelId.value}/${data.id.value}"

val DiscordPartialMessage.jumpUrl: String
    get() = "https://discord.com/channels/${guildId.value ?: "@me"}/${channelId.value}/${id.value}"
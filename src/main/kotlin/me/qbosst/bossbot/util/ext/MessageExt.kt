package me.qbosst.bossbot.util.ext

import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.MessageCreateBuilder
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

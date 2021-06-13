package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.MessageCommandContext
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import dev.kord.core.Kord
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HybridCommandContext<T: Arguments>(val context: CommandContext): KoinComponent {
    private val bot: ExtensibleBot by inject()

    val kord: Kord get() = context.eventObj.kord
    val eventObj: Event get() = context.eventObj

    val channel: MessageChannelBehavior get() = when(context) {
        is SlashCommandContext<*> -> context.channel
        is MessageCommandContext<*> -> context.channel
        else -> error("Unknown context type provided.")
    }

    val guild: Guild? get() = when(context) {
        is SlashCommandContext<*> -> context.guild
        is MessageCommandContext<*> -> context.guild
        else -> error("Unknown context type provided.")
    }

    val member: MemberBehavior? get() = when(context) {
        is SlashCommandContext<*> -> context.member
        is MessageCommandContext<*> -> context.member
        else -> error("Unknown context type provided.")
    }

    val user: UserBehavior? get() = when(context) {
        is SlashCommandContext<*> -> context.user
        is MessageCommandContext<*> -> context.user
        else -> error("Unknown context type provided")
    }

    val message: Message? get() = when(context) {
        is SlashCommandContext<*> -> null
        is MessageCommandContext<*> -> context.message
        else -> error("Unknown context type provided.")
    }

    @Suppress("UNCHECKED_CAST")
    val arguments: T get() = when(context) {
        is SlashCommandContext<*> -> context.arguments
        is MessageCommandContext<*> -> context.arguments
        else -> error("Unknown context type provided.")
    } as T

    suspend fun getPrefix() = when(context) {
        is SlashCommandContext<*> -> "/"
        is MessageCommandContext<*> -> with(bot.settings.messageCommandsBuilder) {
            prefixCallback.invoke(context.eventObj as MessageCreateEvent, defaultPrefix)
        }
        else -> error("Unknown context type provided.")
    }

    suspend inline fun publicFollowUp(builder: PublicHybridMessageCreateBuilder.() -> Unit): Message {
        val messageBuilder = PublicHybridMessageCreateBuilder().apply(builder)

        val response = when(context) {
            is SlashCommandContext<*> -> {
                val interaction = when(context.acked) {
                    false -> context.ack(false)
                    else -> context.interactionResponse!!
                }

                kord.rest.interaction.createFollowupMessage(
                    interaction.applicationId,
                    interaction.token,
                    messageBuilder.toSlashRequest()
                )
            }

            is MessageCommandContext<*> -> {
                val messageId = message?.id

                kord.rest.channel.createMessage(
                    channel.id,
                    when(messageId) {
                        null -> messageBuilder.toMessageRequest()
                        else -> messageBuilder.toMessageRequest(messageId)
                    }
                )
            }
            else -> error("Unknown context type provided")
        }

        val data = MessageData.from(response)
        return Message(data, kord)
    }
}
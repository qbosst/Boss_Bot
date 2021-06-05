package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.MessageCommandContext
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.entity.Guild

open class HybridCommandContext<T: Arguments>(
    private val context: CommandContext

): CommandContext(context.command, context.eventObj, context.commandName, context.argsList) {

    public open val kord: Kord get() = eventObj.kord

    /** Message channel this command happened in, if any. **/
    public open val channel: MessageChannelBehavior get() = when(context) {
        is SlashCommandContext<*> -> context.channel
        is MessageCommandContext<*> -> context.channel

        else -> error("Unknown context type provided")
    }

    /** Guild this command happened in, if any. **/
    public open val guild: Guild? get() = when(context) {
        is SlashCommandContext<*> -> context.guild
        is MessageCommandContext<*> -> context.guild

        else -> error("Unknown context type provided")
    }

    /** Guild member responsible for executing this command, if any. **/
    public open val member: MemberBehavior? get() = when(context) {
        is SlashCommandContext<*> -> context.member
        is MessageCommandContext<*> -> context.member

        else -> error("Unknown context type provided")
    }

    /** User responsible for executing this command, if any (if `null`, it's a webhook). **/
    public open val user: UserBehavior? get() = when(context) {
        is SlashCommandContext<*> -> context.user
        is MessageCommandContext<*> -> context.user

        else -> error("Unknown context type provided")
    }

    /** Message object containing this command invocation. **/
    public open val message: MessageBehavior? get() = when(context) {
        is SlashCommandContext<*> -> null
        is MessageCommandContext<*> -> context.message

        else -> error("Unknown context type provided")
    }

    @Suppress("UNCHECKED_CAST")
    public val arguments: T get() = when(context) {
        is SlashCommandContext<*> -> context.arguments as T
        is MessageCommandContext<*> -> context.arguments as T

        else -> error("Unknown context type provided")
    }


    /**
     * Send an acknowledgement manually, assuming you have `autoAck` set to `NONE`.
     *
     * Note that what you supply for `ephemeral` will decide how the rest of your interactions - both responses and
     * follow-ups. They must match in ephemeral state.
     *
     * This function will throw an exception if an acknowledgement or response has already been sent.
     *
     * @param ephemeral Whether this should be an ephemeral acknowledgement or not.
     */
    public suspend fun ack(ephemeral: Boolean): InteractionResponseBehavior =
        (context as SlashCommandContext<*>).ack(ephemeral)

    public suspend fun publicFollowUp(
        builder: PublicHybridMessageCreateBuilder.() -> Unit
    ) {
        val messageBuilder = PublicHybridMessageCreateBuilder().apply(builder)

        when(context) {
            is SlashCommandContext<*> -> {
                val interaction = (ack(false) as PublicInteractionResponseBehavior)

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
        }
    }

    override suspend fun getChannel(): ChannelBehavior? = context.getChannel()

    override suspend fun getGuild(): GuildBehavior? = context.getGuild()

    override suspend fun getMember(): MemberBehavior? = context.getMember()

    override suspend fun getMessage(): MessageBehavior? = context.getMessage()

    override suspend fun getUser(): UserBehavior? = context.getUser()

    override suspend fun populate() {}
}
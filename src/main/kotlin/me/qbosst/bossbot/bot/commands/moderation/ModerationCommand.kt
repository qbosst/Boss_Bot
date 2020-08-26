package me.qbosst.fbiagent.bot.commands.moderation

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.exception.MissingArgumentException
import me.qbosst.bossbot.entities.database.GuildPunishment
import me.qbosst.bossbot.entities.database.GuildSettingsData
import me.qbosst.bossbot.util.getMemberByString
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.secondsToString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import java.time.OffsetDateTime
import java.util.concurrent.ScheduledFuture

abstract class ModerationCommand(
    name: String,
    description: String = "none",
    usages: List<String> = listOf(),
    examples: List<String> = listOf(),
    aliases: List<String> = listOf(),
    userPermissions: List<Permission> = listOf(),
    botPermissions: List<Permission> = listOf()

): Command(name, description, usages, examples, aliases, true, userPermissions, botPermissions)
{

    protected val timers = mutableMapOf<String, ScheduledFuture<*>>()

    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        if(args.isNotEmpty())
        {
            val target = event.guild.getMemberByString(args[0]) ?: kotlin.run()
            {
                event.channel.sendMessage("I could not find anyone by the id, tag or name of `${args[0].makeSafe()}`").queue()
                return
            }

            when
            {
                // Checks whether author can punish the target user
                !event.guild.selfMember.canInteract(target) ->
                    event.channel.sendMessage("I cannot punish this user!").queue()
                !event.member!!.canInteract(target) ->
                    event.channel.sendMessage("You cannot punish this user!").queue()
                target.hasPermission(Permission.BAN_MEMBERS, Permission.KICK_MEMBERS) ->
                    event.channel.sendMessage("I cannot punish a user with ban and kick permissions!").queue()
                else ->
                {
                    try
                    {
                        val punishment = getPunishment(
                                target = target,
                                issuer = event.member!!,
                                args = args.drop(1)
                        )

                        punish(
                                target = target,
                                issuer = event.member!!,
                                channel = event.textChannel,
                                message = event.message,
                                reason = punishment.reason,
                                duration = punishment.duration,
                                type = punishment.type
                        )
                    }
                    catch (e: Exception)
                    {
                        if(e.message != null)
                        {
                            event.channel.sendMessage(e.message!!).queue()
                        }
                        else
                        {
                            event.channel.sendMessage("An error has occurred while trying to punish...").queue()
                        }
                    }
                }
            }
        }
        else
        {
            event.channel.sendMessage("Please mention the user you would like to punish!").queue()
        }
    }

    protected abstract fun getRestAction(target: Member): RestAction<*>?

    protected abstract fun getPunishment(target: Member, issuer: Member, args: List<String>): GuildPunishment

    fun punish(target: Member, issuer: Member, channel: TextChannel? = null, message: Message? = null,
        reason: String? = null, duration: Long = 0L, type: GuildPunishment.Type)
    {
        if(reason.isNullOrEmpty() && GuildSettingsData.get(target.guild).requireReasonForPunish)
        {
            throw MissingArgumentException("You must provide a reason!")
        }
        else
        {
            val action = when(val restAction = getRestAction(target))
            {
                null ->
                {
                    message?.delete()?.queue()
                    log(target, issuer, reason, duration, type)
                    onSuccessfulPunish(target, issuer, channel, message, reason, duration, type, Unit)
                    return
                }
                is AuditableRestAction -> restAction.reason(reason)
                else -> restAction
            }

            action.queue(
                    {
                        message?.delete()?.queue()
                        channel?.sendMessage("${target.user.asTag} has been ${type.pastTenseName} by ${issuer.user.asTag}" + if(reason != null) " for `$reason`" else "")?.queue()

                        val tc = GuildSettingsData.get(target.guild).getModerationLogsChannel(target.guild)
                        if(tc != null && target.guild.selfMember.hasPermission(tc, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS))
                            tc.sendMessage(log(target, issuer, reason, duration, type).build()).queue()

                        target.user.openPrivateChannel().flatMap()
                        { privateChannel ->
                            val sb = StringBuilder("You have been issued a `${type.displayName}` by `${issuer.user.asTag}")
                            if(reason != null)
                                sb.append("\nReason: `$reason`")
                            if(duration > 0)
                                sb.append("\nThis punishment will last ${secondsToString(duration)}")
                            privateChannel.sendMessage(sb)
                        }.queue({}, {})

                        onSuccessfulPunish(target, issuer, channel, message, reason, duration, type, it)
                    },
                    {
                        onFailedPunish(target, issuer, channel, message, reason, duration, type, it)
                    }
            )
        }
    }

    protected open fun onSuccessfulPunish(
            target: Member, issuer: Member, channel: TextChannel? = null, message: Message? = null, reason: String? = null,
            duration: Long = 0L, type: GuildPunishment.Type, result: Any)
    {
        //TODO LOG
    }

    protected open fun onFailedPunish(
        target: Member, issuer: Member, channel: TextChannel? = null, message: Message? = null, reason: String? = null,
        duration: Long = 0L, type: GuildPunishment.Type, throwable: Throwable)
    {}

    private fun log(
        target: Member, issuer: Member, reason: String? = null, duration: Long = 0L, type: GuildPunishment.Type): EmbedBuilder
    {
        return EmbedBuilder()
            .setTitle("${type.displayName} by ${issuer.user.asTag}")
            .appendDescription(kotlin.run()
            {
                val sb = StringBuilder("Target: ${target.user.asTag}")
                if(reason != null)
                    sb.append("\nReason: $reason")
                if(duration > 0)
                    sb.append("\nDuration: $duration")
                sb
            })
            .setColor(type.colourRaw)
            .setTimestamp(OffsetDateTime.now())
    }

}
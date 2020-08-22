package qbosst.bossbot.bot.commands.moderation

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.AuditLogKey
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.bot.noMentionedUser
import qbosst.bossbot.bot.userNotFound
import qbosst.bossbot.database.data.GuildPunishment
import qbosst.bossbot.database.tables.GuildPunishmentDataTable
import qbosst.bossbot.exception.FailedCheckException
import qbosst.bossbot.util.getMemberByString
import qbosst.bossbot.util.makeSafe
import qbosst.bossbot.util.secondsToString

abstract class ModerationCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf("<reason>"),
        examples: List<String> = listOf("spam", "toxicity"),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        val type: GuildPunishment.Type

): Command(name, description,
        usage.map { "@member $it" },
        examples.map { "@boss $it" },
        aliases, true, userPermissions,
        botPermissions.plus(Permission.MESSAGE_MANAGE)) {

    final override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        try
        {
            checks(event.guild)
        }
        catch (e: FailedCheckException)
        {
            if(!e.message.isNullOrEmpty())
            {
                event.channel.sendMessage(e.message).queue()
            }
            return
        }

        if(args.isNotEmpty())
        {
            val target = event.guild.getMemberByString(args[0])
            if(target != null)
            {
                if(!event.guild.selfMember.canInteract(target))
                {
                    event.channel.sendMessage("I cannot interact with this member!").queue()
                }
                else if(!event.member!!.canInteract(target))
                {
                    event.channel.sendMessage("You cannot interact with this member!").queue()
                }
                else if(target.hasPermission(Permission.BAN_MEMBERS, Permission.KICK_MEMBERS))
                {
                    event.channel.sendMessage("I cannot punish a moderator!").queue()
                }
                else
                {
                    val p = getPunishment(event, target, args.drop(1))
                    if(p != null)
                        punish(event.guild, event.textChannel, event.message, p.first, p.second)
                } }
            else
            {
                event.channel.sendMessage(userNotFound(args[0])).queue()
            }
        }
        else
        {
            event.channel.sendMessage(noMentionedUser()).queue()
        }
    }

    private fun punish(guild: Guild, channel: TextChannel?, message: Message?, action: RestAction<*>, punishment: GuildPunishment)
    {
        if((punishment.reason?.length ?: 0)> GuildPunishmentDataTable.max_reason_length)
        {
            channel?.sendMessage("Reasons cannot be more than ${GuildPunishmentDataTable.max_reason_length} characters long!")?.queue()
        }
        else
        {
            val restAction = if(action is AuditableRestAction)
            {
                val reason = "${punishment.issuer_id}\n${punishment.reason}"
                action.reason(if(reason.length > 512) reason.substring(0, 512) else reason)
            } else action

            restAction.queue(
                    {
                        //TODO add option whether to delete message
                        if(true)
                        {
                            message?.delete()?.queue()
                        }
                        onSuccessfulPunish(guild, channel, message, punishment)

                        val sb = StringBuilder("You have been ${punishment.type.pastTenseName}")
                        if(punishment.reason != null)
                        {
                            sb.append(" for `${punishment.reason}`")
                        }
                        if(punishment.duration > 0)
                        {
                            sb.append("\nThis punishment will last for ${secondsToString(punishment.duration)}")
                        }
                        sb.append("\nThis punishment has been issued by ${BossBot.shards.getUserById(punishment.target_id)?.asTag ?: "User `${punishment.target_id}`"}")
                        punishment.getTarget(guild)?.user?.openPrivateChannel()?.flatMap { it.sendMessage(sb) }?.queue({}, {})
                    },
                    { throwable ->
                        onFailedPunish(guild, channel, message, punishment, throwable)
                    })
        }
    }

    fun punish(target: Member, channel: TextChannel?, message: Message?, punishment: GuildPunishment)
    {
        punish(target.guild, channel, message, getRestAction(target, punishment), punishment)
    }

    protected open fun onSuccessfulPunish(guild: Guild, channel: MessageChannel?, message: Message?, punishment: GuildPunishment) {
        punishment.log(guild)
        channel?.sendMessage("${BossBot.shards.getUserById(punishment.target_id)?.asTag ?: "User `${punishment.target_id}`"} has been ${punishment.type.pastTenseName} by ${BossBot.shards.getUserById(punishment.issuer_id)?.asTag ?: "User `${punishment.issuer_id}`"}")?.queue()
    }

    protected open fun onFailedPunish(guild: Guild, channel: MessageChannel?, message: Message?, punishment: GuildPunishment, throwable: Throwable)
    {
        channel?.sendMessage("An error has occurred")?.queue()
    }

    protected abstract fun getPunishment(event: MessageReceivedEvent, target: Member, args: List<String>): Pair<RestAction<*>, GuildPunishment>?

    @Throws(FailedCheckException::class)
    open fun checks(guild: Guild): Boolean { return true }

    @Throws(FailedCheckException::class)
    abstract fun getRestAction(target: Member, punishment: GuildPunishment): RestAction<*>
}
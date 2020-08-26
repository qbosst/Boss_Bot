package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.exception.MissingArgumentException
import me.qbosst.bossbot.entities.database.GuildPunishment
import me.qbosst.bossbot.entities.database.GuildSettingsData
import me.qbosst.bossbot.util.getSeconds
import me.qbosst.bossbot.util.randomMove
import me.qbosst.bossbot.util.removeRole
import me.qbosst.fbiagent.bot.commands.moderation.ModerationCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.RestAction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

object MuteCommand: ModerationCommand(
        "mute"
), EventListener {

    override fun getPunishment(target: Member, issuer: Member, args: List<String>): GuildPunishment {
        if(args.isNotEmpty())
        {
            val seconds = getSeconds(args[0])
            if(seconds > 60)
            {
                return GuildPunishment(
                        targetId = target.idLong,
                        issuerId = issuer.idLong,
                        reason = if(args.size > 1) args.drop(1).joinToString(" ") else null,
                        duration = seconds,
                        date = Instant.now(),
                        type = GuildPunishment.Type.MUTE
                )
            }
            else
            {
                throw IllegalArgumentException("Durations must be longer than 60 seconds!")
            }
        }
        else
        {
            throw MissingArgumentException("Please provide the duration")
        }
    }

    override fun getRestAction(target: Member): RestAction<*>?
    {
        return target.guild.addRoleToMember(target, getMuteRole(target.guild))
    }

    override fun onSuccessfulPunish(
        target: Member, issuer: Member, channel: TextChannel?, message: Message?, reason: String?, duration: Long,
        type: GuildPunishment.Type, result: Any
    ) {

        val key = "$fullName|${target.guild.idLong}|${target.idLong}"
        timers.put(key, target.removeRole(getMuteRole(target.guild)).submitAfter(duration, TimeUnit.SECONDS, BossBot.threadpool))?.cancel(true)

        transaction()
        {
            val date = OffsetDateTime.now()
            //TODO LOG
        }
    }

    private fun getMuteRole(guild: Guild): Role
    {
        return GuildSettingsData.get(guild).getMuteRole(guild) ?: throw IllegalStateException("This guild does not have a mute role setup!")
    }

    override fun onEvent(event: GenericEvent) {
        when(event)
        {
            is ReadyEvent ->
            {
                transaction()
                {
                    //TODO
                }
            }
            is GuildMemberRoleAddEvent ->
            {
                if(event.roles.contains(GuildSettingsData.get(event.guild).getMuteRole(event.guild)))
                    event.member.randomMove(setOf(Permission.VOICE_SPEAK))?.queue()
            }
        }
    }
}
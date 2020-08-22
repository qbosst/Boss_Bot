package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.database.data.GuildPunishment
import me.qbosst.bossbot.database.data.GuildSettingsData
import me.qbosst.bossbot.exception.FailedCheckException
import me.qbosst.bossbot.util.Key
import me.qbosst.bossbot.util.addRole
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ScheduledFuture

object MuteCommand : ModerationCommand(
        "mute",
        type = GuildPunishment.Type.MUTE
){
    private val mutes = mapOf<Key, ScheduledFuture<*>>()

    override fun getPunishment(event: MessageReceivedEvent, target: Member, args: List<String>): Pair<RestAction<*>, GuildPunishment>? {
        TODO("Not yet implemented")
    }

    override fun getRestAction(target: Member, punishment: GuildPunishment): RestAction<*> {
        val role = GuildSettingsData.get(target.guild).getMuteRole(target.guild) ?: throw FailedCheckException("This guild does not have a mute role setup")
        return target.addRole(role)
    }

    override fun checks(guild: Guild): Boolean {
        return if(GuildSettingsData.get(guild).getMuteRole(guild) == null)
        {
            throw FailedCheckException("This guild does not have a mute role setup")
        } else true
    }

    override fun onSuccessfulPunish(guild: Guild, channel: MessageChannel?, message: Message?, punishment: GuildPunishment) {
        transaction {
            punishment.log(guild, this)
        }
    }
}
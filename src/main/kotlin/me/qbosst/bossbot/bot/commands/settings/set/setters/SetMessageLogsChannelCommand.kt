package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetTextChannelCommand
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.database.GuildSettingsData
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetMessageLogsChannelCommand: SetTextChannelCommand(
        "messagelogs",
        "Sets the channel that message logs go to",
        aliases = listOf("chatlogs", "messagelog", "chatlog"),
        displayName = "Message Logs"
) {
    override fun set(guild: Guild, obj: TextChannel?): TextChannel?
    {
        return guild.getTextChannelById(GuildSettingsData.update(guild, GuildSettingsDataTable.message_logs_channel_id, obj?.idLong ?: 0L)?: 0L)
    }

    override fun get(guild: Guild): TextChannel?
    {
        return GuildSettingsData.get(guild).getMessageLogsChannel(guild)
    }

}
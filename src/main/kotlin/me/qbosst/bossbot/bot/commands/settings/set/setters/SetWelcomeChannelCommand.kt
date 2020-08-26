package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetTextChannelCommand
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.database.GuildSettingsData
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetWelcomeChannelCommand: SetTextChannelCommand(
        "welcomechannel",
        displayName = "Welcome Channel"
) {

    override fun set(guild: Guild, obj: TextChannel?): TextChannel?
    {
        return guild.getTextChannelById(GuildSettingsData.update(guild, GuildSettingsDataTable.welcome_channel_id, obj?.idLong ?: 0L)?: 0L)
    }

    override fun get(guild: Guild): TextChannel?
    {
        return GuildSettingsData.get(guild).getWelcomeChannel(guild)
    }
}
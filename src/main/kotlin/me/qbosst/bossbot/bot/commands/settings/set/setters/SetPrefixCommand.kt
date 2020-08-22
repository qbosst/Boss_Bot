package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetStringCommand
import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.data.GuildSettingsData
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import net.dv8tion.jda.api.entities.Guild

object SetPrefixCommand: SetStringCommand(
        "prefix",
        "Sets the prefix that is used for bot commands",
        maxLength = GuildSettingsDataTable.max_prefix_length,
        displayName = "Prefix"
) {
    override fun set(guild: Guild, obj: String?): String?
    {
        return GuildSettingsData.update(guild, GuildSettingsDataTable.prefix, obj)
    }

    override fun get(guild: Guild): String?
    {
        return GuildSettingsData.get(guild).getPrefixOr(Config.Values.DEFAULT_PREFIX.toString())
    }
}
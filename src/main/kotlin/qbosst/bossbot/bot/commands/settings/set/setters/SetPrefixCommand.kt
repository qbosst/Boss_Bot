package qbosst.bossbot.bot.commands.settings.set.setters

import net.dv8tion.jda.api.entities.Guild
import qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetStringCommand
import qbosst.bossbot.config.Config
import qbosst.bossbot.database.data.GuildSettingsData
import qbosst.bossbot.database.tables.GuildSettingsDataTable

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
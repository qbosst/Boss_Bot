package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.commands.meta.setters.SetStringCommand
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.entities.Guild

object SetPrefixCommand: SetStringCommand(
        "prefix",
        displayName = "Prefix",
        maxLength = GuildSettingsTable.max_prefix_length
)
{
    override fun get(guild: Guild): String? = guild.getSettings().prefix

    override fun set(guild: Guild, newValue: String?): String? = GuildSettingsManager.update(guild, GuildSettingsTable.prefix, newValue)

    override fun getString(value: String?): String = "`${value ?: BotConfig.default_prefix}`"
}
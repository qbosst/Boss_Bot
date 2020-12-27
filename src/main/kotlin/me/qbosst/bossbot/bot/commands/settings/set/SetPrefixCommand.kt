package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.setters.guild.CommandGuildStringSetter
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.entities.Guild

object SetPrefixCommand: CommandGuildStringSetter(
        "prefix",
        displayName = "Prefix",
        maxLength = GuildSettingsTable.max_prefix_length
)
{
    override fun get(key: Guild): String? = key.getSettings().prefix

    override fun set(key: Guild, value: String?): String? =
            GuildSettingsManager.update(key, GuildSettingsTable.prefix, value)

    override fun getString(value: String?): String = "`${value ?: BossBot.config.default_prefix}`"
}
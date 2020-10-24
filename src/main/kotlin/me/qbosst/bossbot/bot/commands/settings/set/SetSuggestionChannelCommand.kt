package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.commands.meta.setters.SetTextChannelCommand
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetSuggestionChannelCommand: SetTextChannelCommand(
        "suggestion",
        displayName = "Suggestion Channel",
        aliases = listOf("suggestions")
)
{
    override fun get(guild: Guild): TextChannel? = guild.getSettings().getSuggestionChannel(guild)

    override fun set(guild: Guild, newValue: TextChannel?): TextChannel? = guild.getTextChannelById(GuildSettingsManager.update(guild, GuildSettingsTable.suggestion_channel_id, newValue?.idLong ?: 0L) ?: 0L)
}
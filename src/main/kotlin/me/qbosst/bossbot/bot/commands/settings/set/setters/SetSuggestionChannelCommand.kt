package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetTextChannelCommand
import me.qbosst.bossbot.database.data.GuildSettingsData
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetSuggestionChannelCommand: SetTextChannelCommand(
        "suggestion",
        "Sets the channel that suggestions go to",
        textChannelPermissions = setOf(Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ, Permission.MESSAGE_ADD_REACTION),
        displayName = "Suggestion"
) {
    override fun set(guild: Guild, obj: TextChannel?): TextChannel?
    {
        return guild.getTextChannelById(GuildSettingsData.update(guild, GuildSettingsDataTable.suggestion_channel_id, obj?.idLong ?: 0L)?: 0L)
    }

    override fun get(guild: Guild): TextChannel?
    {
        return GuildSettingsData.get(guild).getSuggestionChannel(guild)
    }

}
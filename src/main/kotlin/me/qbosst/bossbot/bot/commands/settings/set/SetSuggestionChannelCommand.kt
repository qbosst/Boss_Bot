package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.commands.meta.setters.guild.CommandTextChannelSetter
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetSuggestionChannelCommand: CommandTextChannelSetter(
        "suggestions",
        displayName = "Suggestions",
        textChannelPermissions = listOf(Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ADD_REACTION)
)
{
    override fun set(key: Guild, value: TextChannel?): TextChannel? =
            GuildSettingsManager.update(key, GuildSettingsTable.suggestion_channel_id, value?.idLong ?: 0)
                    ?.let { id -> key.getTextChannelById(id) }

    override fun get(key: Guild): TextChannel? = key.getSettings().getSuggestionChannel(key)
}
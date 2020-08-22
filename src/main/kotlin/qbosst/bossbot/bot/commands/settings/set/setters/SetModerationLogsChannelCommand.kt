package qbosst.bossbot.bot.commands.settings.set.setters

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetTextChannelCommand
import qbosst.bossbot.database.data.GuildSettingsData
import qbosst.bossbot.database.tables.GuildSettingsDataTable

object SetModerationLogsChannelCommand: SetTextChannelCommand(
        "moderationlogs",
        displayName = "Moderation Logs"
) {

    override fun set(guild: Guild, obj: TextChannel?): TextChannel?
    {
        return guild.getTextChannelById(GuildSettingsData.update(guild, GuildSettingsDataTable.moderation_logs_channel_id, obj?.idLong ?: 0L)?: 0L)
    }

    override fun get(guild: Guild): TextChannel?
    {
        return GuildSettingsData.get(guild).getModerationLogsChannel(guild)
    }
}
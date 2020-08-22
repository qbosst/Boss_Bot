package qbosst.bossbot.bot.commands.settings.set.setters

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetTextChannelCommand
import qbosst.bossbot.database.data.GuildSettingsData
import qbosst.bossbot.database.tables.GuildSettingsDataTable

object SetVoiceLogsChannelCommand: SetTextChannelCommand(
        "voicelogs",
        "Sets the channel that voice logs get sent to",
        displayName = "Voice Logs"
) {

    override fun set(guild: Guild, obj: TextChannel?): TextChannel?
    {
        return guild.getTextChannelById(GuildSettingsData.update(guild, GuildSettingsDataTable.voice_logs_channel_id, obj?.idLong ?: 0L)?: 0L)
    }

    override fun get(guild: Guild): TextChannel?
    {
        return GuildSettingsData.get(guild).getVoiceLogsChannel(guild)
    }
}
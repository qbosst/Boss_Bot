package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.commands.meta.setters.guild.CommandTextChannelSetter
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

object SetVoiceLogsChannelCommand: CommandTextChannelSetter(
        "voicelogs",
        displayName = "Voice Logs",
        textChannelPermissions = listOf(Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS),
        aliases = listOf("voicelog", "vclogs", "vclog")
)
{
    override fun set(key: Guild, value: TextChannel?): TextChannel? =
            GuildSettingsManager.update(key, GuildSettingsTable.voiceLogsChannelId, value?.idLong ?: 0)
                    ?.let { id -> key.getTextChannelById(id) }

    override fun get(key: Guild): TextChannel? = key.getSettings().getVoiceLogsChannel(key)
}
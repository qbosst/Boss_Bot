package me.qbosst.bossbot.commands.settings.guild

import me.qbosst.bossbot.database.manager.GuildSettingsManager
import me.qbosst.bossbot.database.manager.settings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.entities.Guild

class SetPrefixCommand: CommandGuildStringSetter() {

    override val label: String = "prefix"
    override val maxLength: Int = 8

    override fun set(key: Guild, value: String?) =
        GuildSettingsManager.update(key.idLong, GuildSettingsTable.prefix, value)

    override fun get(key: Guild): String? =
        key.settings.prefix
}
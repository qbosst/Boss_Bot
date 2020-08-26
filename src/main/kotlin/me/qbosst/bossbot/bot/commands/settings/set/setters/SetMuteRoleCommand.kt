package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetRoleCommand
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.database.GuildSettingsData
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role

object SetMuteRoleCommand : SetRoleCommand(
        "mute",
        "Sets the role that is given to a user when they are muted",
        isInteractive = true,
        displayName = "Mute Role"
) {
    override fun set(guild: Guild, obj: Role?): Role?
    {
        return guild.getRoleById(GuildSettingsData.update(guild, GuildSettingsDataTable.mute_role_id, obj?.idLong ?: 0L) ?: 0L)
    }

    override fun get(guild: Guild): Role?
    {
        return GuildSettingsData.get(guild).getMuteRole(guild)
    }

}
package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.config.Config
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

abstract class DeveloperCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        guildOnly: Boolean = false,
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf()
): Command(
        name, description, usage, examples, aliases, guildOnly, userPermissions, botPermissions
)
{
    final override fun hasPermission(guild: Guild?, user: User): Boolean
    {
        // Checks if the user that invoked the command is a developer
        return user.idLong == Config.Values.DEVELOPER_ID.getLong()
    }
}
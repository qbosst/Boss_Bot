package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.config.Config

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
        return user.idLong == Config.Values.DEVELOPER_ID.getLong()
    }
}
package me.qbosst.bossbot.bot.commands.settings.set.autoroles

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.tables.GuildRoleDataTable
import me.qbosst.bossbot.entities.database.GuildRolesData
import me.qbosst.bossbot.util.loadObjects
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object AutoRoleCommand: Command(
        "autoroles",
        aliases = listOf("autorole"),
        userPermissions = listOf(Permission.MANAGE_ROLES)
)
{
    init
    {
        val commands = loadObjects(this::class.java.`package`.name, Command::class.java).filter { it != this }
        addCommands(commands)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val roles = GuildRolesData.get(event.guild).getRoles(event.guild, GuildRoleDataTable.Type.AUTO_ROLE)

        if(roles.isEmpty())
            event.channel.sendMessage("This guild has no auto roles!").queue()
        else
            event.channel.sendMessage(EmbedBuilder()
                    .setColor(event.guild.selfMember.colorRaw)
                    .setTitle("${event.guild.name} Auto Role(s)")
                    .setDescription(roles.joinToString(", ") { it.asMention })
                    .build()
            ).queue()
    }
}
package me.qbosst.bossbot.bot.commands.settings.set.autoroles

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.tables.GuildRoleDataTable
import me.qbosst.bossbot.entities.database.GuildRolesData
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object AutoRoleAddCommand: Command(
        "add"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val mentioned = event.message.mentionedRoles.toMutableList()
        val toAdd = mutableListOf<Role>()

        if(mentioned.isNotEmpty())
        {
            for(role in mentioned)
                if(event.guild.selfMember.canInteract(role))
                    toAdd.add(role)

            val added = GuildRolesData.add(toAdd, GuildRoleDataTable.Type.AUTO_ROLE)

            val cantInteract = mentioned.toMutableList()
            cantInteract.removeAll(toAdd)

            val alreadyAdded = toAdd.toMutableList()
            alreadyAdded.removeAll(added)

            event.channel.sendMessage(
                    getEmbed(added, cantInteract, alreadyAdded)
                            .setColor(event.guild.selfMember.colorRaw)
                            .build()).queue()
        }
        else
        {
            event.channel.sendMessage("Please mention the roles you would like to add to auto roles").queue()
        }
    }

    private fun getEmbed(added: List<Role>, cantInteract: List<Role>, alreadyAdded: List<Role>): EmbedBuilder
    {
        val embed = EmbedBuilder()
        if(added.isNotEmpty())
            embed.addField("Added", added.joinToString(", ") { it.asMention }, true)

        if(cantInteract.isNotEmpty())
            embed.addField("Couldn't add these roles because I can't interact with them", cantInteract.joinToString(", ") { it.asMention }, true)

        if(alreadyAdded.isNotEmpty())
            embed.addField("These roles are already added to auto-roles", alreadyAdded.joinToString(", ") { it.asMention }, true)

        return embed
    }
}
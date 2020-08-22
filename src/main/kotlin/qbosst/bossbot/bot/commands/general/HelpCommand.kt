package qbosst.bossbot.bot.commands.general

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.bot.listeners.Listener
import qbosst.bossbot.config.Config
import qbosst.bossbot.database.data.GuildSettingsData
import qbosst.bossbot.util.embed.FieldMenuEmbed
import qbosst.bossbot.util.loadClasses
import qbosst.bossbot.util.makeSafe

object HelpCommand: Command(
        "help",
        usage = listOf("[command]", "[page number]"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    private val allCommands = loadClasses("qbosst.bossbot.bot.commands", Command::class.java)

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            if(args[0].toIntOrNull() != null)
            {
                event.channel.sendMessage(getHelpEmbed(event, args[0].toInt()).build()).queue()
            }

            else if(Listener.getCommand(args[0]) != null)
            {
                var command: Command = Listener.getCommand(args[0])!!
                var index = 1
                while (index < args.size)
                {
                    val newCommand = command.getCommand(args[index])

                    if(newCommand != null)
                    {
                        command = newCommand
                        index++
                    } else break
                }

                if(command.hasPermission(if(event.isFromGuild) event.guild else null, event.author) && if(event.isFromGuild) event.member!!.hasPermission(command.fullUserPermissions) else true)
                {
                    event.channel.sendMessage(command.getHelp(event)).queue()
                }
            }
            else
            {
                event.channel.sendMessage("Could not find command `${args[0].makeSafe()}`").queue()
            }
        }
        else
        {
            event.channel.sendMessage(getHelpEmbed(event, 0).build()).queue()
        }
    }

    private fun getHelpEmbed(event: MessageReceivedEvent, page: Int): EmbedBuilder
    {
        var commands = allCommands.filter { it.hasPermission(if(event.isFromGuild) event.guild else null, event.author) }
        if(event.isFromGuild) commands = commands.filter { event.member!!.hasPermission(it.fullUserPermissions) }

        return FieldMenuEmbed(5, commands.map { it.getHelpField(GuildSettingsData.get(if (event.isFromGuild) event.guild else null).getPrefixOr(Config.Values.DEFAULT_PREFIX.toString()), event.isFromGuild) }).createPage(EmbedBuilder(), page)
    }

    private fun Command.getHelpField(prefix: String, isFromGuild: Boolean): MessageEmbed.Field
    {
        return MessageEmbed.Field(
                "${prefix}${this.fullName}" + if(guildOnly && !isFromGuild) " `Guild Only`" else "",
                "`${description}`" + if(aliases.isNotEmpty()) "\n**Aliases** : `${aliases.joinToString("`, `")}`" else "",
                false)
    }

    private fun Command.getHelp(event: MessageReceivedEvent): MessageEmbed
    {
        val embed = EmbedBuilder()
                .setTitle(fullName.split(" ".toRegex()).joinToString(" ") { it.capitalize() })
                .appendDescription("**$description**")

        val prefix = GuildSettingsData.get(if(event.isFromGuild) event.guild else null).getPrefixOr(Config.Values.DEFAULT_PREFIX.toString())

        if(usage.isNotEmpty()) embed.addField("Usages", "${prefix}${usage.joinToString("\n$prefix")}", true)
        if(examples.isNotEmpty()) embed.addField("Examples", "${prefix}${examples.joinToString("\n$prefix")}", true)
        val children = getCommands()
                .filter { it.hasPermission(if(event.isFromGuild) event.guild else null, event.author) }
                .filter { if(event.isFromGuild) event.member!!.hasPermission(fullUserPermissions) else true }

        if(children.isNotEmpty()) embed.addField("Sub Commands", "`${children.joinToString("`, `") { it.name }}`", true)

        if(aliases.isNotEmpty()) embed.setFooter("Aliases : ${aliases.joinToString(", ") { it.capitalize() }}")

        if(fullUserPermissions.isNotEmpty()) embed.addField("User Permissions", "`${fullUserPermissions.joinToString("`, `")}`", true)
        if(fullBotPermissions.isNotEmpty()) embed.addField("Bot Permissions", "`${fullBotPermissions.joinToString(", ")}`", true)

        return embed.build()
    }
}
package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.SetterCommand
import me.qbosst.bossbot.util.loadObjectOrClass
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object SetCommand: Command(
        "set",
        userPermissions = listOf(Permission.ADMINISTRATOR)
)
{
    init
    {
        val commands = loadObjectOrClass(BossBot::class.java.`package`.name, SetterCommand::class.java)
                .sortedBy { it.displayName }
        addCommands(commands)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val embed = EmbedBuilder()
                .setColor(event.guild.selfMember.colorRaw)
                .setTitle(event.guild.name + (if(event.guild.name.endsWith('s')) "'" else "'s") + " Settings")

        @Suppress("UNCHECKED_CAST")
        (getCommands() as Collection<SetterCommand<Any>>)
                .forEach {
                    embed.addField(it.displayName, it.getString(it.get(event.guild)), true)
                }
        if(embed.fields.size % 3 == 2)
            embed.addBlankField(true)

        event.channel.sendMessage(embed.build()).queue()
    }
}
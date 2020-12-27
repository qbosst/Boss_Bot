package me.qbosst.bossbot.bot.commands.settings.set

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.CommandSetter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object SetCommand: Command(
        "set",
        userPermissions = listOf(Permission.ADMINISTRATOR),
        children = listOf(SetPrefixCommand, SetMessageLogsChannelCommand, SetVoiceLogsChannelCommand,
                SetSuggestionChannelCommand)
)
{

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val builder = EmbedBuilder()
                .setColor(event.guild.selfMember.colorRaw)
                .setTitle("${event.guild.name} Settings")
                .apply {
                    children.mapNotNull { child -> child as? CommandSetter<Guild, Any> }
                            .forEach { child ->
                                addField(child.displayName, child.getString(child.get(event.guild)), true)
                            }

                    if(fields.size % 3 == 0)
                        addBlankField(true)
                }

        event.channel.sendMessage(builder.build()).queue()
    }
}
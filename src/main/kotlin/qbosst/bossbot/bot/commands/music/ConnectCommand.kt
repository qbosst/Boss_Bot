package qbosst.bossbot.bot.commands.music

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.TICK

object ConnectCommand: MusicCommand(
        "connect",
        connect = true,
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
) {

    override fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        val channel = event.member!!.voiceState!!.channel!!
        if(connect(channel))
        {
            run(event, args)
        }
        else
        {
            event.channel.sendMessage("I do not have the following permissions for voice channel `${channel.name}`; `${fullBotPermissions.joinToString("`, `")}`").queue()
        }
    }

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        event.message.addReaction(TICK).queue()
    }
}
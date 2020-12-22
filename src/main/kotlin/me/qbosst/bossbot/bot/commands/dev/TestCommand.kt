package me.qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object TestCommand: DeveloperCommand(
        "test"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage("```${args}``` ```${flags}```").queue()
    }
}
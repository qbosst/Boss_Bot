package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object TestCommand: Command(
        "test",
        developerOnly = true
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage("```${args}``` ```${flags}```").queue()
    }
}
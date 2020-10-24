package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.config.BotConfig
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ReloadCommand : DeveloperCommand(
        "reload",
        "Reloads the config file"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("Reloading config...").queue()
        {
            BotConfig.reload()
            it.editMessage("Reloaded").queue()
        }
    }
}
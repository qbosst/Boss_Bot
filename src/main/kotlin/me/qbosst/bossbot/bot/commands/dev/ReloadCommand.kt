package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.config.Config
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.system.exitProcess

object ReloadCommand : DeveloperCommand(
        "reload",
        "Reloads the config file"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("Reloading config...").queue {
            val invalid = Config.reload()
            if(invalid.isNotEmpty()) {
                it.editMessage("There has been an error while reloading the config.\nThe config is missing the values(s); `${invalid.joinToString("`, `") { v -> v.name.toLowerCase() }}`.\nThe bot will shut down").queue(
                        {
                            exitProcess(0)
                        },
                        { t ->
                            BossBot.LOG.error("Please fill in the required config values: ${invalid.joinToString(", ") { v ->  v.name.toLowerCase() }}: $t");
                            exitProcess(0)
                        }
                )
            }
            else
            {
                it.editMessage("Config has successfully reloaded.").queue()
            }
        }
    }
}
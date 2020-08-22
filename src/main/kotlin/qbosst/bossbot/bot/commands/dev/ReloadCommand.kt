package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.config.Config
import kotlin.system.exitProcess

object ReloadCommand : DeveloperCommand(
        "reload",
        "Reloads the config file. Providing incorrect values will turn off the bot. Some changes require a restart of the bot instance (like discord token, database parameters)"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("Reloading config...").queue {
            val invalid = Config.reload(false)
            if(invalid.isNotEmpty()) {
                it.editMessage("There has been an error while reloading the config.\nThe config is missing the values(s); `${invalid.joinToString("`, `") { v -> v.valueName }}`.\nThe bot will shut down").queue(
                        {
                            exitProcess(0)
                        },
                        { t ->
                            BossBot.LOG.error("Please fill in the required config values: ${invalid.joinToString(", ") { v ->  v.valueName }}: $t");
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
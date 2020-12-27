package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.Constants
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.BotEventWaiter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object ShutdownCommand: Command(
        "shutdown",
        description = "Shuts down the bot",
        usages = listOf(""),
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION),
        guildOnly = false,
        developerOnly = true
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage("Are you sure you want to shut me down?").queue()
        { message ->
            message.addReaction(Constants.TICK).queue()
            message.addReaction(Constants.CROSS).queue()

            BotEventWaiter.waitForEvent(MessageReactionAddEvent::class.java,
                { reactionEvent ->
                    reactionEvent.messageIdLong == message.idLong && reactionEvent.userIdLong == event.author.idLong
                },
                { reactionEvent ->
                    message.clearReactions().queue()
                    when(reactionEvent.reactionEmote.name)
                    {
                        Constants.TICK ->
                        {
                            message.editMessage("Ok bye... :(").queue()
                            exitProcess(0)
                        }
                        Constants.CROSS ->
                            message.editMessage("Thank you for not shutting me down :)").queue()
                        else ->
                            message.editMessage("I Was expecting a response of ${Constants.TICK} or ${Constants.CROSS}...").queue()
                    }
                }, 120, TimeUnit.SECONDS,
                {
                    message.clearReactions().queue()
                    message.editMessage("Timed out...").queue()
                }
            )
        }
    }

}
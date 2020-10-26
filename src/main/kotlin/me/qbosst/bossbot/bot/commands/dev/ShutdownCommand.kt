package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.CROSS
import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.bot.listeners.BotEventWaiter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object ShutdownCommand: DeveloperCommand(
        "shutdown",
        description = "Shuts down the bot",
        usage = listOf(""),
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION),
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("Are you sure you want to shut me down?").queue()
        { message ->
            message.addReaction(TICK).queue()
            message.addReaction(CROSS).queue()

            BotEventWaiter.waitForEvent(MessageReactionAddEvent::class.java,
                { reactionEvent ->
                    reactionEvent.messageIdLong == message.idLong && reactionEvent.userIdLong == event.author.idLong
                },
                { reactionEvent ->
                    message.clearReactions().queue()
                    when(reactionEvent.reactionEmote.name)
                    {
                        TICK ->
                        {
                            message.editMessage("Ok bye... :(").queue()
                            exitProcess(0)
                        }
                        CROSS ->
                            message.editMessage("Thank you for not shutting me down :)").queue()
                        else ->
                            message.editMessage("I Was expecting a response of $TICK or ${CROSS}...").queue()
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
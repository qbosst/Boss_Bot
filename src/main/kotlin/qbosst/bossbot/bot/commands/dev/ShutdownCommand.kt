package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import qbosst.bossbot.bot.CROSS
import qbosst.bossbot.bot.TICK
import qbosst.bossbot.bot.listeners.EventWaiter
import kotlin.system.exitProcess

object ShutdownCommand: DeveloperCommand(
        "shutdown",
        "Shuts down the bot"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("Are you sure you want to shut me down?").queue()
        { message ->
            message.addReaction(TICK).queue()
            message.addReaction(CROSS).queue()

            EventWaiter.waitForEvent(MessageReactionAddEvent::class.java,
                    { reactionEvent ->
                        reactionEvent.messageIdLong == message.idLong && reactionEvent.userIdLong == event.author.idLong
                    },
                    { reactionEvent ->
                        message.clearReactions().queue()
                        when(reactionEvent.reactionEmote.name)
                        {
                            TICK ->
                            {
                                message.editMessage("ok bye").queue()
                                exitProcess(0)
                            }
                            CROSS ->
                            {
                                message.editMessage("ok").queue()
                            }
                            else ->
                            {
                                message.editMessage("Was expecting a response of $TICK or ${CROSS}...").queue()
                            }

                        }

                    })
        }
    }

}
package qbosst.bossbot.bot.commands.misc

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command

object `8BallCommand` : Command(
        "8ball",
        guildOnly = false
){
    private val responses = listOf(
            "As I see it, yes.",
            "Ask again later.",
            "Better not tell you now.",
            "Cannot predict now.",
            "Concentrate and ask again.",
            "Don't count on it.",
            "It is certain.",
            "It is decidedly so.",
            "Most likely.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good",
            "Outlook good.",
            "Reply hazy, try again.",
            "Signs point to yes.",
            "Very doubtful",
            "Without a doubt.",
            "Yes.",
            "Yes - definitely.",
            "You may rely on it."
    )

    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        if(args.isNotEmpty())
        {
            event.channel.sendMessage(responses.random()).queue()
        }
        else
        {
            event.channel.sendMessage("Ask your question.").queue()
        }
    }
}
package me.qbosst.bossbot.bot.commands.misc

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object `8BallCommand` : Command(
        "8ball",
        "Asks 8ball something :flushed:",
        guildOnly = false,
        usages = listOf("<question>"),
        examples = listOf("Is ${BossBot::class.java.simpleName} cool?")
)
{
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

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage(if(args.isNotEmpty()) responses.random() else "Ask your question.").queue()
    }
}
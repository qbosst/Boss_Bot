package me.qbosst.bossbot.commands.misc

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.Permission

class EightBallCommand: Command() {
    override val label: String = "8ball"
    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_HISTORY)


    @CommandFunction
    fun execute(ctx: Context, @Greedy question: String) {
        ctx.message
            .reply(":8ball: `${responses.random()}`")
            .mentionRepliedUser(false)
            .queue()
    }

    companion object {
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
    }
}
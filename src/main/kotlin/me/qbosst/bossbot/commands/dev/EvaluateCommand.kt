package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.util.maxLength
import me.qbosst.jda.ext.util.withMultilineCode
import net.dv8tion.jda.api.entities.Message
import javax.script.ScriptEngineManager

class EvaluateCommand: Command()
{
    override val label: String = "evaluate"
    override val description: String = "Converts input into code and runs it"
    override val aliases: Collection<String> = listOf("eval")
    override val developerOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context, @Greedy code: String) {

        if(engine == null) {
            ctx.messageChannel.sendMessage("Engine is null.").queue()
        }
        else {
            ctx.jda.retrieveUserById(123123)
            engine.put("ctx", ctx)
            engine.put("event", ctx.event)
            engine.put("jda", ctx.jda)
            engine.put("channel", ctx.messageChannel)
            engine.put("author", ctx.author)
            engine.put("member", ctx.member)

            val result = kotlin.runCatching { engine.eval(imports+code) }
                .getOrElse { failure -> failure.localizedMessage }

            ctx.messageChannel.sendMessage(
                result.toString()
                    .withMultilineCode("console")
                    .maxLength(Message.MAX_CONTENT_LENGTH)
            ).queue()
        }
    }

    companion object {
        private val engine = ScriptEngineManager().getEngineByName("groovy")

        private val imports = listOf(
            "net.dv8tion.jda.api.EmbedBuilder",
            "net.dv8tion.jda.api.entities.Guild",
            "net.dv8tion.jda.api.entities.Member",
            "net.dv8tion.jda.api.entities.PrivateChannel",
            "net.dv8tion.jda.api.entities.MessageChannel",
            "net.dv8tion.jda.api.entities.TextChannel",
            "net.dv8tion.jda.api.entities.Role"
        ).joinToString(" ") { "import $it;" }
    }
}
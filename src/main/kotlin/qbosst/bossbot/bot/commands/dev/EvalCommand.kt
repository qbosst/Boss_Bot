package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.script.ScriptEngineManager

object EvalCommand : DeveloperCommand(
        "eval"
)
{
    private val engine = ScriptEngineManager().getEngineByName("Groovy")

    private val imports = listOf(
            "net.dv8tion.jda.api.EmbedBuilder",
            "net.dv8tion.jda.api.entities.Guild",
            "net.dv8tion.jda.api.entities.Member",
            "net.dv8tion.jda.api.entities.PrivateChannel",
            "net.dv8tion.jda.api.entities.MessageChannel",
            "net.dv8tion.jda.api.entities.TextChannel",
            "net.dv8tion.jda.api.entities.Role"
    )

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(engine == null)
        {
            event.channel.sendMessage("Engine is null. Please check source code.").queue()
            return
        }

        if(args.isNotEmpty())
        {
            val code = args.joinToString(" ")
            engine.put("event", event)
            engine.put("jda", event.jda)
            engine.put("channel", event.channel)

            try
            {
                engine.eval(imports.joinToString(" ") { "import $it;" } + code)
            }
            catch (e: Exception)
            {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                var exception = "Caught Exception: ```$sw```"
                exception = if(exception.length > Message.MAX_CONTENT_LENGTH)
                {
                    val msg = "...\nCheck console for full details```"
                    exception.substring(0, Message.MAX_CONTENT_LENGTH - msg.length) + msg
                } else exception

                event.channel.sendMessage(exception).queue()
            }
        }
    }

}
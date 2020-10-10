package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.script.ScriptEngineManager

/**
 *  Command used to convert execute java code in real time from a string
 */
object EvalCommand : DeveloperCommand(
        "eval"
)
{
    // The engine that is used to translate the string into code
    private val engine = ScriptEngineManager().getEngineByName("Groovy")

    // The default imports that the engine can access
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
            event.channel.sendMessage("Engine is null.").queue()
        else if(args.isNotEmpty())
        {
            val code = args.joinToString(" ")
            // variables that can be accessed in the code
            engine.put("event", event)
            engine.put("jda", event.jda)
            engine.put("channel", event.channel)

            // Try to execute code, if exception occurs log it back to the channel.
            try
            {
                engine.eval(imports.joinToString(" ") { "import $it;" } + code)
            }
            catch (e: Exception)
            {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                event.channel.sendMessage("Caught Exception: ```$sw```".maxLength(Message.MAX_CONTENT_LENGTH)).queue()
            }
        }
        else
            event.channel.sendMessage("Please provide the code to run").queue()
    }

}
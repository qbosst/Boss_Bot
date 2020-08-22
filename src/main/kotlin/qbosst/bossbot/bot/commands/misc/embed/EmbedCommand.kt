package qbosst.bossbot.bot.commands.misc.embed

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONException
import org.json.JSONObject
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.entities.JSONEmbedBuilder
import java.nio.file.Files

object EmbedCommand: Command(
        "embed",
        "Creates an embed based on json",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    init
    {
        addCommands(EmbedTemplateCommand, EmbedGetCommand)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        if(args.isNotEmpty())
        {
            val json = try
            {
                args.joinToString(" ").toJson()
            } catch (e: JSONException)
            {
                event.channel.sendMessage("Caught Exception while trying to create json object: $e").queue()
                return
            }
            if(json.isEmpty)
            {
                event.channel.sendMessage("This json is empty!").queue()
            }
            else
            {
                process(json, event)
            }
        }
        else if(event.message.attachments.isNotEmpty())
        {
            val first = event.message.attachments.first { it.fileExtension == "json" }
            if(first == null)
            {
                event.channel.sendMessage("bruh").queue()
            }
            else
            {
                first.downloadToFile().thenAccept()
                {
                    val content = Files.readAllLines(it.toPath()).joinToString("")
                    val json = try
                    {
                        content.toJson()
                    } catch (e: JSONException)
                    {
                        event.channel.sendMessage("Caught Exception while trying to create json object: $e").queue()
                        return@thenAccept
                    }
                    if(json.isEmpty)
                    {
                        event.channel.sendMessage("This json is empty!").queue()
                    }
                    else
                    {
                        process(json, event)
                    }
                }
            }
        }
        else
        {
            event.channel.sendMessage("nothing recieved").queue()
        }
    }

    private fun String.toJson(): JSONObject
    {
        return try
        {
            JSONObject(this)
        }
        catch (e: JSONException)
        {
            if(e.message != null)
            {
                val first = e.message!!.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.firstOrNull()
                if(first != null)
                    throw JSONException("${e.message} @ `${this.substring(if(first-10 > 0) first-10 else 0, if(first+10 < this.length) first+10 else this.length)}`")
                else throw e
            } else throw e
        }
    }

    private fun process(json: JSONObject, event: MessageReceivedEvent)
    {
        val embed = try
        {
            JSONEmbedBuilder(json)
        }
        catch (e: Exception)
        {
            if(e is IllegalStateException || e is IllegalArgumentException)
            {
                event.channel.sendMessage("Caught exception while trying to create embed: `${e.message}`").queue()
                return
            }
            else throw e
        }

        when
        {
            embed.isEmpty() -> event.channel.sendMessage("Cannot build an empty embed!").queue()

            embed.description.length > MessageEmbed.TEXT_MAX_LENGTH ->
                event.channel.sendMessage("Descriptions cannot be longer than ${MessageEmbed.TEXT_MAX_LENGTH}! Please limit your input!").queue()

            embed.length() > MessageEmbed.EMBED_MAX_LENGTH_BOT ->
                event.channel.sendMessage("Cannot build an embed with more than ${MessageEmbed.EMBED_MAX_LENGTH_BOT} characters!").queue()

            else -> event.channel.sendMessage(embed.build()).queue()
        }
    }
}
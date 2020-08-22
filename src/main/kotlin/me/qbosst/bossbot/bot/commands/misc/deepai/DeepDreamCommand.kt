package me.qbosst.bossbot.bot.commands.misc.deepai

import me.qbosst.bossbot.util.getMemberByString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

object DeepDreamCommand: DeepaiCommand(
        "deepdream",
        url = "https://api.deepai.org/api/deepdream",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
) {
    override fun execute(event: MessageReceivedEvent, json: CompletableFuture<JSONObject>)
    {
        json.thenAccept {
            val newUrl: String = when
            {
                it.has("output_url") -> it.getString("output_url")
                else ->
                {
                    event.channel.sendMessage("An error has occurred...").queue()
                    return@thenAccept
                }
            }
            event.channel.sendMessage(EmbedBuilder().setImage(newUrl).build()).queue()
        }
    }

    override fun getParameters(event: MessageReceivedEvent, args: List<String>): List<NameValuePair>
    {
        val url = if(args.isNotEmpty())
        {
            val target = event.guild.getMemberByString(args[0])
            if(target != null) target.user.avatarUrl + "?size=256" else args[0]
        }
        else if(event.message.attachments.getOrNull(0)?.isImage == true)
        {
            event.message.attachments[0].url
        }
        else
        {
            event.channel.sendMessage("Please attach an image or a specify a url link!").queue()
            return listOf()
        }

        return listOf(BasicNameValuePair("image", url))
    }
}
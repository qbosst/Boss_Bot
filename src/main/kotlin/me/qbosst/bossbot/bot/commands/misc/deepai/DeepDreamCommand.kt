package me.qbosst.bossbot.bot.commands.misc.deepai

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONObject

object DeepDreamCommand: DeepAiCommand(
        "deepdream",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS),
        url = "https://api.deepai.org/api/deepdream"
)
{
    override fun execute(event: MessageReceivedEvent, json: JSONObject)
    {
        // Checks if json includes an output url, if so this is the url to the processed image.
        if(json.has("output_url"))
            event.channel.sendMessage(EmbedBuilder()
                    .setImage(json.getString("output_url"))
                    .build()).queue()
        else
            event.channel.sendMessage("Could not deep-dream image; I have not received the new image :(").queue()
    }

    override fun getParameters(event: MessageReceivedEvent, args: List<String>): Map<String, String>?
    {
        // Gets the url for the image to process, null if no url was given
        val url = event.message.attachments.firstOrNull { it.isImage}?.url ?:
                event.message.mentionedMembers.firstOrNull()?.user?.effectiveAvatarUrl?.plus("?size=256") ?:
                if(args.isNotEmpty()) args.joinToString(" ") else null

        return if(url != null)
            mapOf(Pair("image", url))
        else {
            event.channel.sendMessage("Please provide a valid url for the picture you want to process").queue()
            null
        }
    }
}
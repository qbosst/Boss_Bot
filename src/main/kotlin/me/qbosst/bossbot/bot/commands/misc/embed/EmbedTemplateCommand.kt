package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.misc.colour.nextColour
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.random.Random

object EmbedTemplateCommand : Command(
        "template",
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        event.channel.sendFile(JSONObject(createTemplate(event.author).decodeToString()).toString(4).toByteArray(), "template.json").queue()
    }

    private fun createTemplate(author: User): ByteArray
    {
        val jda = author.jda
        return EmbedBuilder()
                .setTitle("This is the title!", "https://www.youtube.co.uk")
                .setColor(Random.nextColour(false))
                .setTimestamp(OffsetDateTime.now())
                .setDescription("This is the description!")
                .setFooter("Footer Text!", jda.selfUser.avatarUrl)
                .setAuthor("Author: ${jda.selfUser.asTag}", jda.selfUser.avatarUrl, jda.selfUser.avatarUrl)
                .setThumbnail(jda.selfUser.defaultAvatarUrl)
                .setImage(author.avatarUrl)
                .addField(
                        "First",
                        "This is the value of the first field!",
                        true
                )
                .addBlankField(true)
                .addField(
                        "Third",
                        "This is the value of the third field! The one before is an empty field!",
                        true
                )
                .addField(
                        "Fourth",
                        "This is the value of the fourth field!",
                        false
                )
                .build().toData().toJson()
    }
}
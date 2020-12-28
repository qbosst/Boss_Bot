package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.extensions.toPrettyJson
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import kotlin.random.Random

object EmbedTemplateCommand: Command(
        "template",
        "Provides a JSON template/example for a message embed",
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val embed = createTemplate(event)
        event.channel
                .sendMessage(embed)
                .addFile(embed.toData().toPrettyJson(), "template.json")
                .queue()
    }

    private fun createTemplate(event: MessageReceivedEvent): MessageEmbed
    {
        return EmbedBuilder()
                .setTitle("Example Message Embed", getRandomURL(event))
                .setAuthor("Author: ${event.author.asTag}", event.author.effectiveAvatarUrl, event.author.effectiveAvatarUrl)
                .setImage(getRandomURL(event))
                .setThumbnail(getRandomURL(event))
                .setDescription("Write a description here...")
                .setColor(Random.nextColour())
                .setFooter("Author ID: ${event.author.idLong}")
                .setTimestamp(OffsetDateTime.now())
                .addField("First Field", "Put something here :skull:", true)
                .addField("Second Field", "Put something else here :rofl:", true)
                .build()
    }

    private fun getRandomURL(event: MessageReceivedEvent): String
    {
        return event.jda.users.filter { it.isBot }.random().effectiveAvatarUrl
    }
}
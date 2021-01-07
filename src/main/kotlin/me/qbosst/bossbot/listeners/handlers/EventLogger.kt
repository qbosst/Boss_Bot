package me.qbosst.bossbot.listeners.handlers

import dev.minn.jda.ktx.Embed
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.jda.ext.util.maxLength
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import java.time.OffsetDateTime

abstract class EventLogger(cacheSize: Int) {
    private val messageCache = MessageCache(cacheSize)

    fun logMessageReceivedEvent(event: MessageReceivedEvent) {
        if(!event.isFromGuild || getLogger(event) == null) return
        messageCache + event.message
    }

    fun logMessageUpdateEvent(event: MessageUpdateEvent) {
        val tc = (if(!event.isFromGuild || event.author.isBot) null else getLogger(event)) ?: return
        val message = event.message
        val old = messageCache + message

        val builder = EmbedBuilder()
            .setAuthor("Message Edited", null, event.author.effectiveAvatarUrl)
            .setTimestamp(message.timeEdited ?: OffsetDateTime.now())
            //.setColor(Constants.PASTEL_YELLOW)
            .addField("Channel", event.textChannel.asMention, true)
            .addField("Author", "${event.author.asMention} ${event.author.asTag}", true)
            .addField("Message", "[Jump to Message](${message.jumpUrl})", true)
            .addField("Message Content Before", old?.content?.maxLength(MessageEmbed.VALUE_MAX_LENGTH), true)
            .addField("Message Content After", message.contentRaw.maxLength(MessageEmbed.VALUE_MAX_LENGTH), true)
            .setFooter("Message Id: ${event.messageId} | User Id: ${event.author.id}")

        tc.sendMessage(builder.build()).queue()
    }

    fun logMessageDeleteEvent(event: MessageDeleteEvent) {

        val tc = (if(!event.isFromGuild) null else getLogger(event)) ?: return
        val old = messageCache.pull(event.guild, event.messageIdLong)

        val author = old?.run { event.jda.getUserById(user.idLong) }
        val files = old?.files ?: listOf()
        val builder = EmbedBuilder()
            .setAuthor("Message Deleted", null, author?.effectiveAvatarUrl)
            .setTimestamp(OffsetDateTime.now())
            //.setColor(Constants.PASTEL_RED)
            .addField("Channel", event.textChannel.asMention, true)
            .apply {
                val value = when {
                    author != null -> "${author.asMention} ${author.asTag}"
                    old != null -> "<@${old.user.idLong}> ${old.user.username}#${old.user.discriminator}"
                    else -> "N/A"
                }

                addField("Author", value, true)

                if(files.isNotEmpty())
                {
                    addField("Attachments", files.size.toString(), true)
                }
            }
            .setFooter("Message ID: ${event.messageId} " + old?.user?.let { " | User ID: ${it.idLong}" })

        tc.sendMessage(builder.build())
                // add file attachments to message
            .apply { files.forEach { file -> addFile(file) } }
                // delete files, as they are no longer needed
            .queue { old?.deleteFiles(files) }
    }


    abstract fun getLogger(event: GenericEvent): TextChannel?
}
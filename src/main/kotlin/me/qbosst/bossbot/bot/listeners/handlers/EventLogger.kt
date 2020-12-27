package me.qbosst.bossbot.bot.listeners.handlers

import me.qbosst.bossbot.bot.Constants
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import java.time.OffsetDateTime

abstract class EventLogger(cacheSize: Int)
{
    private val messageCache = MessageCache(cacheSize)

    fun logMessageReceivedEvent(event: MessageReceivedEvent)
    {
        if(!event.isFromGuild || getLogger(event) == null) return
        messageCache.putMessage(event.message)
    }

    fun logMessageUpdateEvent(event: MessageUpdateEvent)
    {
        val tc = (if(!event.isFromGuild || event.author.isBot) null else getLogger(event)) ?: return
        val message = event.message
        val old = messageCache.putMessage(message)

        val builder = EmbedBuilder()
                .setAuthor("Message Edited", null, event.author.effectiveAvatarUrl)
                .setTimestamp(message.timeEdited ?: OffsetDateTime.now())
                .setColor(Constants.PASTEL_YELLOW)
                .addField("Channel", event.textChannel.asMention, true)
                .addField("Author", "${event.author.asMention} ${event.author.asTag}", true)
                .addField("Message", "[Jump to Message](${message.jumpUrl})", true)
                .addField("Message Content Before", old?.content?.maxLength(MessageEmbed.VALUE_MAX_LENGTH), true)
                .addField("Message Content After", message.contentRaw.maxLength(MessageEmbed.VALUE_MAX_LENGTH), true)
                .setFooter("Message ID: ${event.messageId} | User ID: ${event.author.id}")

        tc.sendMessage(builder.build()).queue()
    }

    fun logMessageDeleteEvent(event: MessageDeleteEvent)
    {
        val tc = (if(!event.isFromGuild) null else getLogger(event)) ?: return
        val old = messageCache.pullMessage(event.guild, event.messageIdLong)

        val author = old?.let { event.jda.getUserById(it.author.idLong) }
        val files = old?.files ?: listOf()
        val builder = EmbedBuilder()
                .setAuthor("Message Deleted", null, author?.effectiveAvatarUrl)
                .setTimestamp(OffsetDateTime.now())
                .setColor(Constants.PASTEL_RED)
                .addField("Channel", event.textChannel.asMention, true)
                .addField("Author", author?.let { "${it.asMention} ${it.asTag}" }
                        ?: old?.author?.let { "<@${it.idLong}> ${it.username}#${it.discriminator}" } ?: "N/A", true)
                .setFooter("Message ID: ${event.messageId} " + old?.author?.let { " | User ID: ${it.idLong}" })
                .apply {
                    if(files.isNotEmpty())
                        addField("Attachments", files.size.toString(), true)
                }
                .addField("Content", old?.content?.maxLength(MessageEmbed.VALUE_MAX_LENGTH) ?: "N/A", true)

        tc.sendMessage(builder.build())
                .apply { files.forEach { file -> addFile(file) } }
                .queue { old?.deleteFiles(files) }
    }

    fun logGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        val tc = getLogger(event) ?: return
        val builder = EmbedBuilder()
                .setAuthor(event.user.asTag, null, event.user.effectiveAvatarUrl)
                .setDescription("${event.user.asMention} has joined `${event.channelJoined.name}`")
                .setFooter("Channel ID: ${event.channelJoined.id} | User ID: ${event.user.id}")
                .setTimestamp(OffsetDateTime.now())
                .setThumbnail(event.user.effectiveAvatarUrl)
                .setColor(Constants.PASTEL_GREEN)

        tc.sendMessage(builder.build()).queue()
    }

    fun logGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        val tc = getLogger(event) ?: return
        val builder = EmbedBuilder()
                .setAuthor(event.user.asTag, null, event.user.effectiveAvatarUrl)
                .setDescription("${event.user.asMention} has left `${event.channelLeft.name}`")
                .setFooter("Channel ID: ${event.channelLeft.id} | User ID: ${event.user.id}")
                .setTimestamp(OffsetDateTime.now())
                .setThumbnail(event.user.effectiveAvatarUrl)
                .setColor(Constants.PASTEL_RED)

        tc.sendMessage(builder.build()).queue()
    }

    fun logGuildVoiceMoveEvent(event: GuildVoiceMoveEvent)
    {
        val tc = getLogger(event) ?: return
        val builder = EmbedBuilder()
                .setAuthor(event.user.asTag, null, event.user.effectiveAvatarUrl)
                .setDescription("${event.user.asMention} has switched channels `${event.channelLeft.name}` -> " +
                        "`${event.channelJoined.name}`")
                .setFooter("Channel Left ID: ${event.channelLeft.id} | Channel Joined ID: ${event.channelJoined.id} | " +
                        "User ID: ${event.user.id}")
                .setTimestamp(OffsetDateTime.now())
                .setThumbnail(event.user.effectiveAvatarUrl)
                .setColor(Constants.PASTEL_YELLOW)

        tc.sendMessage(builder.build()).queue()
    }

    private val GenericGuildVoiceEvent.user
        get() = member.user

    abstract fun getLogger(event: GenericEvent): TextChannel?
}
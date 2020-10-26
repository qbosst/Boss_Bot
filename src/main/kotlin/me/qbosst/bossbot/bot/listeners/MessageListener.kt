package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.PASTEL_RED
import me.qbosst.bossbot.bot.PASTEL_YELLOW
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.CommandManagerImpl
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.managers.MemberDataManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.bossbot.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.internal.utils.tuple.MutablePair
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

object MessageListener: EventListener, CommandManagerImpl()
{

    val allCommands: Collection<Command>
    init
    {
        allCommands = loadObjects("${BossBot::class.java.`package`.name}.commands", Command::class.java)
                .sortedBy { it.fullName }
        val commands = allCommands.filter { it.parent == null }
        BossBot.LOG.info("Registered ${commands.size} command(s): ${commands.joinToString(", ") { it.fullName }}")
        addCommands(commands)
    }

    // Caches
    private var textCache = FixedCache<Pair<Long, Long>, MutablePair<OffsetDateTime, Int>>(BotConfig.default_cache_size)
    private val messageCache = MessageCache(BotConfig.default_cache_size)

    private const val seconds_until_eligible = 60L
    private const val xp_to_give = 10 //TODO make adjustable

    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is MessageReceivedEvent ->
                onMessageReceivedEvent(event)
            is MessageUpdateEvent ->
                onMessageUpdateEvent(event)
            is MessageDeleteEvent ->
                onMessageDeleteEvent(event)
            is StatusChangeEvent ->
                onStatusChangeEvent(event)
        }
    }

    private fun onMessageReceivedEvent(event: MessageReceivedEvent)
    {
        if(event.isFromGuild)
        {
            // Checks if guild has a message logs channel, if so it will store the message
            if(event.guild.getSettings().getMessageLogsChannel(event.guild) != null)
                messageCache.putMessage(event.message)

            if(event.member != null && !event.author.isBot)
            {
                val member = event.member!!
                val key = Pair(event.guild.idLong, member.idLong)
                if(!textCache.contains(key))
                    textCache.put(key, MutablePair(event.message.timeCreated, 0))
                    { removedKey, value ->

                        // Checks if the record was removed without the cooldown being finished
                        if(value.left.plusSeconds(seconds_until_eligible).isAfter(OffsetDateTime.now()))
                        {
                            BossBot.LOG.warn("The cache size for ${this::textCache.name} needs to be increased!")
                            val new = FixedCache(textCache.size()+25, textCache)
                            new.put(removedKey, value)
                            textCache = new
                        }

                        // Update message counter for member removed
                        else if(value.right > 0)
                            MemberDataManager.update(removedKey.first, removedKey.second) { old ->
                                return@update old.clone(message_count = old.message_count + value.right)
                            }
                    }
                textCache.get(key)?.setRight((textCache.get(key)?.right ?: 0)+1)
            }
        }

        if(!event.author.isBot)
        {
            val content = event.message.contentRaw
            val prefix = event.getPrefix()

            if(content.startsWith(prefix) && content.length > prefix.length)
            {
                val args = content.substring(prefix.length).split(Regex("\\s+"))
                var command = getCommand(args[0])
                if(command != null)
                {
                    var index = 1
                    while (args.size > index)
                    {
                        val new = command?.getCommand(args[index]) ?: break
                        command = new
                        index++
                    }
                    command = command!!

                    // Check permissions
                    if(!command.hasPermission(event.getGuildOrNull(), event.author))
                        event.channel.sendMessage("You do not have permission for this command!").queue()

                    else if(event.isFromGuild && !(event.guild.selfMember.hasPermission(command.fullBotPermissions) || event.guild.selfMember.hasPermission(event.textChannel, command.fullBotPermissions)))
                        if(event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_WRITE))
                            event.channel.sendMessage("I need the following permissions to use this command; `${command.fullBotPermissions.joinToString("`, `")}`").queue()
                        else
                            event.author.openPrivateChannel().submit()
                                    .thenCompose { it.sendMessage("I need the following permissions to use this command; `${command.fullBotPermissions.joinToString("`, `")}`").submit() }

                    else if(event.isFromGuild && !(event.member!!.hasPermission(command.fullUserPermissions) || event.member!!.hasPermission(event.textChannel, command.fullUserPermissions)))
                        event.channel.sendMessage("You do not have permission for this command!").queue()

                    else if(command.guildOnly && !event.isFromGuild)
                        event.channel.sendMessage("This command is a guild-only command!").queue()

                    else
                        try
                        {
                            command.execute(event, args.drop(index))
                        }
                        catch (t: Throwable)
                        {
                            BossBot.LOG.error("An error has occurred while trying to execute command '${content}' by User ${event.author.id}" + if(event.isFromGuild) "on Guild ${event.guild.id}" else "", t)
                        }
                }
                else
                    onNonCommandEvent(event)
            }
            else
                onNonCommandEvent(event)
        }
        else
            onNonCommandEvent(event)
    }

    private fun onMessageUpdateEvent(event: MessageUpdateEvent)
    {
        if(event.isFromGuild)
        {
            val old = messageCache.putMessage(event.message)
            if(event.author.isBot)
                return

            val textChannel = event.guild.getSettings().getMessageLogsChannel(event.guild) ?: return

            val embed = EmbedBuilder()
                    .setAuthor("Message Edited", null, old?.getAuthor(event.jda.shardManager!!)?.effectiveAvatarUrl)
                    .setTimestamp(event.message.timeEdited ?: OffsetDateTime.now())
                    .setColor(PASTEL_YELLOW)
                    .addField("Channel", event.textChannel.asMention, true)
                    .addField("Author", "${event.author.asMention} ${event.author.asTag}", true)
                    .addField("Message", "[Jump to Message](${event.message.jumpUrl})", true)
                    .addField("Message Content Before", old?.contentRaw?.maxLength(MessageEmbed.VALUE_MAX_LENGTH) ?: "N/A", true)
                    .addField("Message Content After", event.message.contentRaw.maxLength(MessageEmbed.VALUE_MAX_LENGTH), true)
                    .setFooter("User ID: ${event.author.idLong} | Message ID: ${event.messageIdLong}")


            // Logs the message update event
            textChannel.sendMessage(embed.build()).queue()
        }
    }

    private fun onMessageDeleteEvent(event: MessageDeleteEvent)
    {
        if(event.isFromGuild)
        {
            // Logs the message delete event
            val old = messageCache.pullMessage(event.guild, event.messageIdLong)
            val textChannel = event.guild.getSettings().getMessageLogsChannel(event.guild) ?: return

            val author = old?.getAuthor(event.jda.shardManager!!)

            if(author?.isBot == true)
                return

            val embed = EmbedBuilder()
                    .setAuthor("Message Deleted", null, old?.getAuthor(event.jda.shardManager!!)?.effectiveAvatarUrl)
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(PASTEL_RED)
                    .addField("Channel", event.textChannel.asMention, true)
                    .addField("Author", if(old != null) if(author != null) "${author.asMention} ${author.asTag}" else "<@${old.authorIdLong}> ${old.username}#${old.discriminator}" else "N/A", true)
                    .setFooter((if(old != null) "User ID: ${old.authorIdLong} | " else "") + "Message ID: ${event.messageIdLong}")

            val attachments = old?.getAttachmentFiles() ?: listOf()
            if(attachments.isNotEmpty())
                embed.addField("Attachments", attachments.size.toString(), true)

            embed.addField("Content", old?.contentRaw?.maxLength(MessageEmbed.VALUE_MAX_LENGTH) ?: "N/A", false)

            val action = textChannel.sendMessage(embed.build())
            for(file in attachments)
                action.addFile(file)

            action.queue() { old?.deleteFiles() }
        }
    }

    private fun onNonCommandEvent(event: MessageReceivedEvent)
    {
        if(event.author.isBot)
            return

        if(event.message.contentRaw.matches(Message.MentionType.USER.pattern.toRegex()) && event.message.mentionedUsers.contains(event.jda.selfUser))
            event.channel.sendMessage("My prefix is: `${event.getPrefix()}`").queue()

        if(event.isFromGuild && event.member != null)
        {
            val key = Pair(event.guild.idLong, event.author.idLong)
            val record = textCache.get(key) ?: return
            if(record.left.plusSeconds(seconds_until_eligible).isBefore(event.message.timeCreated))
            {
                val counter = record.right
                record.setRight(0)
                record.setLeft(event.message.timeCreated)
                MemberDataManager.update(event.member!!) { old ->
                    return@update old.clone(
                            message_count = old.message_count + counter,
                            text_chat_time = old.text_chat_time + seconds_until_eligible,
                            experience = old.experience + xp_to_give
                    )
                }
            }
        }
    }

    private fun onStatusChangeEvent(event: StatusChangeEvent)
    {
        when(event.newStatus)
        {
            JDA.Status.SHUTTING_DOWN ->
                transaction {
                    for (record in textCache.keys())
                        MemberDataManager.update(record.first, record.second) { old ->
                            return@update old.clone(message_count = old.message_count + (textCache.get(record)?.right ?: 0))
                        }
                }
        }
    }

    fun getCachedMessageCount(guild: Guild, userId: Long): Int = textCache.get(Pair(guild.idLong, userId))?.right ?: 0

    fun getCachedMessageCount(member: Member): Int = getCachedMessageCount(member.guild, member.idLong)
}
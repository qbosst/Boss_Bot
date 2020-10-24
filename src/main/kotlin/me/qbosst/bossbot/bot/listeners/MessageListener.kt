package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.CommandManagerImpl
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.managers.MemberDataManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.MemberDataTable
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.bossbot.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.internal.utils.tuple.MutablePair
import java.awt.Color
import java.time.OffsetDateTime

object MessageListener: EventListener, CommandManagerImpl()
{

    val allCommands: Collection<Command>
    init
    {
        allCommands = loadObjects("${BossBot::class.java.`package`.name}.commands", Command::class.java)
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
        }
    }

    private fun onMessageReceivedEvent(event: MessageReceivedEvent)
    {
        if(event.author.isBot)
            return

        if(event.isFromGuild)
        {
            // Checks if guild has a message logs channel, if so it will store the message
            if(event.guild.getSettings().getMessageLogsChannel(event.guild) != null)
                messageCache.putMessage(event.message)

            if(event.member != null)
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
                        {
                            //TODO UPDATE
                        }
                    }
                textCache.get(key)?.setRight((textCache.get(key)?.right ?: 0)+1)
            }
        }

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

    private fun onMessageUpdateEvent(event: MessageUpdateEvent)
    {
        if(event.isFromGuild)
        {
            val old = messageCache.putMessage(event.message)
            if(event.author.isBot)
                return

            // Logs the message update event
            event.guild.getSettings().getMessageLogsChannel(event.guild)
                    ?.sendMessage(EmbedBuilder()
                            .setAuthor("Message Edited", event.message.jumpUrl, event.author.effectiveAvatarUrl)
                            .setThumbnail(event.author.avatarUrl)
                            .setDescription("**Author**: ${event.author.asMention}\n**Channel**:${event.textChannel.asMention}\n[Jump to Message](${event.message.jumpUrl})")
                            .addField("Message Content Before", old?.content?.maxLength(MessageEmbed.VALUE_MAX_LENGTH) ?: "N/A", false)
                            .addField("Message Content After", event.message.contentRaw.maxLength(MessageEmbed.VALUE_MAX_LENGTH), false)
                            .setFooter("User ID: ${event.author.idLong} | Message ID: ${event.message.idLong}")
                            .setThumbnail(event.author.effectiveAvatarUrl)
                            .setTimestamp(event.message.timeEdited)
                            .setColor(Color.YELLOW)
                            .build())
                    ?.queue()
        }
    }

    private fun onMessageDeleteEvent(event: MessageDeleteEvent)
    {
        if(event.isFromGuild)
        {
            // Logs the message delete event
            val old = messageCache.pullMessage(event.guild, event.messageIdLong)
            val textChannel = event.guild.getSettings().getMessageLogsChannel(event.guild) ?: return

            if(old != null)
            {
                val author = old.getAuthor(BossBot.SHARDS_MANAGER)
                if(author?.isBot == true)
                    return

                val embed = EmbedBuilder()
                        .setAuthor("Message Deleted", null, author?.effectiveAvatarUrl)
                        .addField("Author", author?.asMention ?: "${old.username}#${old.discriminator}", true)
                        .addField("Channel", event.textChannel.asMention, true)
                        .setFooter("User ID: ${old.authorIdLong} | Message ID: ${event.messageId}")
                        .setThumbnail(author?.effectiveAvatarUrl)
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(Color.RED)

                val attachments = old.getAttachmentFiles()
                if(attachments.isNotEmpty())
                    embed.addField("Attachments", attachments.size.toString(), true)
                embed.addField("Message Content", old.content.maxLength(MessageEmbed.VALUE_MAX_LENGTH), false)

                val message = textChannel.sendMessage(embed.build())

                // Attaches files to message
                for(file in attachments)
                    message.addFile(file)

                // Sends message and deletes files from secondary storage
                message.queue()
                {
                    old.deleteFiles()
                }
            }
            else
                textChannel.sendMessage(EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("A message has been deleted.")
                        .addField("Channel", event.textChannel.asMention, true)
                        .setTimestamp(OffsetDateTime.now())
                        .build()
                ).queue()
        }
    }

    private fun onNonCommandEvent(event: MessageReceivedEvent)
    {
        if(event.message.contentRaw.matches(Message.MentionType.USER.pattern.toRegex()) && event.message.mentionedUsers.contains(event.jda.selfUser))
            event.channel.sendMessage("My prefix is: `${event.getPrefix()}`").queue()

        if(event.isFromGuild && event.member != null)
        {
            val key = Pair(event.guild.idLong, event.author.idLong)
            val record = textCache.get(key)!!
            if(record.left.plusSeconds(seconds_until_eligible).isBefore(event.message.timeCreated))
            {
                val counter = record.right
                record.setRight(0)
                record.setLeft(event.message.timeCreated)

                MemberDataManager.update(event.member!!,
                        { insert ->
                            insert[MemberDataTable.message_count] = counter
                            insert[MemberDataTable.text_chat_time] = seconds_until_eligible
                            insert[MemberDataTable.experience] = xp_to_give
                        },
                        { row, update ->
                            update[MemberDataTable.message_count] = row[MemberDataTable.message_count] + counter
                            update[MemberDataTable.text_chat_time] = row[MemberDataTable.text_chat_time] + seconds_until_eligible
                            update[MemberDataTable.experience] = row[MemberDataTable.experience] + xp_to_give
                        })
            }
        }
    }

    fun getCachedMessageCount(guild: Guild, userId: Long): Int = textCache.get(Pair(guild.idLong, userId))?.right ?: 0

    fun getCachedMessageCount(member: Member): Int = getCachedMessageCount(member.guild, member.idLong)
}
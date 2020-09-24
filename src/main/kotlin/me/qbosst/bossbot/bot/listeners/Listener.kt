package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.ICommandManager
import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildUserDataTable
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.bossbot.entities.database.GuildSettingsData
import me.qbosst.bossbot.entities.database.GuildUserData
import me.qbosst.bossbot.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.math.roundToInt

object Listener : EventListener, ICommandManager
{

    private val commands = mutableMapOf<String, Command>()
    private val commandAlias = mutableMapOf<String, Command>()

    private val listeners = loadClasses("me.qbosst.bossbot.bot.commands", EventListener::class.java)

    private const val seconds_until_eligible = 60L
    private const val xp_to_give = 10 //TODO make adjustable

    private var textTimerCache = FixedCache<Key, OffsetDateTime>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
    private var textCounterCache = FixedCache<Key, Int>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
    private val messageCache = MessageCache(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())

    private val voiceCache = mutableMapOf<Key, VoiceMemberStatus>()

    private val rateLimiter = FixedCache<Key, RateLimitStatus>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())

    init
    {
        addCommands(loadClasses("me.qbosst.bossbot.bot.commands", Command::class.java).filter { it.parent == null })
    }


    override fun onEvent(event: GenericEvent)
    {
        listener(event)
        listeners.forEach { it.onEvent(event) }
    }
    
    private fun listener(event: GenericEvent)
    {
        when(event)
        {
            // If the event was a message received event
            is MessageReceivedEvent ->
            {
                if(event.isFromGuild)
                {
                    // Stores message into cache
                    messageCache.putMessage(event.message)

                    // Increases the author's message count by 1 in the cache
                    val key = Key.Type.USER_GUILD.genKey("", event.author.idLong, event.guild.idLong)
                    textCounterCache.put(key, (textCounterCache.get(key) ?: 0) + 1)
                    { k, v ->
                        // If someone else's cached message count was removed to make space for the author's one, it will save the removed one to the database
                        if(v > 0)
                        {
                            GuildUserData.update(k.idTwo, k.idOne,
                                    { insert ->
                                        insert[GuildUserDataTable.message_count] = v
                                    },
                                    { rs, update ->
                                        update[GuildUserDataTable.message_count] = rs[GuildUserDataTable.message_count] + v
                                    })
                        }
                    }
                }

                // Return if the message was sent by a bot account
                if(event.author.isBot)
                {
                    onNonCommandEvent(event)
                }
                else if(event.isWebhookMessage)
                {
                    onNonCommandEvent(event)
                }
                else
                {
                    val content = event.message.contentRaw
                    val prefix = GuildSettingsData.get(if(event.isFromGuild) event.guild else null).getPrefixOr(Config.Values.DEFAULT_PREFIX.getStringOrDefault())

                    // Checks if the message starts with the bot's prefix
                    if(content.startsWith(prefix) && content.length > prefix.length)
                    {
                        // Checks if the author entered a bot command
                        var args = content.substring(prefix.length).split("\\s+".toRegex())
                        var command = getCommand(args[0])

                        // If the command is not null, that means its a valid command
                        if(command != null)
                        {
                            // Checks if the author entered sub-commands
                            var index = 1
                            while(args.size > index)
                            {
                                val cmd = command?.getCommand(args[index])
                                if(cmd != null)
                                {
                                    command = cmd
                                    index++
                                } else break
                            }

                            val key = Key.Type.USER_GUILD.genKey("", event.author.idLong, event.guild.idLong)
                            if(!rateLimiter.contains(key))
                            {
                                rateLimiter.put(key, RateLimitStatus(event.message.timeCreated))
                            }

                            if(rateLimiter.get(key)!!.update(event.message.timeCreated) < 6)
                            {
                                // Checks if the author has any special permission requirements that the command may have
                                if(!command!!.hasPermission(if(event.isFromGuild) event.guild else null, event.author))
                                {
                                    event.channel.sendMessage("You do not have permission for this command!").queue()
                                    return
                                }

                                if(event.isFromGuild)
                                {
                                    if(!event.guild.selfMember.hasPermission(command.fullBotPermissions) || !event.guild.selfMember.hasPermission(event.textChannel, command.fullBotPermissions))
                                    {
                                        if(event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_WRITE))
                                        {
                                            event.channel.sendMessage("I need the following permissions to use this command; `${command.fullBotPermissions.joinToString("`, `")}`").queue()
                                        }
                                        else
                                        {
                                            event.author.openPrivateChannel()
                                                    .flatMap { it.sendMessage("I need the following permissions to use this command; `${command.fullBotPermissions.joinToString("`, `")}`") }
                                                    .queue({}, {})
                                        }
                                        return
                                    }

                                    // Checks if the author has permission to use the command in the guild
                                    if(!event.member!!.hasPermission(command.fullUserPermissions) || !event.member!!.hasPermission(event.textChannel, command.fullUserPermissions))
                                    {
                                        return
                                    }
                                }
                                else
                                {
                                    // Checks if the command is a guild-only command
                                    if(command.guildOnly)
                                    {
                                        event.channel.sendMessage("This command is a guild-only command!").queue()
                                        return
                                    }
                                }

                                args = args.drop(index)

                                try
                                {
                                    command.execute(event, args)
                                }
                                catch (e: Exception)
                                {
                                    event.channel.sendMessage("An error has occurred...").queue()

                                    val sw = StringWriter()
                                    e.printStackTrace(PrintWriter(sw))
                                    BossBot.LOG.error("$sw")

                                    BossBot.shards.getUserById(Config.Values.DEVELOPER_ID.getLongOrDefault())?.openPrivateChannel()?.map {
                                        val message: String = kotlin.run {
                                            val message = "A problem has happened with me! Check console for more details... ```$sw```"
                                            if(message.length > Message.MAX_CONTENT_LENGTH) message.substring(0, Message.MAX_CONTENT_LENGTH-3)+"```" else message
                                        }
                                        it.sendMessage(message).queue()
                                    }?.queue()
                                }

                            }

                            // If the author has been rate limiting the bot but hasn't been warned yet, it will tell them to slow down.
                            else if(!rateLimiter.get(key)!!.warned)
                            {
                                event.channel.sendMessage("Slow down a bit...").queue()
                                rateLimiter.get(key)!!.warned = true
                            }
                        }
                        else
                        {
                            onNonCommandEvent(event)
                        }
                    }
                    else
                    {
                        onNonCommandEvent(event)
                    }
                }
            }

            is MessageUpdateEvent ->
            {
                if(event.isFromGuild)
                {
                    if(event.author.isBot)
                        return
                    val old = messageCache.putMessage(event.message)

                    GuildSettingsData.get(event.guild).getMessageLogsChannel(event.guild)
                            ?.sendMessage(EmbedBuilder()
                                    .setAuthor("Message Edited", event.message.jumpUrl, event.author.effectiveAvatarUrl)
                                    .setThumbnail(event.author.avatarUrl)
                                    .setDescription("**Author**: ${event.author.asMention}\n**Channel**:${event.textChannel.asMention}\n[Jump to Message](${event.message.jumpUrl})")
                                    .addField("Message Content Before", old?.content?.makeSafe(MessageEmbed.VALUE_MAX_LENGTH) ?: "N/A", false)
                                    .addField("Message Content After", event.message.contentRaw.makeSafe(MessageEmbed.VALUE_MAX_LENGTH), false)
                                    .setFooter("User ID: ${event.author.idLong} | Message ID: ${event.message.idLong}")
                                    .setThumbnail(event.author.effectiveAvatarUrl)
                                    .setTimestamp(event.message.timeEdited)
                                    .setColor(Color.YELLOW)
                                    .build())
                            ?.queue()
                }
            }

            is MessageDeleteEvent ->
            {
                if(event.isFromGuild)
                {
                    val old = messageCache.pullMessage(event.guild, event.messageIdLong) ?: return
                    val tc = GuildSettingsData.get(event.guild).getMessageLogsChannel(event.guild) ?: return

                    val author = old.getAuthor(BossBot.shards)

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

                    embed.addField("Message Content", old.content.makeSafe(MessageEmbed.VALUE_MAX_LENGTH), false)

                    val message = tc.sendMessage(embed.build())
                    for(file in attachments) message.addFile(file)
                    message.queue { old.deleteFiles() }
                }
            }

            is GuildVoiceJoinEvent ->
            {
                if(!event.member.user.isBot)
                {
                    voiceCache[Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)] = VoiceMemberStatus(OffsetDateTime.now(), event.voiceState.isMuted)
                }

                GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.asMention}** has joined `${event.channelJoined.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .setColor(Color.GREEN)
                        .build())?.queue()
            }

            is GuildVoiceLeaveEvent ->
            {
                if(!event.member.user.isBot)
                {
                    val key = Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)

                    val stats = voiceCache.remove(key) ?: return
                    val now = OffsetDateTime.now()
                    if(event.voiceState.isMuted)
                    {
                        stats.update(now)
                    }
                    val total = Duration.between(stats.join, now).seconds
                    val loop: Long = (total - stats.secondsMuted) / seconds_until_eligible

                    GuildUserData.update(event.member,
                            { insert ->
                                insert[GuildUserDataTable.experience] = xp_to_give
                                insert[GuildUserDataTable.voice_chat_time] = total
                            },
                            { rs, update ->
                                update[GuildUserDataTable.experience] = rs[GuildUserDataTable.experience] + (xp_to_give * loop).toInt()
                                update[GuildUserDataTable.voice_chat_time] = rs[GuildUserDataTable.voice_chat_time] + total
                            })
                    BossBot.LOG.debug("[${event.guild.name}] ${event.member.user.asTag} has spent a total of ${secondsToString(total)} in vc, ${secondsToString(stats.secondsMuted)} muted")
                }

                GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.user.asMention}** has left `${event.channelLeft.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(Color.RED)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())?.queue()
            }

            is GuildVoiceMoveEvent ->
            {
                GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.user.asMention}** has switched channels: `${event.channelLeft.name}` -> `${event.channelJoined.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(Color.YELLOW)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())?.queue()
            }

            is GuildVoiceMuteEvent ->
            {
                if(!event.member.user.isBot)
                {
                    voiceCache[Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)]?.update(if(event.isMuted) OffsetDateTime.now() else null)
                }
            }

            is StatusChangeEvent ->
            {
                if(event.newStatus == JDA.Status.CONNECTED)
                {
                    event.jda.presence.setPresence(
                            OnlineStatus.ONLINE,
                            Activity.of(Activity.ActivityType.DEFAULT, "${Config.Values.DEFAULT_PREFIX.getStringOrDefault()}help")
                    )
                }

                if(event.newStatus == JDA.Status.SHUTTING_DOWN)
                {
                    //TODO
                }
            }

            is GuildMemberRemoveEvent ->
            {
                if(event.guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS))
                {
                    val tc = GuildSettingsData.get(event.guild).getModerationLogsChannel(event.guild) ?: return
                    event.guild.retrieveAuditLogs()
                            .limit(5)
                            .map { list -> list.filter { it.type == ActionType.KICK || it.type == ActionType.BAN } }
                            .map { list -> list.filter { it.targetIdLong == event.user.idLong } }
                            .map { list -> list.filter { it.timeCreated.isAfter(OffsetDateTime.now().minusSeconds(60)) } }
                            .map { list -> list.firstOrNull() }
                            .queue()
                            {
                                if(it != null)
                                {
                                    val embed = EmbedBuilder()
                                            .setTitle("${event.user.asTag} has been ${if (it.type == ActionType.BAN) "banned" else "kicked"}")
                                            .appendDescription("Moderator responsible: ${
                                            if(it.user == event.jda.selfUser)
                                            {
                                                val id = it.reason!!.split("\n")[0]
                                                BossBot.shards.getUserById(id)?.asTag ?: "User `${id}`"
                                            }
                                            else
                                            {
                                                //TODO get user id
                                                it.user?.asTag ?: "N/A"
                                            }
                                            }")
                                            .appendDescription("\nReason provided: ${it.reason ?: "none"}")
                                            .setTimestamp(OffsetDateTime.now())

                                    tc.sendMessage(embed.build()).queue()
                                }
                            }
                }
            }
            is GuildMemberJoinEvent ->
            {
                val settings = GuildSettingsData.get(event.guild)
                val tc = settings.getWelcomeChannel(event.guild) ?: return
                val message = settings.getWelcomeMessage(event.member) ?: return

                val permissions = mutableSetOf(Permission.MESSAGE_WRITE)
                if(message.embeds.isNotEmpty())
                    permissions.add(Permission.MESSAGE_EMBED_LINKS)

                if(!event.guild.selfMember.hasPermission(permissions) || !event.guild.selfMember.hasPermission(tc, permissions))
                    return
                else
                    tc.sendMessage(message).queue()
            }
        }
    }

    private fun onNonCommandEvent(event: MessageReceivedEvent)
    {
        if(event.author.isBot) return

        // Checks if the message is pinging the bot, if so it will return the prefix that it is using
        if(event.message.contentRaw.matches(Regex("<@!?${event.jda.selfUser.idLong}>$")))
        {
            val prefix = GuildSettingsData.get(if(event.isFromGuild) event.guild else null).getPrefixOr(Config.Values.DEFAULT_PREFIX.getStringOrDefault())
            event.channel.sendMessage("My prefix is `${prefix}`!").queue()
        }

        if(event.isFromGuild)
        {
            val key = Key.Type.USER_GUILD.genKey("", event.author.idLong, event.guild.idLong)
            if(textTimerCache.get(key)?.plusSeconds(seconds_until_eligible)?.isBefore(event.message.timeCreated) != false)
            {
                textTimerCache.put(key, event.message.timeCreated)
                { k, v ->
                    BossBot.LOG.debug("$k was removed from the text timer cache")
                    if(v.plusSeconds(seconds_until_eligible).isAfter(OffsetDateTime.now()))
                    {
                        BossBot.LOG.warn("The cache size for the text timer needs to be increased!")
                        val new = FixedCache(if (textTimerCache.size() > 10) (textTimerCache.size() * 1.2).roundToInt() else textCounterCache.size() + 10, textTimerCache)
                        new.put(k, v)
                        textTimerCache = new
                    }
                }

                GuildUserData.update(event.member!!,
                        { insert ->
                            insert[GuildUserDataTable.experience] = xp_to_give
                            insert[GuildUserDataTable.message_count] = textCounterCache.pull(key) ?: 0
                            insert[GuildUserDataTable.text_chat_time] = seconds_until_eligible
                        },
                        { rs, update ->
                            update[GuildUserDataTable.experience] = rs[GuildUserDataTable.experience] + xp_to_give
                            update[GuildUserDataTable.message_count] = rs[GuildUserDataTable.message_count] + (textCounterCache.pull(key) ?: 0)
                            update[GuildUserDataTable.text_chat_time] = rs[GuildUserDataTable.text_chat_time] + seconds_until_eligible
                        })
            }
        }
    }

    fun getCachedMessageCount(member: Member): Int
    {
        return textCounterCache.get(Key.Type.USER_GUILD.genKey("", member.idLong, member.guild.idLong)) ?: 0
    }

    fun getCachedVoiceChatTime(member: Member): Long
    {
        val time = voiceCache[Key.Type.USER_GUILD.genKey("", member.idLong, member.guild.idLong)]?.join
        return if(time == null) 0L else Duration.between(time, OffsetDateTime.now()).seconds
    }

    override fun getCommand(name: String): Command?
    {
        val label = name.toLowerCase()
        return commands[label] ?: commandAlias[label]
    }

    override fun getCommands(): Collection<Command>
    {
        return commands.values
    }

    override fun addCommand(command: Command)
    {
        commands[command.name] = command
        command.aliases.forEach { commandAlias[it] = command }
    }

    override fun addCommands(vararg commands: Command)
    {
        commands.forEach { addCommand(it) }
    }

    override fun addCommands(commands: Collection<Command>)
    {
        commands.forEach { addCommand(it) }
    }

    private data class RateLimitStatus(
            var time: OffsetDateTime
    )
    {
        var count: Int = 0
        var warned: Boolean = false

        fun update(time: OffsetDateTime): Int
        {
            if(this.time.plusSeconds(secondsUntilUpdate).isAfter(time))
                count++
            else
            {
                count = 0
                warned = false
            }
            this.time = time
            return count
        }

        companion object
        {
            private const val secondsUntilUpdate: Long = 4L
        }
    }

    private class VoiceMemberStatus(
            val join: OffsetDateTime,
            isMuted: Boolean
    )
    {
        var mute: OffsetDateTime? = if(isMuted) join else null
        var secondsMuted: Long = 0

        fun update(mute: OffsetDateTime?)
        {
            if(this.mute != null)
                secondsMuted += Duration.between(this.mute, OffsetDateTime.now()).seconds

            this.mute = mute
        }
    }
}
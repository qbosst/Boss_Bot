package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.ICommandManager
import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildRoleDataTable
import me.qbosst.bossbot.database.tables.GuildUserDataTable
import me.qbosst.bossbot.entities.MessageCache
import me.qbosst.bossbot.entities.database.GuildRolesData
import me.qbosst.bossbot.entities.database.GuildSettingsData
import me.qbosst.bossbot.entities.database.GuildUserData
import me.qbosst.bossbot.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
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
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.math.roundToInt

object Listener : EventListener, ICommandManager
{
    // List of all 'main' commands
    private val commands = mutableMapOf<String, Command>()

    // Economy parameters
    private const val seconds_until_eligible = 60L
    private const val xp_to_give = 10 //TODO make adjustable

    // Caches
    private var textTimerCache = FixedCache<Key, OffsetDateTime>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
    private var textCounterCache = FixedCache<Key, Int>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
    private val messageCache = MessageCache(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
    private val voiceCache = mutableMapOf<Key, VoiceMemberStatus>()

    // Commands that have also implement Event Listeners
    private val listeners: List<EventListener>

    init
    {
        // Get all commands using reflection
        val commands = loadObjects("${BossBot::class.java.`package`.name}.commands", Command::class.java)
        BossBot.LOG.debug("Found ${commands.size} commands: ${commands.joinToString(", ") { it.fullName }}")

        // Add all the 'main' commands to the map
        addCommands(commands.filter { it.parent == null })

        // Add all commands that implement Event Listener to listeners
        listeners = commands.filter { it is EventListener }.map { it as EventListener }
    }

    override fun onEvent(event: GenericEvent)
    {
        // Executes the event
        listener(event)

        // Passes this event onto the other command listeners
        for(listener in listeners)
            try
            {
                listener.onEvent(event)
            }
            catch (e: Exception)
            {
                BossBot.LOG.error("Caught unhandled exception in one of the command listeners", e)
            }
    }
    
    private fun listener(event: GenericEvent)
    {
        // Checks if event is instance of something and if so runs the correlating method
        when(event)
        {
            is MessageReceivedEvent -> onMessageReceivedEvent(event)
            is MessageUpdateEvent -> onMessageUpdateEvent(event)
            is MessageDeleteEvent -> onMessageDeleteEvent(event)
            is GuildVoiceJoinEvent -> onGuildVoiceJoinEvent(event)
            is GuildVoiceLeaveEvent -> onGuildVoiceLeaveEvent(event)
            is GuildVoiceMoveEvent -> onGuildVoiceMoveEvent(event)
            is GuildVoiceMuteEvent -> onGuildVoiceMuteEvent(event)
            is StatusChangeEvent -> onStatusChangeEvent(event)
            is GuildMemberRemoveEvent -> onGuildMemberLeaveEvent(event)
            is GuildMemberJoinEvent -> onGuildMemberJoinEvent(event)
        }
    }

    private fun onMessageReceivedEvent(event: MessageReceivedEvent)
    {
        val settings = GuildSettingsData.get(event.getGuildOrNull())

        if(event.isFromGuild)
        {
            // Checks if guild has a message logs channel, if so it will store the message
            if(settings.getMessageLogsChannel(event.guild) != null)
                messageCache.putMessage(event.message)

            // Increase the author's message count by 1 in the cache
            val key = Key.Type.USER_GUILD.genKey("", event.author.idLong, event.guild.idLong)
            textCounterCache.put(key, (textCounterCache.get(key) ?: 0) + 1)
            { key, value ->
                // If someone else's cached message count was removed to make space for this one, it will save the removed one to the database
                if(value > 0)
                    GuildUserData.update(key.idTwo, key.idOne,
                            { insert ->
                                insert[GuildUserDataTable.message_count] = value
                            },
                            { rs, update ->
                                update[GuildUserDataTable.message_count] = rs[GuildUserDataTable.message_count] + value
                            })
            }
        }

        if(event.author.isBot || (event.isFromGuild && event.member == null))
            onNonCommandEvent(event, settings)
        else
        {
            val content = event.message.contentRaw
            val prefix = settings.prefix ?: Config.Values.DEFAULT_PREFIX.getStringOrDefault()

            // Checks if the message starts with the bots prefix (checks if message is command)
            if(content.startsWith(prefix) && content.length > prefix.length)
            {
                // Checks if the author entered a bot command
                var args = content.substring(prefix.length).split(Regex("\\s+"))
                var command = getCommand(args[0])
                if(command != null)
                {
                    // Checks if the author entered sub-commands
                    var index = 1
                    while(args.size > index)
                    {
                        val new = command?.getCommand(args[index])
                        if(new != null)
                        {
                            command = new
                            index++
                        }
                        else
                            break
                    }

                    command = command!!

                    // Checks permissions
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

                    // Executes command
                    else
                        try
                        {
                            command.execute(event, args.drop(index))
                        }
                        catch (e: Throwable)
                        {
                            BossBot.LOG.error("An unhandled exception has occurred while trying to execute a command", e)
                            if(event.isFromGuild && event.guild.selfMember.hasPermission(event.textChannel, listOf(Permission.MESSAGE_WRITE)))
                                event.channel.sendMessage("An error has occurred...").queue()
                        }
                }
                else
                    onNonCommandEvent(event)
            }
            else
                onNonCommandEvent(event)
        }
    }

    private fun onMessageUpdateEvent(event: MessageUpdateEvent)
    {
        if(event.isFromGuild)
        {
            val old = messageCache.putMessage(event.message)
            if(event.author.isBot)
                return

            // Logs the message update event
            GuildSettingsData.get(event.guild).getMessageLogsChannel(event.guild)
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
            val textChannel = GuildSettingsData.get(event.guild).getMessageLogsChannel(event.guild) ?: return

            if(old != null)
            {
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

    private fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        // Logs the guild voice join event for stats
        if(!event.member.user.isBot)
            voiceCache[Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)] = VoiceMemberStatus(OffsetDateTime.now(), event.voiceState.isMuted)

        // Logs the guild voice join event for logging
        GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                    .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                    .setDescription("**${event.member.asMention}** has joined `${event.channelJoined.name}`")
                    .setFooter("User ID : ${event.member.idLong}")
                    .setTimestamp(OffsetDateTime.now())
                    .setThumbnail(event.member.user.effectiveAvatarUrl)
                    .setColor(Color.GREEN)
                    .build())
                ?.queue()
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        // Logs the guild voice leave event for stats
        if(!event.member.user.isBot)
        {
            val key = Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)

            val stats = voiceCache.remove(key) ?: return
            val now = OffsetDateTime.now()
            if(event.voiceState.isMuted)
                stats.update(now)

            val total = Duration.between(stats.join, now).seconds
            val loop: Long = (total - stats.secondsMuted) / seconds_until_eligible

            // Updates stats
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

        // Logs the guild voice leave event for logging
        GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                    .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                    .setDescription("**${event.member.user.asMention}** has left `${event.channelLeft.name}`")
                    .setFooter("User ID : ${event.member.idLong}")
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(Color.RED)
                    .setThumbnail(event.member.user.effectiveAvatarUrl)
                    .build())
                ?.queue()
    }

    private fun onGuildVoiceMoveEvent(event: GuildVoiceMoveEvent)
    {
        // Logs the guild voice move event for logging
        GuildSettingsData.get(event.guild).getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                    .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                    .setDescription("**${event.member.user.asMention}** has switched channels: `${event.channelLeft.name}` -> `${event.channelJoined.name}`")
                    .setFooter("User ID : ${event.member.idLong}")
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(Color.YELLOW)
                    .setThumbnail(event.member.user.effectiveAvatarUrl)
                    .build())
                ?.queue()
    }

    private fun onGuildVoiceMuteEvent(event: GuildVoiceMuteEvent)
    {
        // Logs the guild voice mute event for stats
        if(!event.member.user.isBot)
            voiceCache[Key.Type.USER_GUILD.genKey("", event.member.idLong, event.guild.idLong)]?.update(if(event.isMuted) OffsetDateTime.now() else null)
    }

    private fun onStatusChangeEvent(event: StatusChangeEvent)
    {
        when(event.newStatus)
        {
            // Sets the status for the jda when the it is finished loading
            JDA.Status.CONNECTED ->
            {
                event.jda.presence.setPresence(
                        OnlineStatus.ONLINE,
                        Activity.of(Activity.ActivityType.DEFAULT, "${Config.Values.DEFAULT_PREFIX.getStringOrDefault()}help")
                )

                // Adds all the members in vc in a guild to the voice cache to log stats
                for(guild in event.jda.guilds)
                    for(vc in guild.voiceChannels)
                        for(member in vc.members)
                            if(member.voiceState != null)
                                voiceCache[Key.Type.USER_GUILD.genKey("", member.idLong, guild.idLong)] = VoiceMemberStatus(OffsetDateTime.now(), member.voiceState!!.isMuted)
            }
        }
    }

    private fun onGuildMemberLeaveEvent(event: GuildMemberRemoveEvent)
    {
        //TODO
    }

    private fun onGuildMemberJoinEvent(event: GuildMemberJoinEvent)
    {
        val settings = GuildSettingsData.get(event.guild)
        val tc = settings.getWelcomeChannel(event.guild)
        val message = settings.getWelcomeMessage(event.member)

        // Sends welcome message
        if(tc != null && message != null)
        {
            val permissions = mutableSetOf(Permission.MESSAGE_WRITE)
            if(message.embeds.isNotEmpty())
                permissions.add(Permission.MESSAGE_EMBED_LINKS)

            if(event.guild.selfMember.hasPermission(permissions) && event.guild.selfMember.hasPermission(tc, permissions))
                tc.sendMessage(message).queue()
        }

        // Gives auto roles to new member
        if(event.guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
        {
            val roles = GuildRolesData.get(event.guild).getRoles(event.guild, GuildRoleDataTable.Type.AUTO_ROLE)
                    .filter { event.guild.selfMember.canInteract(it) }

            event.guild.modifyMemberRoles(event.member, roles, null).reason("Auto Role(s)").queue()
        }
    }

    private fun onNonCommandEvent(event: MessageReceivedEvent, settings: GuildSettingsData = GuildSettingsData.get(event.getGuildOrNull()))
    {
        // Ignore bot input
        if(event.author.isBot)
            return

        // Checks if the message is pinging the bot, if so it will return the prefix that it is using
        if(event.message.contentRaw.matches(Regex("<@!?${event.jda.selfUser.idLong}>$")))
        {
            val prefix = settings.prefix ?: Config.Values.DEFAULT_PREFIX.getStringOrDefault()
            event.channel.sendMessage("My prefix is `${prefix}`!").queue()
        }

        if(event.isFromGuild && event.member != null)
        {
            val key = Key.Type.USER_GUILD.genKey("", event.author.idLong, event.guild.idLong)
            if(textTimerCache.get(key)?.plusSeconds(seconds_until_eligible)?.isBefore(event.message.timeCreated) != false)
            {
                textTimerCache.put(key, event.message.timeCreated)
                { removedKey, value ->
                    BossBot.LOG.debug("$removedKey was removed from the text timer cache")
                    if(value.plusSeconds(seconds_until_eligible).isAfter(OffsetDateTime.now()))
                    {
                        BossBot.LOG.warn("The cache size for the text timer needs to be increased!")
                        val new = FixedCache(if(textTimerCache.size() > 10) (textTimerCache.size() * 1.25).roundToInt() else textCounterCache.size() + 10, textTimerCache)
                        new.put(removedKey, value)
                        textTimerCache = new
                    }
                }

                // Update user stats
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

    fun getCachedMessageCount(guild: Guild, userId: Long): Int
    {
        return textCounterCache.get(Key.Type.USER_GUILD.genKey("", userId, guild.idLong)) ?: 0
    }

    fun getCachedMessageCount(member: Member): Int
    {
        return getCachedMessageCount(member.guild, member.idLong)
    }

    fun getCachedVoiceChatTime(guild: Guild, userId: Long): Long
    {
        val time = voiceCache[Key.Type.USER_GUILD.genKey("", userId, guild.idLong)]?.join
        return if(time != null) Duration.between(time, OffsetDateTime.now()).seconds else 0
    }

    fun getCachedVoiceChatTime(member: Member): Long
    {
        return getCachedVoiceChatTime(member.guild, member.idLong)
    }

    override fun getCommand(name: String): Command?
    {
        return commands[name.toLowerCase()]
    }

    override fun getCommands(): Collection<Command>
    {
        return commands.values
    }

    override fun addCommand(command: Command)
    {
        commands[command.name.toLowerCase()] = command
        for(alias in command.aliases)
            commands[alias.toLowerCase()] = command
    }

    override fun addCommands(commands: Collection<Command>)
    {
        for(command in commands)
            addCommand(command)
    }

    /**
     *  Class used to track voice member stats like time spent in vc, time spent muted in vc
     */
    private class VoiceMemberStatus(
            val join: OffsetDateTime,
            isMuted: Boolean
    )
    {
        // Time of the mute
        var mute: OffsetDateTime? = if(isMuted) join else null
            private set

        // Seconds that the member has already been muted for
        var secondsMuted: Long = 0
            private set

        /**
         *  Used when member mutes or un-mutes their mic so that it can be logged.
         */
        fun update(mute: OffsetDateTime?)
        {
            if(this.mute != null)
                secondsMuted += Duration.between(this.mute, OffsetDateTime.now()).seconds

            this.mute = mute
        }
    }
}
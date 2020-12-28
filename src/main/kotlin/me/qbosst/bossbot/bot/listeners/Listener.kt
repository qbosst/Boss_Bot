package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.handlers.CommandHandler
import me.qbosst.bossbot.bot.listeners.handlers.EconomyHandler
import me.qbosst.bossbot.bot.listeners.handlers.EventLogger
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.util.extensions.getGuildOrNull
import me.qbosst.bossbot.util.loadObjectOrClass
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.*
import net.dv8tion.jda.api.events.message.*
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Listener(cacheSize: Int): EventListener
{
    val ecoHandler = EconomyHandler(cacheSize)

    private val eventLogger = object: EventLogger(cacheSize)
    {
        override fun getLogger(event: GenericEvent): TextChannel? = when(event)
        {
            is GenericMessageEvent ->
                event.getGuildOrNull()?.let { guild -> guild.getSettings().getMessageLogsChannel(guild) }
            is MessageBulkDeleteEvent ->
                event.guild.getSettings().getMessageLogsChannel(event.guild)
            is GenericGuildVoiceEvent ->
                event.guild.getSettings().getVoiceLogsChannel(event.guild)
            else -> null
        }
    }

    val cmdHandler = CommandHandler()
            .apply {
                val commands = loadObjectOrClass("${BossBot::class.java.`package`.name}.commands", Command::class.java)
                LOG.info("Registered {} commands: {}", commands.size, commands.map { it.fullName })

                val parents = commands.filter { it.parent == null }
                LOG.info("Registered {} parent commands: {}", parents.size, parents.map { it.fullName })

                this+parents
            }

    override fun onEvent(event: GenericEvent) = when(event)
    {
        is MessageReceivedEvent ->
            onMessageReceivedEvent(event)
        is MessageUpdateEvent ->
            onMessageUpdateEvent(event)
        is MessageDeleteEvent ->
            onMessageDeleteEvent(event)

        is GuildVoiceJoinEvent ->
            onGuildVoiceJoinEvent(event)
        is GuildVoiceLeaveEvent ->
            onGuildVoiceLeaveEvent(event)
        is GuildVoiceMuteEvent ->
            onGuildVoiceMuteEvent(event)
        is GuildVoiceMoveEvent ->
            onGuildVoiceMoveEvent(event)

        is ReadyEvent ->
            onReadyEvent(event)

        else -> {}
    }

    private fun onMessageReceivedEvent(event: MessageReceivedEvent)
    {
        eventLogger.logMessageReceivedEvent(event)
        val isCommandEvent = cmdHandler.handle(event)
        ecoHandler.handleMessageReceivedEvent(event, isCommandEvent)
    }

    private fun onMessageUpdateEvent(event: MessageUpdateEvent)
    {
        eventLogger.logMessageUpdateEvent(event)
    }

    private fun onMessageDeleteEvent(event: MessageDeleteEvent)
    {
        eventLogger.logMessageDeleteEvent(event)
    }

    private fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        eventLogger.logGuildVoiceJoinEvent(event)
        ecoHandler.handleGuildVoiceJoinEvent(event)
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        eventLogger.logGuildVoiceLeaveEvent(event)
        ecoHandler.handleGuildVoiceLeaveEvent(event)
    }

    private fun onGuildVoiceMuteEvent(event: GuildVoiceMuteEvent)
    {
        ecoHandler.handleGuildVoiceMuteEvent(event)
    }

    private fun onGuildVoiceMoveEvent(event: GuildVoiceMoveEvent)
    {
        eventLogger.logGuildVoiceMoveEvent(event)
    }

    private fun onReadyEvent(event: ReadyEvent)
    {
        event.jda.presence.setPresence(OnlineStatus.ONLINE, Activity.playing("Hello :D"))
        LOG.info("Shard #{} Ready!", event.jda.shardInfo.shardId)
    }

    companion object
    {
        private val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
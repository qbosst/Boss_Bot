package me.qbosst.bossbot.listeners

import dev.minn.jda.ktx.CoroutineEventListener
import me.qbosst.bossbot.BossBot
import me.qbosst.bossbot.commands.colour.ColourCommand
import me.qbosst.bossbot.commands.dev.*
import me.qbosst.bossbot.commands.misc.*
import me.qbosst.bossbot.commands.settings.guild.SetCommand
import me.qbosst.bossbot.commands.settings.guild.SetPrefixCommand
import me.qbosst.bossbot.commands.spacespeak.SpaceSpeakCommand
import me.qbosst.bossbot.commands.time.TimeCommand
import me.qbosst.bossbot.database.manager.GuildColoursManager
import me.qbosst.bossbot.database.manager.GuildSettingsManager
import me.qbosst.bossbot.database.manager.settings
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.entities.parsers.ColourParser
import me.qbosst.bossbot.entities.parsers.UnitParser
import me.qbosst.bossbot.entities.parsers.ZoneIdParser
import me.qbosst.bossbot.listeners.handlers.CommandClient
import me.qbosst.bossbot.listeners.handlers.EconomyHandler
import me.qbosst.bossbot.listeners.handlers.EventLogger
import me.qbosst.bossbot.listeners.handlers.commandClient
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.events.message.*

@OptIn(ExperimentalStdlibApi::class)
class Listener(cacheSize: Int): CoroutineEventListener {

    val commandClient = commandClient {
        developerIds = BossBot.config.developerIds
        commands = buildList {
            // dev
            add(EvaluateCommand())
            add(ReadDirectMessagesCommand())
            add(ActivityCommand())
            add(OnlineStatusCommand())
            add(ShutdownCommand())
            add(BotStatisticsCommand())
            add(CommandStatisticsCommand())

            // misc
            add(InviteCommand())
            add(VoteCommand())
            add(EightBallCommand())
            add(PingCommand())
            add(AvatarCommand())

            // time
            add(TimeCommand())

            // spacespeak
            add(SpaceSpeakCommand())

            // colour
            add(ColourCommand())

            // settings
            add(SetCommand())
        }

        registerDefaultParsers()
        registerParser(UnitParser())
        registerParser(ColourParser())
        registerParser(ZoneIdParser())
    }

    private val eventLogger = object: EventLogger(cacheSize) {
        override fun getLogger(event: GenericEvent): TextChannel? = when(event) {
            is GenericMessageEvent -> {
                if(event.isFromGuild) {
                    val guild = event.guild
                    guild.getTextChannelById(guild.settings.messageLogsChannelId)
                } else {
                    null
                }
            }
            is MessageBulkDeleteEvent ->
                event.guild.getTextChannelById(event.guild.settings.messageLogsChannelId)
            else ->
                null
        }
    }

    val ecoHandler = EconomyHandler(cacheSize)

    override suspend fun onEvent(event: GenericEvent) {
        when(event) {
            is MessageReceivedEvent -> onMessageReceivedEvent(event)
            is MessageDeleteEvent -> onMessageDeleteEvent(event)
            is MessageUpdateEvent -> onMessageUpdateEvent(event)

            is GuildVoiceJoinEvent -> onGuildVoiceJoinEvent(event)
            is GuildVoiceLeaveEvent -> onGuildVoiceLeaveEvent(event)
            is GuildVoiceMuteEvent -> onGuildVoiceMuteEvent(event)

            is GuildLeaveEvent -> onGuildLeaveEvent(event)
            is ReadyEvent -> onReadyEvent(event)
            is ShutdownEvent -> onShutdownEvent(event)
        }
    }

    private suspend fun onMessageReceivedEvent(event: MessageReceivedEvent) {
        eventLogger.logMessageReceivedEvent(event)
        val isCommandEvent = commandClient.handle(event)
        ecoHandler.handleMessageReceivedEvent(event, isCommandEvent)
    }

    private fun onMessageDeleteEvent(event: MessageDeleteEvent) {
        eventLogger.logMessageDeleteEvent(event)
    }

    private fun onMessageUpdateEvent(event: MessageUpdateEvent) {
        eventLogger.logMessageUpdateEvent(event)
    }

    private fun onGuildLeaveEvent(event: GuildLeaveEvent) {
        GuildSettingsManager.delete(event.guild.idLong)
        GuildColoursManager.delete(event.guild.idLong)
    }

    private fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent) {
        ecoHandler.handleGuildVoiceJoinEvent(event)
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent) {
        ecoHandler.handleGuildVoiceLeaveEvent(event)
    }

    private fun onGuildVoiceMuteEvent(event: GuildVoiceMuteEvent) {
        ecoHandler.handleGuildVoiceMuteEvent(event)
    }

    private fun onShutdownEvent(event: ShutdownEvent) {
        ecoHandler.save(event.jda)
    }

    private fun onReadyEvent(event: ReadyEvent) {
        event.jda.presence.setPresence(
            OnlineStatus.ONLINE,
            Activity.competing("1st place for best bot on Discord :)"),
        )
    }


}
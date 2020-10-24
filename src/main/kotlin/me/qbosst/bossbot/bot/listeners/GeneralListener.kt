package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.database.managers.GuildSettingsManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.EventListener

object GeneralListener: EventListener
{
    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is ReadyEvent ->
                onReadyEvent(event)
            is GuildLeaveEvent ->
                onGuildLeaveEvent(event)
        }
    }

    private fun onReadyEvent(event: ReadyEvent)
    {
        event.jda.presence.setPresence(OnlineStatus.ONLINE, Activity.playing("bruh"))
    }

    private fun onGuildLeaveEvent(event: GuildLeaveEvent)
    {
        // Deletes all the custom colours from the guild
        GuildColoursManager.clear(event.guild)

        // Deletes all the settings from the guild
        GuildSettingsManager.clear(event.guild)
    }
}
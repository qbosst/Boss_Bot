package me.qbosst.bossbot.bot.listeners

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener

object GeneralListener: EventListener
{
    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is ReadyEvent ->
                onReadyEvent(event)
        }
    }

    private fun onReadyEvent(event: ReadyEvent)
    {
        event.jda.presence.setPresence(OnlineStatus.ONLINE, Activity.playing("bruh"))
    }
}
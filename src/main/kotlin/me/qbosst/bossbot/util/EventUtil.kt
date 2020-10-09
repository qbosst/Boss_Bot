package me.qbosst.bossbot.util

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.GenericMessageEvent

fun GenericMessageEvent.getGuildOrNull(): Guild?
{
    return if(isFromGuild) guild else null
}
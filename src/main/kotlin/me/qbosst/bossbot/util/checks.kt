package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.core.event.message.MessageCreateEvent

fun defaultCheck(event: MessageCreateEvent): Boolean = when {
    event.message.author.isNullOrBot() -> false

    else -> true
}
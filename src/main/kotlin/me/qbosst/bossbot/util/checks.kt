package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent

fun defaultCheck(event: MessageCreateEvent): Boolean = when {
    event.message.author.isNullOrBot() -> false

    else -> true
}

suspend fun isNotBot(event: Event): Boolean = userFor(event)?.asUserOrNull()?.isNullOrBot()?.not() ?: true
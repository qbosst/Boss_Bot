package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.checks.messageFor
import dev.kord.core.event.Event

suspend fun defaultCheck(event: Event): Boolean {
    val message = messageFor(event)?.asMessage()

    return when {
        // make sure this is a message event
        message == null -> false

        // prevent web hook messages
        message.author == null -> false

        // make sure we are not replying to ourself
        message.author!!.id == event.kord.selfId -> false

        // make sure author is not a bot
        message.author!!.isBot -> false

        else -> true
    }
}
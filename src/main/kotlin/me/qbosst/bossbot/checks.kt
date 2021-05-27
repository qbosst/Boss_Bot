package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.checks.CheckFun
import com.kotlindiscord.kord.extensions.checks.failed
import com.kotlindiscord.kord.extensions.checks.passed
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import mu.KotlinLogging

fun defaultMessageCheck(): suspend (MessageCreateEvent) -> Boolean {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.defaultMessageCheck")

    suspend fun inner(event: MessageCreateEvent): Boolean {
        return when {
            event.message.author.isNullOrBot() -> {
                logger.failed("Author is either a webhook or bot.")
                false
            }
            else -> {
                logger.passed()
                true
            }
        }
    }

    return ::inner
}

fun isUser(vararg ids: Long): CheckFun {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.isUser")

    suspend fun inner(event: Event): Boolean {
        val user = userFor(event)

        if(user == null) {
            logger.failed("This event does not have a user associated.")
            return false
        }

        for(id in ids) {
            if(id == user.id.value) {
                logger.passed()
                return true
            }
        }

        logger.failed("The user id did not match any of the ids given")
        return false
    }

    return ::inner
}
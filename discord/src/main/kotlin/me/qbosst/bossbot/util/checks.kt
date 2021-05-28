package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.checks.CheckFun
import com.kotlindiscord.kord.extensions.checks.failed
import com.kotlindiscord.kord.extensions.checks.passed
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import mu.KotlinLogging

fun defaultMessageCommandCheck(event: MessageCreateEvent): Boolean {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.util.defaultMessageCheck")

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

suspend fun isNotBot(event: Event): Boolean {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.util.isNotBot")

    val user = userFor(event)

    return when {
        user == null -> {
            logger.failed("This event does not have a user associated.")
            false
        }
        user.asUser().isBot -> {
            logger.failed("This user is a bot.")
            false
        }
        else -> {
            logger.passed()
            true
        }
    }
}

fun isUser(vararg ids: Long): CheckFun {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.util.isUser")

    suspend fun inner(event: Event): Boolean {
        val user = userFor(event)

        return when {
            user == null -> {
                logger.failed("This event does not have a user associated.")
                false
            }
            ids.none { it == user.id.value } -> {
                logger.failed("The user's id did not match any of the ids given")
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
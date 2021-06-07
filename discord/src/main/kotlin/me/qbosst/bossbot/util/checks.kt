package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.checks.CheckFun
import com.kotlindiscord.kord.extensions.checks.failed
import com.kotlindiscord.kord.extensions.checks.passed
import com.kotlindiscord.kord.extensions.checks.userFor
import dev.kord.core.event.Event
import mu.KotlinLogging

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
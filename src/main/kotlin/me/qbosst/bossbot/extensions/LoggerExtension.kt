package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent

class LoggerExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "logger"

    override suspend fun setup() {
        event<MessageUpdateEvent> {
            check(::anyGuild)

            action {
                val logger = event.logger ?: return@action

            }
        }

        event<MessageDeleteEvent> {
            check(::anyGuild)

            action {
                val logger = event.logger ?: return@action
            }
        }
    }

    private val Event.logger: MessageChannelBehavior?
        get() = when(this) {
            is MessageUpdateEvent, is MessageDeleteEvent -> null
            else -> null
        }
}
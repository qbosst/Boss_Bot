package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.interaction.edit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class MiscExtension: Extension() {
    override val name: String get() = "misc"

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        slashCommand {
            name = "ping"
            description = "Pings the bot"
            autoAck = AutoAckType.PUBLIC
            guild(714482588005171200)

            action {
                val (followUp, time) = measureTimedValue { publicFollowUp { content = "Pinging..." } }
                followUp.edit {
                    content = "${time.toLongMilliseconds()}ms"
                }
            }
        }
    }
}
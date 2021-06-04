package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.reply
import kotlin.time.measureTimedValue

class MiscExtension: Extension() {
    override val name: String get() = "misc"

    override suspend fun setup() {
        slashCommand {
            name = "ping"
            description = "Pings the bot"
            autoAck = AutoAckType.PUBLIC

            action {
                val (followUp, time) = measureTimedValue { publicFollowUp { content = "Pinging..." } }
                followUp.edit {
                    content = "${time.inWholeMilliseconds}ms"
                }
            }
        }

        command {
            name = "ping"
            description = "Pings the bot"

            action {
                val (followUp, time) = measureTimedValue {
                    message.reply {
                        allowedMentions {}
                        content = "Pinging..."
                    }
                }

                followUp.edit {
                    content = "${time.inWholeMilliseconds}ms"
                }
            }
        }
    }
}
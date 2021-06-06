package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.edit
import me.qbosst.bossbot.util.cache.hybridCommand
import kotlin.time.measureTimedValue

class MiscExtension: Extension() {
    override val name: String get() = "misc"

    override suspend fun setup() {
        hybridCommand {
            name = "ping"
            description = "Pings the bot"

            action {
                val (message, time) = measureTimedValue {
                    publicFollowUp {
                        allowedMentions {}
                        content = "Pinging..."
                    }
                }

                message.edit {
                    content = "${time.inWholeMilliseconds}ms"
                }
            }
        }
    }
}
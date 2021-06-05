package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import kotlinx.coroutines.delay
import me.qbosst.bossbot.util.cache.hybridCommand
import kotlin.time.Duration
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
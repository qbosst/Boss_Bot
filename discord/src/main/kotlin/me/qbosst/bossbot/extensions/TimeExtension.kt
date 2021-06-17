package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.reply

class TimeExtension: Extension() {
    override val name: String get() = "time"

    override suspend fun setup() {
        slashCommand {
            action {
                val m = publicFollowUp {  }

                m.edit {  }

                val r = ephemeralFollowUp("") {

                }
            }
        }

        command {
            action {
                val m = message.reply {  }

                m.edit {  }

                m.delete()
            }
        }
    }
}
package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.channel.createMessage

class TestExtension: Extension() {
    override val name: String
        get() = "test"

    override suspend fun setup() {
        command {
            action {
                this.channel.createMessage {
                    components {

                    }
                }
            }
        }
    }
}
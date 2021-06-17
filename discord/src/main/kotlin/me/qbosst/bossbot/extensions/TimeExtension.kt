package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import me.qbosst.bossbot.util.hybridCommand

class TimeExtension: Extension() {
    override val name: String get() = "time"

    override suspend fun setup() {
        hybridCommand {

        }
    }
}
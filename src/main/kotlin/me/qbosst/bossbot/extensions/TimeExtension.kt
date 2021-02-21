package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension

class TimeExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "time"

    override suspend fun setup() {
        TODO("Not yet implemented")
    }
}
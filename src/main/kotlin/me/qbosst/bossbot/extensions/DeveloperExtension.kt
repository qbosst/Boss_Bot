package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.isUser

class DeveloperExtension: Extension() {
    override val name: String get() = "developer"

    override suspend fun setup() {
        check(isUser(env("discord.developer_id")!!.toLong()))

        command {
            name = "botstatistics"
            aliases = arrayOf("botstats")

            action {
                message.reply {
                    content = "Test"
                    allowedMentions {}
                }
            }
        }
    }
}
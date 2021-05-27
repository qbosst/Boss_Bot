package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.getUrl
import com.kotlindiscord.kord.extensions.utils.users
import dev.kord.core.behavior.reply
import kotlinx.coroutines.flow.count
import me.qbosst.bossbot.isUser

class DeveloperExtension: Extension() {
    override val name: String get() = "developer"

    override suspend fun setup() {
        check(isUser(env("discord.developer_id")!!.toLong()))

        command {
            name = "botstatistics"
            aliases = arrayOf("botstats")

            action {
                val self = event.kord.getSelf()

                val guilds = event.kord.guilds.count()
                val cachedUsers = event.kord.users.count()

                val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
                val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

                message.getUrl()

                message.reply {
                    allowedMentions {}

                    embed {
                        title = "${self.tag} statistics"

                        field("Memory Usage", true) { "${usedMb}MB / ${totalMb}MB"}
                        field("Guilds", true) { guilds.toString() }
                        field("Cached Users", true) { cachedUsers.toString() }
                    }
                }
            }
        }
    }
}
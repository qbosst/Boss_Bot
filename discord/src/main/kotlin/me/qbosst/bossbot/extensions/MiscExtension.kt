package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.Color
import me.qbosst.bossbot.commands.hybrid.behaviour.edit
import me.qbosst.bossbot.util.getColour
import me.qbosst.bossbot.util.hybridCommand
import me.qbosst.bossbot.util.random
import kotlin.time.measureTimedValue

class MiscExtension: Extension() {
    override val name: String get() = "misc"

    class AvatarArgs: Arguments() {
        val member by optionalMember("user", "The user's avatar you want to display")
    }

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

        hybridCommand(::AvatarArgs) {
            name = "avatar"
            description = "Displays a user's avatar"

            action {
                val target = arguments.member?.asUser() ?: user!!.asUser()

                publicFollowUp {
                    allowedMentions {}

                    embed {
                        image = "${target.avatar.url}?size=256"
                        color = guild?.getMemberOrNull(kord.selfId)?.getColour() ?: Color.random()
                    }
                }
            }

            messageSettings { aliases = arrayOf("av") }
        }
    }
}
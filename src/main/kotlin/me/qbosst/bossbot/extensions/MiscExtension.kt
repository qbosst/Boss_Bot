package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.optionalUser
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.edit
import dev.kord.core.event.message.MessageCreateEvent
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.replyEmbed
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class MiscExtension(bot: ExtensibleBot, val voteLinks: List<String>): Extension(bot) {
    override val name: String = "misc"

    class AvatarArgs: Arguments() {
        val user by optionalUser("user", "the user's avatar you want to display", outputError = true)
    }

    class EightBallArgs: Arguments() {
        val question by coalescedString("question", "the question to ask 8ball")
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        // reply with prefix if bot is mentioned
        event<MessageCreateEvent> {
            check { event ->
                val content = event.message.content
                val selfId = event.kord.selfId.value

                content == "<@${selfId}>" || content == "<@!${selfId}>"
            }

            action {
                val prefix = bot.settings.commandsBuilder.prefixCallback.invoke(event, bot.settings.commandsBuilder.defaultPrefix)

                event.message.reply(false) {
                    content = "My prefix is `${prefix}`"
                }
            }
        }

        command {
            name = "ping"

            action {
                val (message, time) = measureTimedValue { event.message.reply(false) { content = "Pinging..." } }
                message.edit {
                    content = "${time.toLongMilliseconds()}ms"
                }
            }
        }

        command(::AvatarArgs) {
            name = "avatar"
            aliases = arrayOf("av")

            action {
                val user = arguments.user ?: event.message.author!!
                message.replyEmbed {
                    description = "[${user.tag}](${user.avatar.url})"
                    image = user.avatar.url
                }
            }
        }

        command {
            name = "vote"

            val voteDescription = "- "+voteLinks.joinToString("\n - ")

            action {
                val self = event.kord.getSelf()
                message.replyEmbed {
                    author {
                        name = "Enjoying ${self.username}? Upvote it here!"
                        icon = self.avatar.url
                    }
                    footer {
                        text = "Thank you!"
                    }
                    description = voteDescription
                }
            }
        }

        command(::EightBallArgs) {
            name = "8ball"

            val responses = listOf(
                "As I see it, yes.",
                "Ask again later.",
                "Better not tell you now.",
                "Cannot predict now.",
                "Concentrate and ask again.",
                "Don't count on it.",
                "It is certain.",
                "It is decidedly so.",
                "Most likely.",
                "My reply is no.",
                "My sources say no.",
                "Outlook not so good",
                "Outlook good.",
                "Reply hazy, try again.",
                "Signs point to yes.",
                "Very doubtful",
                "Without a doubt.",
                "Yes.",
                "Yes - definitely.",
                "You may rely on it."
            )

            action {
                message.reply(false) {
                    content = responses.random()
                }
            }
        }
    }
}
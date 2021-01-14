package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.NumberConverter
import com.kotlindiscord.kord.extensions.commands.converters.number
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.converters.ColourCoalescingConverter
import me.qbosst.bossbot.converters.ColourConverter
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.blend
import me.qbosst.bossbot.util.ColourUtil.nextColour
import kotlin.random.Random

class ColourExtension(bot: ExtensibleBot): Extension(bot) {

    override val name: String = "colours"

    override suspend fun setup() {
        group(::colourGroup)
    }

    private suspend fun colourGroup(command: GroupCommand) = command.apply {
        name = "colour"

        val parser = object: Arguments() {
            val colour by arg("colour", ColourCoalescingConverter { ColourUtil.systemColours })
        }

        signature { parser }

        action {
            with(parse { parser }) {
                message.reply {
                    ColourUtil.buildColourEmbed(this, colour, "colour.png")

                    allowedMentions {
                        repliedUser = false
                    }
                }
            }
        }

        command(::randomCommand)
        command(::blendCommand)
    }

    private suspend fun randomCommand(command: Command) = command.apply {
        name = "random"

        val parser = object: Arguments() {
            val isAlpha by defaultingBoolean("isAlpha", false)
        }

        signature { parser }

        action {
            with(parse { parser }) {
                message.reply {
                    val colour = Random.nextColour(isAlpha)
                    ColourUtil.buildColourEmbed(this, colour, "colour.png")

                    allowedMentions {
                        repliedUser = false
                    }
                }
            }
        }
    }

    private suspend fun blendCommand(command: Command) = command.apply {
        name = "blend"

        val parser = object: Arguments() {
            val colours by arg("colours", ColourConverter { ColourUtil.systemColours }.toMulti())
        }

        action {
            with(parse { parser }) {
                val blended = colours.blend()
                message.reply {
                    ColourUtil.buildColourEmbed(this, blended, "colour.png")

                    allowedMentions {
                        repliedUser = false
                    }
                }
            }
        }
    }
}
package me.qbosst.bossbot.commands.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.OptionalConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder

class SingleToNullableConverter<T: Any>(
    val singleConverter: SingleConverter<T>
): OptionalConverter<T>(outputError = true) {

    override val signatureTypeString: String = singleConverter.signatureTypeString

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        if(SET_WORDS.any { word -> arg.equals(word, true) }) {
            this.parsed = null
            return true
        } else {
            val result = singleConverter.parse(arg, context, bot)
            this.parsed = singleConverter.parsed
            return result
        }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = singleConverter.toSlashOption(arg)

    companion object {
        val SET_WORDS = listOf("null", "none", "default")
    }
}
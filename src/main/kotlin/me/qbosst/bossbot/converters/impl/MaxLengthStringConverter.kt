package me.qbosst.bossbot.converters.impl

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import kotlin.reflect.KProperty

class MaxLengthStringConverter(val minLength: Int = 0, val maxLength: Int): SingleConverter<String>() {
    override val signatureTypeString: String = "text"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        when {
            arg.length > maxLength -> {
                println("max")
                throw ParseException("'${arg}' cannot be longer than $maxLength characters")
            }
            arg.length < minLength -> {
                println("min")
                throw ParseException("'${arg}' cannot be fewer than $minLength characters")
            }
        }

        this.parsed = arg
        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}
package me.qbosst.bossbot.commands.converters.impl

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder

class MaxStringConverter(val minLength: Int = 0, val maxLength: Int): SingleConverter<String>() {
    override val signatureTypeString: String = "text"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean = when {
        arg.length > maxLength ->
            throw ParseException("'${arg}' cannot be longer than $maxLength characters.")
        arg.length < minLength ->
            throw ParseException("'${arg}' cannot be fewer than $minLength characters.")
        else -> {
            this.parsed = arg
            true
        }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}
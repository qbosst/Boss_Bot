package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.StringConverter
import com.kotlindiscord.kord.extensions.commands.parser.Arguments

/**
 * This will return the argument as it was given, however it first checks if the argument
 * is over the [maxLength]. If it is, it will throw an error
 */
class LengthyStringConverter(val maxLength: Int): SingleConverter<String>() {
    override val signatureTypeString: String = "text"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        if(arg.length > maxLength) {
            throw ParseException("'${arg}' cannot be longer than $maxLength characters")
        }

        this.parsed = arg
        return true
    }
    val s = StringConverter()
}
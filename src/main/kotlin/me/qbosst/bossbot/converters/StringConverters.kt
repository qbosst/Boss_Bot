package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Arguments

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
}

class LengthyCoalescingStringConverter(val maxLength: Int): CoalescingConverter<String>() {
    override val signatureTypeString: String = "text"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        val arg = args.joinToString(" ")
        if(arg.length > maxLength) {
            throw ParseException("'${arg}' cannot be longer than $maxLength characters")
        }

        this.parsed = arg
        return args.size
    }
}

fun Arguments.lengthyString(displayName: String, maxLength: Int) =
    arg(displayName, LengthyStringConverter(maxLength))

fun Arguments.lengthyCoalescingString(displayName: String, maxLength: Int) =
    arg(displayName, LengthyCoalescingStringConverter(maxLength))
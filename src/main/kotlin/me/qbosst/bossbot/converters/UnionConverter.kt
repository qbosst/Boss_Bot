package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.Converter
import com.kotlindiscord.kord.extensions.commands.converters.MultiConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder

class UnionConverter(
    val converters: Collection<Converter<*>>,
    override val shouldThrow: Boolean = false
): CoalescingConverter<Any>() {

    override val signatureTypeString: String = "any"

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        for(converter in converters) {
            try {
                when(converter) {
                    is MultiConverter<*> -> {
                        val result = converter.parse(args, context, bot)
                        if(result > 0) {
                            this.parsed = converter.parsed
                            return result
                        }
                    }
                    is CoalescingConverter<*> -> {
                        val result = converter.parse(args, context, bot)
                        if(result > 0) {
                            this.parsed = converter.parsed
                            return result
                        }
                    }
                    is SingleConverter<*> -> {
                        val arg = args[0]
                        val result = converter.parse(arg, context, bot)
                        if(result) {
                            this.parsed = converter.parsed
                            return 1
                        }
                    }
                    // unsupported converter
                    else -> {}
                }
            } catch (e: ParseException) {} // ignore
        }

        // this will run when none of the converters could convert
        if(shouldThrow) {
            val arg = args.joinToString(" ")
            throw ParseException("'${arg}' could not be parsed into ${converters.joinToString(", ") { it.signatureTypeString}}")
        }
        return 0
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}
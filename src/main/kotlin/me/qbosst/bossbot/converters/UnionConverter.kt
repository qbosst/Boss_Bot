package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*

class UnionConverter(val converters: Collection<Converter<*>>): CoalescingConverter<Any>() {

    override val signatureTypeString: String = "any"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        for(converter in converters) {
            try {
                when(converter) {
                    is MultiConverter<*> -> {
                        val result = converter.parse(args, context, bot)
                        this.parsed = converter.parsed
                        return result
                    }
                    is CoalescingConverter<*> -> {
                        val result = converter.parse(args, context, bot)
                        this.parsed = converter.parsed
                        return result
                    }
                    is SingleConverter<*> -> {
                        val arg = args.first()
                        val result = converter.parse(arg, context, bot)

                        if(result) {
                            this.parsed = converter.parsed
                        }
                        return 1
                    }
                }
            } catch (e: ParseException) {} // ignore error
        }

        return 0
    }
}
package me.qbosst.bossbot.converters.impl

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import me.qbosst.bossbot.util.TimeUtil
import java.time.Duration
import java.util.concurrent.TimeUnit

class DurationConverter: SingleConverter<Duration>() {
    override val signatureTypeString: String = "duration"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        val time = TimeUtil.parseTime(arg, TimeUnit.SECONDS)
            ?: throw ParseException("Could not parse '${arg}' into a duration")

        this.parsed = Duration.ofSeconds(time)
        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}
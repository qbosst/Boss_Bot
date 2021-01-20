package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
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
}
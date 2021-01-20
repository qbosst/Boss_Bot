package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import me.qbosst.bossbot.util.TimeUtil
import java.time.ZoneId

class ZoneIdConverter: SingleConverter<ZoneId>() {
    override val signatureTypeString: String = "zone id"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        val zoneId = TimeUtil.filterZones(arg).firstOrNull()
            ?: throw ParseException("'${arg}' is not a valid zone id")

        this.parsed = zoneId
        return true
    }
}
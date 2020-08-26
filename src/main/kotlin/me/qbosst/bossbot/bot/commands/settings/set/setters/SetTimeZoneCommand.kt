package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetterCommand
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.database.GuildSettingsData
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.DateTimeException
import java.time.ZoneId

object SetTimeZoneCommand: SetterCommand<ZoneId>(
        "timezone",
        aliases = listOf("zoneid"),
        displayName = "Timezone"
) {
    override fun isValid(event: MessageReceivedEvent, args: List<String>): Boolean
    {
        val string = args.joinToString(" ")
        return try {
            ZoneId.of(string)
            true
        } catch (e: DateTimeException) {
            event.channel.sendMessage("$string is not a valid zoneid!").queue()
            false
        }
    }

    override fun set(guild: Guild, obj: ZoneId?): ZoneId?
    {
        return try
        {
            val old = GuildSettingsData.update(guild, GuildSettingsDataTable.zone_id, obj?.id)
            if(old != null)
            {
                ZoneId.of(old)
            }
            else null
        }
        catch (e: DateTimeException)
        {
            null
        }
    }

    override fun get(guild: Guild): ZoneId?
    {
        return GuildSettingsData.get(guild).zone_id
    }

    override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): ZoneId?
    {
        val string = args.joinToString(" ")
        return try
        {
            ZoneId.of(string)
        }
        catch (e: DateTimeException)
        {
            event.channel.sendMessage("$string is not a valid timezone!").queue()
            null
        }
    }

    override fun getAsString(guild: Guild): String
    {
        return get(guild)?.toString() ?: "none"
    }
}
package me.qbosst.bossbot.bot.commands.misc.time

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.UserDataManager
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.TimeUtil.zoneIdOf
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object TimeSetCommand: Command(
        "set",
        description = "Sets the time zone that you are in",
        usage_raw = listOf("<zoneId>"),
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val name = args.joinToString(" ")
            val zoneId = TimeUtil.filterZones(name).firstOrNull()
            if(zoneId == null)
                event.channel.sendMessage(argumentInvalid(args[0], "time zone")).queue()
            else
            {
                val old = UserDataManager.update(event.author, UserDataTable.zone_id, zoneId.id)
                val oldZoneId = zoneIdOf(old)
                event.channel.sendMessage("Your timezone has been updated from `${if(oldZoneId == null) "none" else oldZoneId.id}` to `${zoneId.id}`").queue()
            }
        }
        else
            event.channel.sendMessage(argumentMissing("zone")).queue()
    }
}
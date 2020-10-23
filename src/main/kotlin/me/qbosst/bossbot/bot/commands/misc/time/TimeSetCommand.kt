package me.qbosst.bossbot.bot.commands.misc.time

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.entities.database.UserData
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.ZoneId

object TimeSetCommand: Command(
        "set",
        "Sets the timezone that you are in",
        usage = listOf("<zoneId>"),
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val name = args.joinToString(" ")
            val zoneId = getZoneId(name)
            if(zoneId == null)
            {
                event.channel.sendMessage("`${name.maxLength()}` is not a valid time zone!").queue()
                return
            }
            val old = UserData.update(event.author, UserDataTable.zone_id, zoneId.id)
            val oldZoneId = try { ZoneId.of(old) } catch (t: Throwable) { null }
            event.channel.sendMessage("Your timezone has been updated from `${if(oldZoneId == null) "none" else oldZoneId.id}` to `${zoneId.id}`").queue()
        }
        else
            event.channel
                    .sendMessage("Here is a list of zones that you can use.")
                    .addFile(ZoneId.getAvailableZoneIds()
                            .sortedBy { it }
                            .joinToString("\n")
                            .toByteArray(), "zone_ids.txt")
                    .queue()
    }

    private fun getZoneId(string: String): ZoneId?
    {
        return ZoneId.of(ZoneId.getAvailableZoneIds().firstOrNull { it.equals(string, true) } ?: return null)
    }
}
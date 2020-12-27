package me.qbosst.bossbot.bot.commands.misc.time

import me.qbosst.bossbot.bot.commands.meta.CommandSetter
import me.qbosst.bossbot.database.managers.UserDataManager
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object TimeSetCommand: CommandSetter<User, String>(
        "set",
        displayName = "Zone Id"
)
{
    override fun set(key: User, value: String?): String? = UserDataManager.update(key, UserDataTable.zoneId, value)

    override fun get(key: User): String? = key.getUserData().zone_id?.id

    override fun getValue(event: MessageReceivedEvent, args: List<String>, key: User): String?
    {
        val query = args.joinToString(" ")
        val results = TimeUtil.filterZones(query)
        println(results)
        return when
        {
            results.isEmpty() ->
            {
                onUnsuccessfulSet(event.channel, "Could not find any zone id matching `${query.maxLength(32)}`")
                null
            }
            results.size == 1 ->
                results[0].id
            else ->
                results[0].id // TODO make a role menu that shows up
        }
    }

    override fun getKey(event: MessageReceivedEvent, args: List<String>): User = event.author

}
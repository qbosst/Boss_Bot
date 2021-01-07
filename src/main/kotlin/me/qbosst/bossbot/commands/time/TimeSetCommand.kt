package me.qbosst.bossbot.commands.time


import me.qbosst.bossbot.commands.settings.CommandSetter
import me.qbosst.bossbot.database.manager.UserData
import me.qbosst.bossbot.database.manager.UserDataManager
import me.qbosst.bossbot.database.manager.userData
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.util.TimeUtil
import net.dv8tion.jda.api.entities.User
import java.time.ZoneId

class TimeSetCommand: CommandSetter<User, ZoneId>()
{
    override val label: String = "set"

    override fun get(key: User): ZoneId? = key.userData.zoneId

    override fun getKey(ctx: Context): User = ctx.author

    override fun set(key: User, value: ZoneId?): ZoneId? {
        val old = UserDataManager.update(key.idLong, UserDataTable.zoneId, value?.id)
        return TimeUtil.zoneIdOf(old)
    }
}
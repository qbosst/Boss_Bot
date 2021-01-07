package me.qbosst.bossbot.commands.settings.guild

import me.qbosst.bossbot.commands.settings.CommandSetter
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.entities.Guild

abstract class CommandGuildSetter<V>: CommandSetter<Guild, V>() {

    final override val guildOnly: Boolean = true

    final override fun getKey(ctx: Context): Guild = ctx.guild!!

}
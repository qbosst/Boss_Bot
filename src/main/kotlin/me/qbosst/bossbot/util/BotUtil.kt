package me.qbosst.bossbot.util

import me.qbosst.bossbot.bot.ZERO_WIDTH
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.sharding.ShardManager

fun getId(string: String): Long {
    return if(string.matches(Regex("(<@&?!?)?(<#!?)?\\d{17,19}(>)?$"))) {
        string.replace(Regex("\\D+"), "").toLong()
    } else 0L
}

fun Guild.getMemberByString(string: String): Member?
{
    return getMemberById(getId(string)) ?: if(string.matches(Regex(".{2,32}#\\d{4}$"))) {
        val parts: List<String> = string.split(Regex("#"))
        getMemberByTag(parts[0], parts[1])
    } else getMembersByName(string, false).firstOrNull()
}

fun ShardManager.getUserByString(string: String): User?
{
    return getUserById(getId(string)) ?: if(string.matches(Regex(".{2,32}#\\d{4}$"))) {
        val parts: List<String> = string.split(Regex("#"))
        getUserByTag(parts[0], parts[1])
    } else null
}

fun Guild.getRoleByString(string: String): Role?
{
    return getRoleById(getId(string)) ?: getRolesByName(string, true).firstOrNull()
}

fun Guild.getTextChannelByString(string: String): TextChannel?
{
    return getTextChannelById(getId(string)) ?: getTextChannelsByName(string, true).firstOrNull()
}

fun Member.addRole(role: Role): AuditableRestAction<Void>
{
    return guild.addRoleToMember(this, role)
}

fun Member.removeRole(role: Role): AuditableRestAction<Void>
{
    return guild.removeRoleFromMember(this, role)
}

fun Member.move(vc: VoiceChannel): RestAction<Void>
{
    return guild.moveVoiceMember(this, vc)
}

fun String.makeSafe(maxLength: Int = 32): String
{
    val new = replace("@", "@$ZERO_WIDTH")
    return if(new.length > maxLength) "${new.substring(0, maxLength-3)}..." else new
}
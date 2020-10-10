package me.qbosst.bossbot.util

import me.qbosst.bossbot.bot.ZERO_WIDTH
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.sharding.ShardManager

val USER_MENTION_REGEX = Regex("<@!?[0-9]{17,19}>$")
val CHANNEL_MENTION_REGEX = Regex("<#[0-9]{17,19}$")
val ROLE_MENTION_REGEX = Regex("<@&[0-9]{17,19}$")
val DISCORD_ID_REGEX = Regex("[0-9]{17,19}$")

val GENERAL_REGEX = Regex("")

fun getId(string: String): Long
{
    val regexes = listOf(USER_MENTION_REGEX, CHANNEL_MENTION_REGEX, ROLE_MENTION_REGEX, DISCORD_ID_REGEX)
    return if(regexes.any { regex -> string.matches(regex) })
        string.replace(Regex("\\D+"), "").toLong()
    else
        0
}

fun Guild.getMemberByString(string: String): Member?
{
    return getMemberById(getId(string)) ?: if(string.matches(Regex(".{2,32}#\\d{4}$")))
    {
        val parts = string.split(Regex("#"))
        getMemberByTag(parts[0], parts[1])
    }
    else
        getMembersByName(string, false).firstOrNull()
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

fun String.maxLength(maxLength: Int = 32): String
{
    val new = replace("@", "@$ZERO_WIDTH")
    return if(new.length > maxLength) "${new.substring(0, maxLength-3)}..." else new
}
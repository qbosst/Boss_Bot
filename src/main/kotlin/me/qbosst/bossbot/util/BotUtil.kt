package me.qbosst.bossbot.util

import me.qbosst.bossbot.bot.ZERO_WIDTH
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.managers.getSettings
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager
import java.time.ZoneId


val LONG_REGEX = Regex("\\d{17,19}")

/**
 *  Get's a discord ID based on a mention or if its a long value
 *  @param id The string to try and get the id from
 *  @return Discord ID in Long value
 */
fun getId(id: String): Long
{
    if(id.matches(LONG_REGEX))
        return id.replace(Regex("\\D+"), "").toLong()

    for(type in enumValues<Message.MentionType>())
        if(id.matches(type.pattern.toRegex()))
            return id.replace(Regex("\\D+"), "").toLong()

    return -1
}

/**
 *  Searches for a member in a guild by their ID, tag or name.
 *
 *  @param query The string to try and search the member by
 *
 *  @return The member in that guild. Null if no member was found
 */
fun Guild.getMemberByString(query: String): Member? = getMemberById(getId(query)) ?: if(query.matches(User.USER_TAG.toRegex())) getMemberByTag(
    query
) else null ?: getMembersByEffectiveName(query, false).firstOrNull()

/**
 *  Searches for a user within the whole bot by their ID or tag.
 *
 *  @param query The string to try and search the user by
 *
 *  @return A user object. Null if no user was found.
 */
fun ShardManager.getUserByString(query: String): User? = getUserById(getId(query)) ?: getUserByTag(query)

/**
 *  Searches for a role in a guild based on it's id and name
 *
 *  @param query The string to try and search the role by
 *
 *  @return A role. Null if no role was found
 */
fun Guild.getRoleByString(query: String): Role? = getRoleById(getId(query)) ?: getRolesByName(query, true).firstOrNull()

/**
 *  Searches for a text channel in a guild based on it's id and name
 *
 *  @param query The string to try and search the text channel by
 *
 *  @return A text channel. Null if no text channel was found.
 */
fun Guild.getTextChannelByString(query: String): TextChannel? = getTextChannelById(getId(query)) ?: getTextChannelsByName(
    query,
    true
).firstOrNull()

/**
 *  Moves a member in a voice channel
 *
 *  @param vc The voice channel to move the member to
 *
 *  @return The rest action for moving the member
 */
fun Member.move(vc: VoiceChannel): RestAction<Void> = guild.moveVoiceMember(this, vc)


fun String.maxLength(maxLength: Int = 32): String
{
    val new = replace("@", "@$ZERO_WIDTH")
    return if(new.length > maxLength) "${new.substring(0, maxLength - 3)}..." else new
}

fun GenericMessageEvent.getGuildOrNull(): Guild? = if(isFromGuild) guild else null

fun MessageReceivedEvent.getPrefix(): String = getGuildOrNull()?.getSettings()?.prefix ?: BotConfig.default_prefix

fun getZoneId(zoneId: String?): ZoneId?
{
    return ZoneId.of(ZoneId.getAvailableZoneIds().firstOrNull { it.equals(zoneId, true) } ?: return null)
}

fun String.split(partitionSize: Int): List<String>
{
    val parts = mutableListOf<String>()
    val len = length
    var i = 0;
    while (i < len)
    {
        parts.add(this.substring(i, Math.min(len, i+partitionSize)))
        i+= partitionSize
    }
    return parts
}
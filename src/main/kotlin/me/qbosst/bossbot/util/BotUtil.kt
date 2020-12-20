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

    for(type in listOf(Message.MentionType.USER, Message.MentionType.CHANNEL, Message.MentionType.ROLE))
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
fun Guild.getMemberByString(query: String): Member? = getMemberById(getId(query)) ?: if(query.matches(User.USER_TAG.toRegex())) getMemberByTag(query) else null ?: getMembersByName(query, true).firstOrNull()

/**
 *  Searches for a user within the whole bot by their ID or tag.
 *
 *  @param query The string to try and search the user by
 *
 *  @return A user object. Null if no user was found.
 */
fun ShardManager.getUserByString(query: String): User? = getUserById(getId(query)) ?: if(query.matches(User.USER_TAG.toRegex())) getUserByTag(query) else null

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
fun Guild.getTextChannelByString(query: String): TextChannel? = getTextChannelById(getId(query)) ?: getTextChannelsByName(query, true).firstOrNull()

/**
 *  Moves a member in a voice channel
 *
 *  @param vc The voice channel to move the member to
 *
 *  @return The rest action for moving the member
 */
fun Member.move(vc: VoiceChannel): RestAction<Void> = guild.moveVoiceMember(this, vc)

/**
 *  Cuts off a string if it reaches the max length allowed
 *
 *  @param maxLength The maximum allowed of length that the string is allowed to be. Default is 32
 *
 *  @return String that is no longer than the maximum length specified.
 */
fun String.maxLength(maxLength: Int = 32): String = if(this.length > maxLength) "${this.substring(0, maxLength - 3)}..." else this

/**
 *  Prevents Discord mentions by putting a zero width character after an '@'
 *
 *  @return Safe string that cannot mention anyone
 */
fun String.makeSafe(): String = replace("@", "@$ZERO_WIDTH")

/**
 *  Gets the guild from the generic message event. Null if the message was not from a guild
 *
 *  @return Guild that the message came from. Null if it did not come from a guild
 */
fun GenericMessageEvent.getGuildOrNull(): Guild? = if(isFromGuild) guild else null

/**
 *  Gets the self user's prefix for commands
 *
 *  @return String the prefix used to invoke commands
 */
fun MessageReceivedEvent.getPrefix(): String = getGuildOrNull()?.getSettings()?.prefix ?: BotConfig.default_prefix

/**
 *  Better way of getting zone ids.
 *
 *  @param zoneId The zoneId string to try and get the Zone Id object from
 *
 *  @return Zone Id object. Null if no zone id corresponded to the parameter given
 */
fun zoneIdOf(zoneId: String?): ZoneId?
{
    return ZoneId.of(ZoneId.getAvailableZoneIds().firstOrNull { it.equals(zoneId, true) } ?: return null)
}

/**
 *  Splits a string up every n amount of characters
 *
 *  @param partitionSize The amount of characters to split it up after
 *
 *  @return List of strings
 */
fun String.split(partitionSize: Int): List<String>
{
    val parts = mutableListOf<String>()
    val len = length
    var i = 0
    while (i < len)
    {
        parts.add(this.substring(i, Math.min(len, i+partitionSize)))
        i+= partitionSize
    }
    return parts
}
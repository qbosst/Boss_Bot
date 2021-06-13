package me.qbosst.bossbot.events

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.events.ExtensionEvent
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User

/**
 * This is an event representing a user has voted for the bot.
 *
 * @param userId The Discord id of the user who has voted for the bot.
 */
data class UserVoteEvent(
    override val bot: ExtensibleBot,
    val userId: Long,
    val voteSite: String
): ExtensionEvent {

    suspend fun getUser(): User? = bot.getKoin().get<Kord>().getUser(Snowflake(userId))
}

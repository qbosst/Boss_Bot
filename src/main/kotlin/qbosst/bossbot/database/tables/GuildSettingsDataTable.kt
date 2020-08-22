package qbosst.bossbot.database.tables

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object GuildSettingsDataTable : Table()
{
    val max_welcome_message_length = Message.MAX_CONTENT_LENGTH + MessageEmbed.EMBED_MAX_LENGTH_BOT
    const val max_prefix_length = 8
    private val max_zone_id_length: Int = ZoneId.getAvailableZoneIds().maxBy { it.length }?.length ?: 32

    val guild_id = long("GUILD_ID").default(0L)

    val suggestion_channel_id = long("SUGGESTION_CHANNEL_ID").default(0L)
    val message_logs_channel_id = long("MESSAGE_LOGS_CHANNEL_ID").default(0L)
    val voice_logs_channel_id = long("VOICE_LOGS_CHANNEL_ID").default(0L)
    val moderation_logs_channel_id = long("MODERATION_LOGS_CHANNEL_ID").default(0L)
    val welcome_channel_id = long("WELCOME_CHANNEL_ID").default(0L)

    val mute_role_id = long("MUTE_ROLE_ID").default(0L)
    val dj_role_id = long("DJ_ROLE_ID").default(0L)

    val welcome_message = varchar("WELCOME_MESSAGE", max_welcome_message_length).nullable()
    val zone_id = varchar("ZONE_ID", max_zone_id_length).nullable()
    val prefix = varchar("PREFIX", max_prefix_length).nullable()

    override val tableName
        get() = "GUILD_SETTINGS_DATA"

    override val primaryKey
        get() = PrimaryKey(guild_id)
}
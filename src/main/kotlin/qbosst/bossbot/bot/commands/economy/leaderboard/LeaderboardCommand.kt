package qbosst.bossbot.bot.commands.economy.leaderboard

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.database.tables.GuildUserDataTable
import qbosst.bossbot.util.assertNumber
import qbosst.bossbot.util.makeSafe
import java.time.OffsetDateTime

object LeaderboardCommand : Command(
        "leaderboard",
        aliases = listOf("lb")
)
{

    private const val limit_per_page = 5

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val type: Type = if(args.isNotEmpty())
        {
            enumValues<Type>().firstOrNull { it.matcher.matches(args[0].toLowerCase()) }
        }
        else
        {
            event.channel.sendMessage("Please mention the type of leaderboard you would like to view").queue()
            return
        } ?: kotlin.run {
            event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid leaderboard type!").queue()
            return
        }

        var page: Long = kotlin.run {
            val arguments = args.drop(1)
            if(arguments.isNotEmpty())
            {
                arguments[0].toLongOrNull() ?: kotlin.run {
                    event.channel.sendMessage("${arguments[0].makeSafe()} is not a valid page number!")
                    return
                }
            } else 1
        }-1

        transaction {
            val count: Long = GuildUserDataTable.select {GuildUserDataTable.guild_id.eq(event.guild.idLong)}.count()

            page = assertNumber(0, getMaxPages(count), page)

            val list = GuildUserDataTable
                    .slice(GuildUserDataTable.user_id, type.column)
                    .select {GuildUserDataTable.guild_id.eq(event.guild.idLong)}
                    .orderBy(type.column, SortOrder.DESC)
                    .limit(limit_per_page, limit_per_page * page)
                    .map { type.format.invoke(type.column, it) }

            val embed = EmbedBuilder()
                    .setTitle("${event.guild.name} ${type.formatName} Leaderboard")
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter("Page ${page+1} / ${getMaxPages(count)+1}")

            list.forEach { embed.appendDescription(it) }
            event.channel.sendMessage(embed.build()).queue()
        }
    }

    private enum class Type(val formatName: String, val matcher: Regex, val column: Column<*>, val format: (Column<*>, ResultRow) -> String)
    {
        EXPERIENCE("Experience", Regex("e?xp(erience)?$"), GuildUserDataTable.experience, { column, rs ->
            "<@${rs[GuildUserDataTable.user_id]}> -> ${rs[column]}xp\n"
        }),
        TEXT_CHAT_TIME("Text Chat Time", Regex("t(ext)?c(hat)?$"), GuildUserDataTable.text_chat_time, { column, rs ->
            "<@${rs[GuildUserDataTable.user_id]}> -> ${qbosst.bossbot.util.secondsToString(rs[column] as Long)} tc time\n"
        }),
        VOICE_CHAT_TIME("Voice Chat Time", Regex("v(oice)?c(hat)?$"), GuildUserDataTable.voice_chat_time, { column, rs ->
            "<@${rs[GuildUserDataTable.user_id]}> -> ${qbosst.bossbot.util.secondsToString(rs[column] as Long)} vc time\n"
        }),
        MESSAGES_SENT("Messages Sent", Regex("message|msg$"), GuildUserDataTable.message_count, { column, rs ->
            "<@${rs[GuildUserDataTable.user_id]}> -> ${rs[column]} messages sent\n"
        })
    }

    private fun getMaxPages(count: Long): Long
    {
        var maxPages: Long = count / limit_per_page
        if(count % limit_per_page == 0L && maxPages > 0)
        {
            maxPages -= 1
        }
        return maxPages
    }
}
package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.database.data.GuildPunishment
import me.qbosst.bossbot.database.data.GuildSettingsData
import me.qbosst.bossbot.database.tables.GuildPunishmentDataTable
import me.qbosst.bossbot.util.dateTimeFormatter
import me.qbosst.bossbot.util.embed.FieldMenuEmbed
import me.qbosst.bossbot.util.getId
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.secondsToString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*

object HistoryCommand : Command(
        "history",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
){
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val id: Long = kotlin.run {
                val id: Long = getId(args[0])
                if(id == 0L)
                {
                    event.channel.sendMessage("That is not a valid id!").queue()
                    return
                }
                id
            }

            val zoneId = GuildSettingsData.get(event.guild).zone_id
            val history = FieldMenuEmbed(3, GuildPunishmentDataTable.getHistory(event.guild.idLong, id).toMessageEmbedField(zoneId).toList())
            val page = if(args.size > 1)
            {
                if(args[1].toIntOrNull() != null) args[1].toInt()
                else
                {
                    event.channel.sendMessage("${args[1].makeSafe()} is not a valid page number!").queue()
                    return
                }
            } else 0

            event.channel.sendMessage(history.createPage(EmbedBuilder()
                    .setTitle("Punishment History for ${BossBot.shards.getUserById(id)?.asTag ?: "User `$id`"}")
                    .setThumbnail(BossBot.shards.getUserById(id)?.avatarUrl)
                    .setFooter("Dates are in ${TimeZone.getTimeZone(zoneId).getDisplayName(true, TimeZone.SHORT)}"),
                    page).build()).queue()
        }
    }

    private fun Collection<GuildPunishment>.toMessageEmbedField(zoneId: ZoneId): Collection<MessageEmbed.Field>
    {
        return mapIndexed {
            index, it ->
            MessageEmbed.Field(
                    "Case ${index+1}",
                    "**Type** `:` ${it.type.name}\n" +
                            "**Staff** `:` ${BossBot.shards.getUserById(it.issuer_id)?.asTag ?: "User `${it.issuer_id}`"}\n" +
                            "**Reason** `:` ${it.reason}\n" +
                            if(it.duration > 0) "**Duration** `:` ${secondsToString(it.duration)}\n" else "" +
                            "**Date** `:` ${dateTimeFormatter.format(it.date.atZone(zoneId))} `${secondsToString(Duration.between(it.date, Instant.now()).seconds)} ago`",
                    false
            )
        }
    }
}
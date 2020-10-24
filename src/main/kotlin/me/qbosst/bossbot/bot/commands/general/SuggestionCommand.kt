package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.*
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.misc.colour.mixColours
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.awt.Color
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

object SuggestionCommand: Command(
        "suggestion",
        aliases_raw = listOf("suggest"),
        usage_raw = listOf("<suggestion>"),
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)
), EventListener
{

    private const val SECONDS_UNTIL_NEXT_EDIT = 20L
    private const val MAX_SUGGESTION_LENGTH = 512

    private val rateLimiter = FixedCache<Long, SuggestionPair>(BotConfig.default_cache_size)

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        // Gets the guild's suggestion channel
        val tc =  event.guild.getSettings().getSuggestionChannel(event.guild)

        // Checks to see if there is a suggestion channel
        if(tc == null)
            event.channel.sendMessage("This guild does not have a suggestion channel setup!").queue()

        // Checks to see if bot has permissions for the suggestion channel
        else if(!event.guild.selfMember.hasPermission(tc, fullBotPermissions))
            event.channel.sendMessage("I need the following permissions on ${tc.asMention}; `${fullBotPermissions.joinToString("`, `")}`")

        // Gets suggestion and posts it to suggestion channel
        else if(args.isNotEmpty())
        {
            val suggestion = args.joinToString(" ")
            if(suggestion.length > MAX_SUGGESTION_LENGTH)
                event.channel.sendMessage("Your suggestion is too long, please shorten it down to a maximum of $MAX_SUGGESTION_LENGTH characters.").queue()
            else
                tc.sendMessage(suggestEmbed(event.member!!, args.joinToString(" ")).build()).queue {
                    it.addReaction(THUMBS_UP).queue()
                    it.addReaction(THUMBS_DOWN).queue()

                    event.message.delete().queue()
                    event.channel.sendMessage("Your suggestion has been posted!").queue()
                }
        }
        else
            event.channel.sendMessage("Please provide your suggestion.").queue()
    }

    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is GenericGuildMessageReactionEvent ->
            {
                // This will make sure that the bot doesn't respond to it's own reactions
                if(event.user == null || event.user!!.isBot)
                    return

                // This checks if the reaction was made in the guild's suggestion channel and the reaction is a thumbs up or down
                else if(listOf(THUMBS_UP, THUMBS_DOWN).contains(event.reactionEmote.name))
                {

                    /*
                    The purpose of waiting for at least a few seconds before editing the suggestion again is to prevent
                    rate limiting the bot
                     */

                    // This checks when this suggestion was last edited, if it's after the minimum wait time it will change instantly
                    if(!rateLimiter.contains(event.messageIdLong) || rateLimiter.get(event.messageIdLong)!!.lastEdit.plusSeconds(SECONDS_UNTIL_NEXT_EDIT).isBefore(OffsetDateTime.now()))
                        editEmbed(event)

                    // This checks if the change is already scheduled, if not it will schedule a change after the wait time is finished
                    else if(!rateLimiter.get(event.messageIdLong)!!.isScheduled)
                    {
                        BossBot.scheduler.schedule(
                                {
                                    editEmbed(event)
                                },
                                Duration.between(OffsetDateTime.now(), rateLimiter.get(event.messageIdLong)!!.lastEdit.plusSeconds(SECONDS_UNTIL_NEXT_EDIT)).seconds, TimeUnit.SECONDS)
                        rateLimiter.get(event.messageIdLong)!!.isScheduled = true
                    }
                    // This is invoked when the cool-down is active
                    else
                    {
                        //println("gotta wait another ${Duration.between(OffsetDateTime.now(), lastEdit[event.messageIdLong]!!.lastEdit.plusSeconds(seconds_until_next_edit)).seconds}")
                    }
                }
                else if(event.member?.isOwner == true && listOf(TICK, CROSS).contains(event.reactionEmote.name))
                {
                    event.channel.retrieveMessageById(event.messageIdLong).queue()
                    { message ->
                        if(isSuggestionEmbed(message))
                        {
                            val user = BossBot.SHARDS_MANAGER.getUserById(message.embeds[0].footer?.text?.replace("\\D+".toRegex(), "") ?: "")
                            if(user != null)
                                message.delete().queue()
                                {
                                    val sb = StringBuilder("You suggestion of `${message.embeds[0].description!!}` has been ")
                                    if(event.reactionEmote.name == TICK)
                                        sb.append("accepted.")
                                    else
                                        sb.append("declined.")
                                    user.openPrivateChannel().flatMap { it.sendMessage(sb) }.queue()
                                }
                        }
                    }
                }
            }
        }
    }

    private fun editEmbed(event: GenericGuildMessageReactionEvent)
    {
        event.channel.retrieveMessageById(event.messageIdLong).queue { message ->
            if(isSuggestionEmbed(message))
            {
                val up = message.reactions.firstOrNull { itt -> itt.reactionEmote.name == THUMBS_UP }?.count?.minus(1) ?: return@queue
                val down = message.reactions.firstOrNull { itt -> itt.reactionEmote.name == THUMBS_DOWN }?.count?.minus(1) ?: return@queue
                val total = up + down

                val colour: Color = when
                {
                    // If there are more up-votes than down-votes, it will mix green and yellow together
                    up > down ->
                    {
                        val ratio = up.toFloat() / total
                        mixColours(Pair(Color.GREEN, ratio), Pair(Color.YELLOW, 1 - ratio))
                    }

                    // If there are more down-votes than up-votes, it will mix red and yellow together
                    up < down ->
                    {
                        val ratio = down.toFloat() / total
                        mixColours(Pair(Color.RED, ratio), Pair(Color.YELLOW, 1 - ratio))
                    }

                    // If there are the same amount of up-votes as down-votes, it will return yellow
                    else -> Color.YELLOW
                }

                // Sets the new last time edited
                message.editMessage(EmbedBuilder(message.embeds[0]).setColor(colour).build()).queue {
                    rateLimiter.put(it.idLong, SuggestionPair(false, it.timeEdited!!))
                }
            }
        }
    }

    private fun suggestEmbed(member: Member, suggestion: String): EmbedBuilder
    {
        return EmbedBuilder()
                .setAuthor("Suggestion from ${member.user.asTag}", null, member.user.avatarUrl)
                .appendDescription(suggestion)
                .setFooter("User ID: ${member.idLong}")
                .setTimestamp(OffsetDateTime.now())
    }


    private fun isSuggestionEmbed(message: Message): Boolean
    {
        return !message.isWebhookMessage
                && message.embeds.size == 1
                && message.member == message.guild.selfMember
                && message.embeds[0].author?.name?.matches(Regex("Suggestion from .{0,32}#[0-9]{4}")) ?: false
    }

    private data class SuggestionPair(
            var isScheduled: Boolean,
            val lastEdit: OffsetDateTime
    )
}
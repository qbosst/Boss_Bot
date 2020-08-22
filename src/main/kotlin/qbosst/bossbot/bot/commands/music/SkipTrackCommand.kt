package qbosst.bossbot.bot.commands.music

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.EventListener
import qbosst.bossbot.bot.TICK
import qbosst.bossbot.entities.music.GuildAudioEventListener
import qbosst.bossbot.entities.music.GuildMusicManager

object SkipTrackCommand : MusicCommand(
        "skip",
        "Skips the current song playing"
), EventListener, GuildAudioEventListener
{
    private val votes = mutableMapOf<Long, Vote>()

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        if(GuildMusicManager.get(event.guild).getQueue().isEmpty())
        {
            event.channel.sendMessage("There is nothing in the queue!").queue()
        }
        else
        {
            when
            {
                event.member!!.isDj() -> skip(event.textChannel)

                getMembersConnected(event.guild, false).count() < 3 -> skip(event.textChannel)

                else ->
                {
                    votes[event.guild.idLong] = Vote(0, event.messageIdLong, event.guild.audioManager.connectedChannel!!.idLong)
                    event.message.addReaction(TICK).queue()
                }
            }
        }
    }

    override fun onEvent(event: GenericEvent) {
        when(event)
        {
            is GuildMessageReactionAddEvent ->
            {
                if(event.user.isBot) return

                if(votes.contains(event.guild.idLong))
                {
                    val current = votes[event.guild.idLong]!!
                    if(current.messageId == event.messageIdLong && event.member.voiceState?.channel?.idLong == current.voiceChannelId && event.reactionEmote.name == TICK)
                    {
                        current.count++

                        if(current.count++ >= getMembersConnected(event.guild, false).size-1)
                        {
                            skip(event.channel)
                            votes.remove(event.guild.idLong)
                        }
                    }
                }
            }
            is GuildMessageReactionRemoveEvent ->
            {
                if(event.user?.isBot == true) return

                if(votes.contains(event.guild.idLong))
                {
                    val current = votes[event.guild.idLong]!!
                    if(current.messageId == event.messageIdLong && event.member?.voiceState?.channel?.idLong == current.voiceChannelId && event.reactionEmote.name == TICK)
                    {
                        current.count--
                    }
                }
            }
            is GuildMessageReactionRemoveAllEvent ->
            {

                if(votes[event.guild.idLong]?.messageId == event.messageIdLong)
                {
                    votes.remove(event.guild.idLong)
                }
            }
            is GuildVoiceLeaveEvent ->
            {
                if(event.member == event.guild.selfMember)
                {
                    votes.remove(event.guild.idLong)
                }
            }
            is GuildVoiceMoveEvent ->
            {
                if(event.member == event.guild.selfMember)
                {
                    votes.remove(event.guild.idLong)
                }
            }
        }
    }

    private fun skip(channel: TextChannel)
    {
        GuildMusicManager.get(channel.guild).nextTrack()
        channel.sendMessage("The current track has been skipped!").queue()
    }

    private data class Vote(
            var count: Int,
            var messageId: Long,
            var voiceChannelId: Long
    )

    override fun onEvent(event: AudioEvent, guildId: Long) {
        when(event)
        {
            is TrackEndEvent ->
            {
                votes.remove(guildId)
            }
        }
    }

}
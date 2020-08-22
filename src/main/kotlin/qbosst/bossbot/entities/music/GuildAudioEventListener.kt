package qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent

interface GuildAudioEventListener
{
    fun onEvent(event: AudioEvent, guildId: Long)
}
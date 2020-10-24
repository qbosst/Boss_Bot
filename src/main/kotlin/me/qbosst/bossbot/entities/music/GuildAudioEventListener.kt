package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent

interface GuildAudioEventListener
{
    fun onEvent(manager: GuildMusicManager, event: AudioEvent)
}
package me.qbosst.bossbot.entities.music.source.youtube

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import java.net.MalformedURLException
import java.net.URL

class YoutubeModifiedAudioSourceManager: YoutubeAudioSourceManager()
{
    val scraper = YoutubeScraper(this.httpInterface)

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem?
    {
        if(isUrl(reference.identifier))
            return super.loadItem(manager, reference)
        else
        {
            val videoId = scraper.scrapeVideos(reference.identifier).firstOrNull()?.id ?: return null
            return super.loadItem(manager, AudioReference(videoId, videoId))
        }
    }

    private fun isUrl(input: String): Boolean = try { URL(input); true } catch (e: MalformedURLException) { false }
}
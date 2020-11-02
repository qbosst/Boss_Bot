package me.qbosst.bossbot.entities.music.source.spotify

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.credentials.ClientCredentials
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import me.qbosst.bossbot.entities.music.source.youtube.YoutubeScraper
import java.time.Duration
import java.time.OffsetDateTime
import java.util.regex.Pattern
import kotlin.math.abs

class SpotifyAudioSourceManager(
        clientId: String,
        clientSecret: String
): YoutubeAudioSourceManager()
{
    private val spotifyApi = SpotifyApi.Builder()
            .setClientSecret(clientSecret)
            .setClientId(clientId)
            .build()

    private val scraper = YoutubeScraper(this.httpInterface)

    private lateinit var accessToken: AccessToken

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem?
    {
        // Checks if identifier matches spotify links
        if (!trackUrlPattern.matcher(reference.identifier).matches())
            return null

        // Checks if access token is still valid, if not generates a new one
        if(!this::accessToken.isInitialized || accessToken.isExpired())
        {
            val credentials = AccessToken(spotifyApi.clientCredentials().build().execute())
            spotifyApi.accessToken = credentials.accessToken
            accessToken = credentials
        }

        // Gets the query and id from the spotify link
        val splitUrl = reference.identifier.split(Regex("/"))
        val query = enumValues<SpotifyQuery>().first { splitUrl[splitUrl.lastIndex-1] == it.regex }
        val id = splitUrl[splitUrl.lastIndex].split(Regex("\\?"))[0]


        val tracks = query.getTracks(spotifyApi, id)
                .mapNotNull { track ->
                    // Name to try and search the tracks by on youtube
                    val trackQuery = "${track.artists.joinToString(",")} ${track.name}"

                    // List containing all the videos found on youtube
                    val foundVideos = scraper.scrapeVideos(trackQuery)

                    println(foundVideos)

                    // Finds the video that has the closest duration to the spotify track
                    foundVideos
                            .filter { it.uploader.verified }
                            .minByOrNull { abs(track.durationMs/1000 - it.getDurationAsSeconds()) } ?: foundVideos
                            .filter { track.artists.firstOrNull { name -> name.equals(it.uploader.username, true) } != null }
                            .minByOrNull { abs(track.durationMs/1000 - it.getDurationAsSeconds()) } ?: foundVideos
                            .minByOrNull { abs(track.durationMs/1000 - it.getDurationAsSeconds()) }
                }

        return when(tracks.size)
        {
            // No tracks were loaded
            0 -> null

            // One track was loaded
            1 -> loadTrackWithVideoId(tracks.first().id, true)

            // Multiple tracks were loaded
            else -> {
                // Turns them into youtube audio tracks
                val ytTracks = tracks.map { track ->
                    val info = AudioTrackInfo(track.title, track.uploader.username, track.getDurationAsSeconds()*1000, track.id, false, track.url)
                    YoutubeAudioTrack(info, this)
                }
                // Returns playlist
                BasicAudioPlaylist("Playlist", ytTracks, ytTracks.first(), false)
            }
        }
    }

    companion object
    {
        private val SPOTIFY_URL_REGEX = "https?://open.spotify.com/(${enumValues<SpotifyQuery>().joinToString("|"){it.regex}})/[a-zA-Z0-9]+(\\?si=[a-zA-Z0-9-_]+)?"
        private val trackUrlPattern: Pattern = Pattern.compile(SPOTIFY_URL_REGEX)
    }

    private enum class SpotifyQuery(val regex: String, val getTracks: (SpotifyApi, String) -> List<SpotifyTrack>)
    {
        TRACK("track", { api, id ->
            listOf(api.getTrack(id).build().execute().toSpotifyTrack())
        }),
        ALBUM("album", {api, id ->
            api.getAlbumsTracks(id).build().execute().items.map { it.toSpotifyTrack() }
        }),
        PLAYLIST("playlist", {api, id ->
            api.getPlaylistsItems(id).build().execute().items
                    .filter { it.track.type == com.wrapper.spotify.enums.ModelObjectType.TRACK }
                    .map { (it.track as Track).toSpotifyTrack() }
        }),
        ARTIST("artist", {api, id ->
            api.getArtistsTopTracks(id, com.neovisionaries.i18n.CountryCode.EU).build().execute().map { it.toSpotifyTrack() }
        })
        ;

        companion object
        {
            private fun Track.toSpotifyTrack(): SpotifyTrack = SpotifyTrack(
                    artists = artists.map { it.name },
                    name = name,
                    uri = uri,
                    durationMs = durationMs,
                    id = id
            )

            private fun TrackSimplified.toSpotifyTrack(): SpotifyTrack = SpotifyTrack(
                    artists = artists.map { it.name },
                    name = name,
                    uri = uri,
                    durationMs = durationMs,
                    id = id
            )
        }
    }

    /**
     *  Keeps track of the current access token and its expiry
     *
     *  @param credentials The spotify credentials containing data needed for this class
     */
    private class AccessToken(credentials: ClientCredentials)
    {
        /**
         *  The date of which this access token will expire
         */
        private val expireDate = OffsetDateTime.now().plusSeconds(credentials.expiresIn.toLong())

        val accessToken: String = credentials.accessToken

        /**
         *  Method used to determine whether the access token has expired
         *
         *  @param date The date to check from (default is the time of which the method is executed)
         */
        fun isExpired(date: OffsetDateTime = OffsetDateTime.now()): Boolean = date.isAfter(expireDate)

        /**
         *  Returns the amount of seconds left until the access token expires
         *
         *  @param date The date to check from (default is the time of which the method is executed)
         */
        fun expiresIn(date: OffsetDateTime = OffsetDateTime.now()): Long = Duration.between(date, expireDate).seconds
    }
}
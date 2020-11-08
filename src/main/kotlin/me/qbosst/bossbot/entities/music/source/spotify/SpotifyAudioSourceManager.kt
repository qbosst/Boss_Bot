package me.qbosst.bossbot.entities.music.source.spotify

import com.neovisionaries.i18n.CountryCode
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.*
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.IModelObject
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import com.wrapper.spotify.model_objects.specification.Track
import java.io.DataInput
import java.io.DataOutput
import java.util.regex.Pattern

class SpotifyAudioSourceManager(
        private val api: SpotifyApi,
        private val youtubeAudioSourceManager: YoutubeAudioSourceManager
): AudioSourceManager
{

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem?
    {
        val urlMatcher = URL_PATTERN.matcher(reference.identifier)
        val uriMatcher = URI_PATTERN.matcher(reference.identifier)

        // Checks if the identifier matches the spotify urls, if not return null
        val matcher = if(urlMatcher.matches()) urlMatcher else if(uriMatcher.matches()) uriMatcher else return null

        // gets the route and id of the link
        val route = enumValues<SpotifyRoute>().first { matcher.group(1) == it.regex }
        val id = matcher.group(2)

        api.accessToken = api.clientCredentials().build().execute().accessToken

        // Return an audio item, depending on what the route is
        return when(route)
        {
            SpotifyRoute.TRACK ->
            {
                val track = api.getTrack(id).build().execute()

                val info = AudioTrackInfo(track.name, track.artists.joinToString(", ") {it.name},
                        track.durationMs.toLong(), track.id, false, track.uri)

                SpotifyAudioTrack(info, youtubeAudioSourceManager)
            }
            SpotifyRoute.PLAYLIST ->
            {
                val playlist = api.getPlaylist(id).build().execute()
                val tracks = mutableListOf<PlaylistTrack>()
                tracks.addAll(playlist.tracks.items)

                val totalCount = playlist.tracks.total
                var currentCount = tracks.size

                // Queries the api until it has all the tracks, spotify api can only retrieve 100 tracks from a playlist at a time
                while (totalCount > currentCount)
                {
                    val remaining = api.getPlaylistsItems(id).offset(currentCount).build().execute()
                    tracks.addAll(remaining.items)
                    currentCount = tracks.size
                }

                createPlaylist(playlist.name, tracks
                        .mapNotNull { try { it.track as Track } catch (e: ClassCastException) { null } }.toTypedArray()
                )
            }
            SpotifyRoute.ALBUM ->
            {
                val album = api.getAlbum(id).build().execute()

                // HIGHLY UNLIKELY that an artist album has more than 100 tracks so does not check for it

                createPlaylist(album.name, album.tracks.items)
            }
            SpotifyRoute.ARTIST ->
            {
                val tracks = api.getArtistsTopTracks(id, CountryCode.US).build().execute()

                createPlaylist("Top Tracks", tracks)
            }
        }
    }

    /**
     * Turns a list of spotify tracks into a playlist
     *
     * @param name The name of the playlist
     * @param tracks The tracks to add to the playlist
     *
     * @return BasicAudioPlaylist item
     */
    private fun createPlaylist(name: String, tracks: Array<out IModelObject>): BasicAudioPlaylist
    {
        return BasicAudioPlaylist(name, tracks
                .mapNotNull { track ->
                    try {
                        val clazz = track::class.java

                        // Get track parameters using reflection (spotify can return a TrackSimplified and Track)
                        val trackName = clazz.getMethod("getName").invoke(track) as String
                        val artists = (clazz.getMethod("getArtists").invoke(track) as Array<*>).mapNotNull { artist ->
                            if(artist != null)
                                artist::class.java.getMethod("getName").invoke(artist) as String
                            else
                                null
                        }.joinToString(", ")

                        val durationMs = clazz.getMethod("getDurationMs").invoke(track) as Int
                        val id = clazz.getMethod("getId").invoke(track) as String
                        val uri = clazz.getMethod("getUri").invoke(track) as String

                        // Put these parameters into an info object
                        val info = AudioTrackInfo(trackName, artists, durationMs.toLong(), id, false, uri)

                        // Return audio track
                        SpotifyAudioTrack(info, youtubeAudioSourceManager)
                    }
                    catch (e: Exception)
                    {
                        println("Could not load track: $track due to $e")
                        null
                    }
                },
                null, false
        )
    }

    override fun encodeTrack(track: AudioTrack?, output: DataOutput?) =
            youtubeAudioSourceManager.encodeTrack(track, output)

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack =
            youtubeAudioSourceManager.decodeTrack(trackInfo, input)

    override fun getSourceName(): String = "spotify"

    override fun shutdown() = youtubeAudioSourceManager.shutdown()

    override fun isTrackEncodable(track: AudioTrack): Boolean = youtubeAudioSourceManager.isTrackEncodable(track)

    companion object
    {
        private val URL_REGEX = "https?://open.spotify.com/(${enumValues<SpotifyRoute>().joinToString("|") {
            it.regex }})/([a-zA-Z0-9]+)(\\?si=[a-zA-Z0-9-_]+)?"
        val URL_PATTERN: Pattern = Pattern.compile(URL_REGEX)

        private val URI_REGEX = "spotify:(${enumValues<SpotifyRoute>().joinToString("|") {it.regex}}):([a-zA-Z0-9]+)"
        val URI_PATTERN: Pattern = Pattern.compile(URI_REGEX)
    }

    /**
     * Different types of spotify routes
     *
     * @param regex how to filter them
     */
    private enum class SpotifyRoute(val regex: String)
    {
        TRACK("track"),
        PLAYLIST("playlist"),
        ALBUM("album"),
        ARTIST("artist"),
    }
}
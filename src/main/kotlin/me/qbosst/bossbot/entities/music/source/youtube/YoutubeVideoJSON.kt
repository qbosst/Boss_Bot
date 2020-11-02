package me.qbosst.bossbot.entities.music.source.youtube

import org.json.JSONObject

data class YoutubeVideoJSON private constructor(
        val id: String,
        val title: String,
        val url: String,
        val duration: String,
        val snippet: String,
        val upload_date: String,
        val thumbnail_src: String,
        val views: Int,
        val uploader: Uploader
)
{

    fun getDurationAsSeconds(): Long
    {
        val parts = duration.split(Regex(":")).map { it.toInt() }

        var seconds = 0L
        when(parts.size) {
            // MINUTES : SECONDS
            2 ->
            {
                seconds += (parts[0]*60)
                seconds += parts[1]
            }
            // HOURS : MINUTES : SECONDS
            3 ->
            {
                seconds += (parts[0]*60*60)
                seconds += (parts[1]*60)
                seconds += parts[2]
            }
            else ->
                throw IllegalStateException("Duration format is invalid")
        }
        return seconds
    }

    data class Uploader private constructor(
            val username: String,
            val url: String,
            val verified: Boolean
    )
    {
        companion object
        {
            fun fromJSON(json: JSONObject): Uploader = Uploader(
                    username = json.getString("username"),
                    url = json.getString("url"),
                    verified = json.getBoolean("verified")
            )
        }
    }

    companion object
    {
        fun fromJSON(json: JSONObject): YoutubeVideoJSON
        {
            val video = json.getJSONObject("video")
            return YoutubeVideoJSON(
                    id = video.getString("id"),
                    title = video.getString("title"),
                    url = video.getString("url"),
                    duration = video.getString("duration"),
                    snippet = video.getString("snippet"),
                    upload_date = video.getString("upload_date"),
                    thumbnail_src = video.getString("thumbnail_src"),
                    views = video.getString("views").replace(Regex("\\D+"), "").toIntOrNull() ?: 0,
                    uploader = Uploader.fromJSON(json.getJSONObject("uploader"))
            )
        }
    }
}
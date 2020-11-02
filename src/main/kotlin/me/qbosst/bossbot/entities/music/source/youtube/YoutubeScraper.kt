package me.qbosst.bossbot.entities.music.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import org.apache.http.client.methods.HttpGet
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class YoutubeScraper(val http: HttpInterface)
{
    fun scrape(query: String, page: Int? = null): JSONArray
    {

        val url = "http://youtube-scrape.herokuapp.com/api/search?q=${URLEncoder.encode(query, "UTF-8")}${if(page != null) "&page=${page}" else ""}"

        val request = HttpGet(url)
        val response = http.execute(request)

        val json = JSONObject(response.entity.content.readBytes().decodeToString())
        val results = json.getJSONArray("results")

        return results
    }

    fun scrapeVideos(query: String, page: Int? = null): List<YoutubeVideoJSON>
    {
        return scrape(query, page)
                .filter { (it as JSONObject).has("video") }
                .map { YoutubeVideoJSON.fromJSON(it as JSONObject) }
    }
}
package me.qbosst.bossbot.config

import net.dv8tion.jda.api.utils.data.DataObject

object BotConfig: JSONConfig(
        "./config.json",
        kotlin.run {
            val data = DataObject.empty()
            for(value in enumValues<Values>())
                data.put(value.key, value.default)
            data
        })
{

    val discord_token: String
        get() = data.getString(Values.DISCORD_TOKEN.key, Values.DISCORD_TOKEN.default as String)

    val default_prefix: String
        get() = data.getString(Values.DEFAULT_PREFIX.key, Values.DEFAULT_PREFIX.default as String)

    val database_url: String
        get() = data.getString(Values.DATABASE_URL.key, Values.DATABASE_URL.default as String)

    val database_user: String
        get() = data.getString(Values.DATABASE_USER.key, Values.DATABASE_USER.default as String)

    val database_password: String
        get() = data.getString(Values.DATABASE_PASSWORD.key, Values.DATABASE_PASSWORD.default as String)

    val default_cache_size: Int
        get() = data.getInt(Values.DEFAULT_CACHE_SIZE.key, Values.DEFAULT_CACHE_SIZE.default as Int)

    val developer_id: Long
        get() = 332947254602235914

    val deepai_token: String
        get() = data.getString(Values.DEEPAI_TOKEN.key, Values.DEEPAI_TOKEN.default as String)

    val spotify_client_id: String
        get() = data.getString(Values.SPOTIFY_CLIENT_ID.key, Values.SPOTIFY_CLIENT_ID.default as String)

    val spotify_client_secret: String
        get() = data.getString(Values.SPOTIFY_CLIENT_SECRET.key, Values.SPOTIFY_CLIENT_SECRET.default as String)

    fun reload()
    {
        read()
    }


    private enum class Values(val default: Any)
    {
        DISCORD_TOKEN("token-here"),
        DEEPAI_TOKEN(""),
        SPOTIFY_CLIENT_ID(""),
        SPOTIFY_CLIENT_SECRET(""),

        DEFAULT_PREFIX("!"),

        DATABASE_URL(""),
        DATABASE_USER(""),
        DATABASE_PASSWORD(""),

        DEFAULT_CACHE_SIZE(500),

        ;

        val key: String
            get() = name.toLowerCase()
    }

}
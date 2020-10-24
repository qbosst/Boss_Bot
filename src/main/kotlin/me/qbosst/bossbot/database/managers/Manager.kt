package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.util.FixedCache

abstract class Manager<K, V>
{
    private val cache = FixedCache<K ,V>(BotConfig.default_cache_size)

    protected abstract fun getDatabase(key: K): V

    fun getCached(key: K): V? = cache.get(key)

    fun get(key: K): V = getCached(key) ?: kotlin.run {
        val value = getDatabase(key)
        cache.put(key, value)
        return@run value
    }

    protected fun pull(key: K): V? = cache.pull(key)

    fun isCached(key: K): Boolean = cache.contains(key)

    open fun onReady() {}
}
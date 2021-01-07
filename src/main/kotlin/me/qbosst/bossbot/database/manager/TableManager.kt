package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.BossBot
import me.qbosst.jda.ext.util.FixedCache

abstract class TableManager<K, V>(cacheSize: Int = DEFAULT_CACHE_SIZE) {

    private val cache = FixedCache<K, V>(cacheSize)

    abstract fun retrieve(key: K): V

    abstract fun delete(key: K)

    fun get(key: K) = cache[key]

    fun getOrRetrieve(key: K): V = get(key) ?: retrieve(key)
        .also { value -> value?.let { cache[key] = value } }

    fun pull(key: K): V? = cache.pull(key)

    operator fun contains(key: K) = cache.contains(key)

    operator fun set(key: K, value: V): V? = cache.put(key, value)

    companion object {
        private val DEFAULT_CACHE_SIZE = BossBot.config.cacheSize
    }
}
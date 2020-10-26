package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.util.FixedCache

abstract class Manager<K, V>
{
    private val cache = FixedCache<K ,V>(BotConfig.default_cache_size)

    /**
     *  Method used to get a value straight from the database
     *
     *  @param key The key to get the value with
     *
     *  @return The value returned from the database
     */
    protected abstract fun getDatabase(key: K): V

    /**
     *  Method used to get a value from the cache
     *
     *  @param key The key to get the value with
     *
     *  @return The value in the cache. Null if the value is not cached.
     */
    fun getCached(key: K): V? = cache.get(key)

    /**
     *  Method used to get a value
     *
     *  @param key The key to get the value with
     *
     *  @return The value
     */
    fun get(key: K): V
    {
        // Tries to get the cached value first, if not make a database call
        return getCached(key) ?: kotlin.run {
            val value = getDatabase(key)
            cache.put(key, value)
            return@run value
        }
    }

    /**
     *  Removes a value from the cache
     *
     *  @param key The key that represents the value
     *
     *  @return The value removed. Null if no value was removed
     */
    protected fun pull(key: K): V? = cache.pull(key)

    /**
     *  Determines if a value is cached
     *
     *  @param key The key to check
     *
     *  @return Boolean value. True if value is cached. False if not.
     */
    fun isCached(key: K): Boolean = cache.contains(key)

    /**
     *  Puts a value in the cache
     *
     *  @param key The key to put in the cache
     *  @param value The value to put in the cache
     *
     *  @return The old value that has been removed to make space for the new value. Null if no previous value existed with the same key.
     */
    fun putCache(key: K, value: V): V? = cache.put(key, value)

    /**
     *  Method run when a database is ready (tables and columns created)
     */
    open fun onReady() {}
}
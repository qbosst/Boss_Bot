package me.qbosst.bossbot.util

/**
 *  I saw this fixed cache class and decided to use it. The class has been slightly modified so not everything is
 *  written by the original author but the idea and main structure still came from him
 *
 *  @author John Grosh (john.a.grosh@gmail.com)
 */
class FixedCache<K, V> {
    private val map: MutableMap<K, V>
    private val keys: Array<K>
    private var currentIndex: Int = 0

    /**
     *  This will create a new fixed cache object
     *
     *  @param size The maximum amount of values this cache can hold
     */
    constructor(size: Int)
    {
        // Checks if the cache size is less than 1, if so throw an error
        if(size < 1)
            throw IllegalArgumentException("Cache size must at least be 1!")

        map = mutableMapOf()
        @Suppress("UNCHECKED_CAST")
        keys = arrayOfNulls<Any>(size) as Array<K>
    }

    /**
     *  This will create a new fixed cache object
     *
     *  @param size The maximum amount of values this cache can hold
     *  @param cache Fixed cache object to copy all of the values to the new one
     */
    constructor(size: Int, cache: FixedCache<K, V>)
    {
        // Checks if the cache size is less than 1, if so throw an error
        if(size < 1)
            throw IllegalArgumentException("Cache size must at least be 1!")

        // Checks if the old cache object size is bigger than the new one (this one), if so throw an error
        if(cache.keys.size >= size)
            throw IllegalArgumentException("New cache size should be bigger than the last one!")

        map = cache.map
        @Suppress("UNCHECKED_CAST")
        keys = arrayOfNulls<Any>(size) as Array<K>

        // Puts all the old values from the old cache object into the new one
        for(key in cache.keys.withIndex())
            keys[key.index] = key.value

        currentIndex = (currentIndex + 1) % keys.size
    }

    /**
     *  Puts a new key & value into the cache
     *
     *  @param key The key to put into the cache
     *  @param value The value of that key
     *  @param consumer The old key&value that was removed in order to make space for this new one
     *  @return Old value of the key (if any otherwise will return null)
     */
    fun put(key: K, value: V, consumer: (K, V) -> Unit): V?
    {
        if(map.containsKey(key))
            return map.put(key, value)

        val current = keys[currentIndex]
        val pair = kotlin.run {
            if(current != null) {
                val old = map.remove(current) ?: return@run null
                Pair(current, old)
            }
            else null
        }

        keys[currentIndex] = key
        currentIndex = (currentIndex + 1) % keys.size

        val old = map.put(key, value)
        if(pair != null)
            consumer.invoke(pair.first, pair.second)
        return old
    }

    /**
     *  Puts a new key & value into the cache, without the end user having to worry about what happens with the old key & value removed
     *
     *  @param key The key to put into the cache
     *  @param value The value of that key
     *  @return Old value of the key (if any otherwise will return null)
     */
    fun put(key: K, value: V): V?
    {
        if(map.containsKey(key))
            return map.put(key, value)

        if(keys[currentIndex] != null)
            map.remove(keys[currentIndex])

        keys[currentIndex] = key
        currentIndex = (currentIndex + 1) % keys.size
        return map.put(key, value)
    }

    /**
     *  Removes a key&value from the cache
     *
     *  @param key The key of the pair to remove
     *  @return The value of the key removed (if any otherwise will return null)
     */
    fun pull(key: K): V? = map.remove(key)

    /**
     *  Gets a value from the key
     *
     *  @param key The key of the pair to get
     *  @return The value of the key (if any otherwise will return null)
     */
    fun get(key: K): V? = map[key]

    /**
     *  Checks if a key is in the cache
     *
     *  @param key The key to check of whether it's in the cache or not
     *  @return A true or false boolean based on whether this key is in the cache or not
     *
     */
    fun contains(key: K): Boolean = map.containsKey(key)

    /**
     *  Returns a collection of all the values in the cache
     *
     *  @return All the values in the cache
     */
    fun values(): Collection<V> = map.values

    /**
     *  Returns a set of all the keys in the cache
     *
     *  @return A set of all the keys in the cache
     */
    fun ketSet(): Set<K?> = keys.toSet()

    /**
     *  Returns the amount of maximum allowed key&value in the cache
     *
     *  @return The amount of maximum allowed key&value in the cache
     */
    fun size(): Int = keys.size
}
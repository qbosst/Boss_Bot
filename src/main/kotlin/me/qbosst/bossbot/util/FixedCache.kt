package me.qbosst.bossbot.util

/**
 * A collection that holds a limited amount of pairs of objects (keys and values).
 * Map keys are unique; the map holds only one value for each key.
 *
 * This class has been slightly modified so not everything is written by the original author but the idea and
 * main structure is from him.
 *
 * @param size The size of the cache
 * @param K the type of cache keys. The map is invariant in its key type, as it
 *          can accept key as a parameter (of [containsKey] for example) and return it in [keys] set.
 * @param V the type of cache values. The map is covariant in its value type.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class FixedCache<K, V>(size: Int)
{
    private val map: MutableMap<K ,V>
    private val keys: Array<K>

    init {
        // Checks if cache size is less than 1, if so throw an error since we cannot cache 0 or less values
        if(size < 1)
            throw IllegalArgumentException("Cache size must at least be 1!")

        @Suppress("UNCHECKED_CAST")
        keys = arrayOfNulls<Any>(size) as Array<K>
        map = mutableMapOf()
    }

    private var currentIndex: Int = 0

    /**
     * Returns a read-only [Set] of all keys in this cache.
     */
    val values
        get() = map.values

    val keySet
        get() = map.keys

    /**
     * Returns the number of key/value pairs in the map.
     */
    val size
        get() = keys.size

    /**
     * Creates a new cache and transfers over all the values from the old cache to the new cache.
     *
     * @param cache The cache to transfer all the data from
     */
    constructor(size: Int, cache: FixedCache<K, V>): this(size) {
        if(cache.size >= size)
            throw IllegalArgumentException("New cache size cannot be smaller or equal to the old cache!")

        map.putAll(cache.map)
        cache.keys.withIndex().forEach {
            keys[it.index] = it.value
        }

        currentIndex = (currentIndex + 1) % this.size
    }

    /**
     * Associates the specified [value] with the specified [key] in the cache.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the cache.
     */
    fun put(key: K, value: V): V?
    {
        if(map.containsKey(key))
            return map.put(key, value)

        if(keys[currentIndex] != null)
            map.remove(keys[currentIndex])

        keys[currentIndex] = key
        currentIndex = (currentIndex + 1) % this.size
        return map.put(key, value)
    }

    /**
     * Associates the specified [value] with the specified [key] in the cache.
     *
     * @param consumer The [value] and [key] removed from the cache to make space for the new [value] and [key] provided
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the cache.
     */
    fun put(key: K, value: V, consumer: (K, V) -> Unit): V?
    {
        if(map.containsKey(key))
            return map.put(key, value)

        val current = keys[currentIndex]
        val (k, v) = Pair(current, map.remove(current))

        keys[currentIndex] = key
        currentIndex = (currentIndex + 1) % keys.size

        val old = map.put(key, value)

        if(k != null && v != null)
            consumer.invoke(k, v)

        return old
    }

    /**
     * Removes the specified key and its corresponding value from this cache.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the cache.
     */
    fun pull(key: K): V? = map.remove(key)

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the cache.
     */
    operator fun get(key: K): V? = map[key]

    /**
     * Returns `true` if the cache contains the specified [key].
     */
    operator fun contains(key: K): Boolean = map.containsKey(key)

    /**
     * Associates the specified [value] with the specified [key] in the cache.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the cache.
     */
    operator fun set(key: K, value: V) = put(key, value)


}
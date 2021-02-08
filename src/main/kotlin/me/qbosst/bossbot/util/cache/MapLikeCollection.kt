package me.qbosst.bossbot.util.cache

import dev.kord.cache.map.MapLikeCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class MapLikeCollection<K, V>(private val map: MutableMap<K, V>): MapLikeCollection<K, V> {
    override suspend fun get(key: K): V? = map[key]

    override suspend fun contains(key: K): Boolean = map.contains(key)

    override suspend fun put(key: K, value: V) {
        map[key] = value
    }

    override fun values(): Flow<V> = flow {
        map.values.toList().forEach { emit(it) }
    }

    override suspend fun clear() = map.clear()

    override suspend fun remove(key: K) {
        map.remove(key)
    }

    override fun getByKey(predicate: suspend (K) -> Boolean): Flow<V> = flow {
        for ((key, value) in map.entries.toList()) {
            if (predicate(key)) {
                emit(value)
            }
        }
    }

    override fun getByValue(predicate: suspend (V) -> Boolean): Flow<V> = flow {
        for (value in map.values.toList()) {
            if (predicate(value)) {
                emit(value)
            }
        }
    }
}
package me.qbosst.bossbot.util.cache

import java.util.*

open class FixedCache<K, V>(val maxSize: Int): LinkedHashMap<K, V>(maxSize, 1.0F, true), MutableMap<K, V> {

    final override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return (size > maxSize).also {
            if(it && eldest != null) {
                onRemoveEldestEntry(eldest)
            }
        }
    }

    open fun onRemoveEldestEntry(entry: MutableMap.MutableEntry<K, V>) {}

    companion object {
        /**
         * DSL method that allows for overriding [onRemoveEldestEntry]
         */
        operator fun <K, V> invoke(size: Int, init: (entry: MutableMap.MutableEntry<K, V>) -> Unit): FixedCache<K, V> =
            object: FixedCache<K, V>(size) {
                override fun onRemoveEldestEntry(entry: MutableMap.MutableEntry<K, V>) {
                    init.invoke(entry)
                }
            }
    }
}
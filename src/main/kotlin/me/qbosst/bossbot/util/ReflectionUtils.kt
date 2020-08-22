package me.qbosst.bossbot.util

import org.reflections.Reflections
import java.lang.reflect.Modifier

fun <T> loadClasses(path: String, clazz: Class<out T>) : List<T>
{
    return Reflections(path).getSubTypesOf(clazz)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .sortedBy { it.simpleName }
        .mapNotNull { it.getDeclaredField("INSTANCE").get(null) }
        .toList() as List<T>
}
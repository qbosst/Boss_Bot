package me.qbosst.bossbot.util

import me.qbosst.bossbot.bot.BossBot
import org.reflections.Reflections
import java.lang.reflect.Modifier

/**
 *  @author Din0s
 */
fun <T> loadObjects(path: String, clazz: Class<out T>) : List<T>
{
    BossBot.LOG.debug("Scanning path: $path")
    return Reflections(path).getSubTypesOf(clazz)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .sortedBy { it.simpleName }
        .mapNotNull { it.getDeclaredField("INSTANCE").get(null) }
        .toList() as List<T>
}
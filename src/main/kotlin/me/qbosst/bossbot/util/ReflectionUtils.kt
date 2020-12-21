package me.qbosst.bossbot.util

import org.reflections.Reflections
import java.lang.reflect.Modifier

/**
 *  @author Din0s
 *
 *  Loads singleton classes from the given path
 *
 *  @param path The path to look for the classes in
 *  @param clazz The type of class to look for
 *
 *  @return List of classes found
 */
@Suppress("UNCHECKED_CAST")
fun <T> loadObjects(path: String, clazz: Class<out T>, log: Boolean = false): List<T> = Reflections(path)
        .getSubTypesOf(clazz)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .mapNotNull { it.getDeclaredField("INSTANCE").get(null) }
        .toList() as List<T>

/**
 *  Loads singleton class or regular class from the given path
 *
 *  @param path The path to look for the classes in
 *  @param clazz The type of class to look for
 *
 *  @return List of classes found
 */
@Suppress("UNCHECKED_CAST")
fun <T> loadObjectOrClass(path: String, clazz: Class<out T>): List<T> = Reflections(path)
        .getSubTypesOf(clazz)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .mapNotNull {
            if(it.declaredFields.map { field -> field.name }.contains("INSTANCE"))
                it.getDeclaredField("INSTANCE").get(null)
            else
                it.getDeclaredConstructor().newInstance()
        }
        .toList() as List<T>
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
fun <T> loadObjects(path: String, clazz: Class<out T>) : List<T>
{
    @Suppress("UNCHECKED_CAST")
    val result =  Reflections(path).getSubTypesOf(clazz)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .mapNotNull { it.getDeclaredField("INSTANCE").get(null) }
        .toList() as List<T>
    logPostScan(path, clazz, result)
    return result
}

/**
 *  Loads classes from the given path
 *
 *  @param path The path to look for the classes in
 *  @param clazz The type of class to look for
 *  @param parameterTypes Parameter types that may be needed to create the instance
 *  @param initargs Init Args that may be needed to create the instance
 *
 *  @return List of classes found
 */
fun <T> loadClasses(path: String, clazz: Class<out T>, parameterTypes: Array<Class<*>> = arrayOf(), vararg initargs: Array<Any> = arrayOf()): List<T>
{
    val result = Reflections(path)
            .getSubTypesOf(clazz)
            // Filters the abstract classes out
            .filter { !Modifier.isAbstract(it.modifiers) }
            // Creates an instance
            .mapNotNull { it.getDeclaredConstructor(*parameterTypes).newInstance(*initargs) }
    logPostScan(path, clazz, result)
    return result
}

/**
 *  Loads singleton class or regular class from the given path
 *
 *  @param path The path to look for the classes in
 *  @param clazz The type of class to look for
 *
 *  @return List of classes found
 */
fun <T> loadObjectOrClass(path: String, clazz: Class<out T>): List<T>
{
    @Suppress("UNCHECKED_CAST")
    val result = Reflections(path)
            .getSubTypesOf(clazz)
            // Filters the abstract classes out
            .filter { !Modifier.isAbstract(it.modifiers) }
            // Creates an instance
            .mapNotNull {
                if((it as Class<Any>).kotlin.objectInstance == null)
                    it.getDeclaredConstructor().newInstance()
                else
                    it.getDeclaredField("INSTANCE").get(null)
            }
            .toList() as List<T>
    logPostScan(path, clazz, result)
    return result
}

private fun logPostScan(path: String, clazz: Class<*>, result: List<*>) = Reflections.log.debug("Found ${result.size} classes of type '${clazz}' in path `${path}`: ${result.joinToString(", ") { it?.javaClass?.simpleName ?: it.toString() }}")
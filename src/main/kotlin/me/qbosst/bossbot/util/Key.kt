package me.qbosst.bossbot.util

data class Key private constructor(private val key: String){

    // The name of the key
    val name: String
        get() = key.split(Regex("[|]"))[0]

    // The first id in the key
    val idOne: Long
        get() = key.split(Regex("[|]"))[1].replace(Regex("\\D+"), "").toLong()

    // The second id in the key
    val idTwo: Long
        get() = kotlin.run {
            val array = key.split(Regex("[|]"))
                    .map { it.replace(Regex("\\D+"), "") }
                    .mapNotNull { it.toLongOrNull() }
            if (array.size > 2) array[2] else 0L
        }

    companion object
    {
        /**
         *  Creates a new key object. This is the method used for creating this object as the constructor for it
         *  is private. This is to make sure that only valid keys are being created
         *
         *  @param type The type of key that should be created. The type will contain the format
         *  @param name The name of the key
         *  @param idOne The first id of the key
         *  @param idTwo The second id of the key
         *  @return A new key object created based on all the parameteres given
         */
        fun genKey(type: Type, name: String? = null, idOne: Long, idTwo: Long): Key
        {
            return Key(String.format("%s|${type.format}", name ?: "", idOne, idTwo))
        }

        /**
         *  Returns a key from a string
         *
         *  @param key The string of the possible key
         *  @return A new key object. If the key matches a key format, it will create a new key object otherwise it will return null
         */
        fun fromString(key: String): Key?
        {
            // the max length in characters that a long value can be
            val maxIdLength = Long.MAX_VALUE.toString().length

            // Checks if the string matches a typical key format
            return if (key.matches(Regex(".*[|].:[0-9]{1,$maxIdLength}([|].:[0-9]{1,$maxIdLength})?$"))) {
                Key(key)
            } else null
        }
    }

    override fun toString(): String
    {
        return key
    }

    override fun equals(other: Any?): Boolean
    {
        return if(other is Key) other.key == key else false
    }

    override fun hashCode(): Int
    {
        return key.hashCode()
    }

    enum class Type(val format: String)
    {
        USER("U:%d"),
        CHANNEL("C:%d"),
        USER_CHANNEL("U:%d|G:%d"),
        GUILD("G:%d"),
        USER_GUILD("U:%d|G:%d");

        fun genKey(name: String? = null, idOne: Long, idTwo: Long = 0L): Key
        {
            return genKey(this, name, idOne, idTwo)
        }
    }
}
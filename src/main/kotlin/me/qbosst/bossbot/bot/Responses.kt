package me.qbosst.bossbot.bot

import me.qbosst.bossbot.util.maxLength

fun noMentionedUser(): String = "Please mention a user."

fun userNotFound(arg: String): String = "I could not find anyone with the tag or id of `${arg.maxLength()}`"

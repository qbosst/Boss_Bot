package me.qbosst.bossbot.bot

import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.maxLength

fun userNotMentioned(): String = "Please mention a user."

fun userNotFound(userQuery: String): String = "I could not find anyone with the tag or id of `${userQuery.makeSafe().maxLength()}`."

fun argumentMissing(missingArgument: String, prefix: String = "the"): String = "Please provide $prefix **$missingArgument**"

fun argumentInvalid(invalidArgument: String, typeArgument: String = "argument type"): String = "`${invalidArgument.makeSafe().maxLength()}` is not a valid **$typeArgument**."
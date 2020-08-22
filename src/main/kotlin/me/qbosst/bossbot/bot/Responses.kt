package me.qbosst.bossbot.bot

import me.qbosst.bossbot.util.makeSafe

fun noMentionedUser(): String
{
    return "Please mention a user."
}

fun userNotFound(arg: String): String
{
    return "I could not find anyone with the tag or id of `${arg.makeSafe()}`"
}

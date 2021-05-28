package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.commands.parser.Arguments

fun Arguments.positiveInt(): suspend Argument<*>.(Int) -> Unit = { int ->
    if(int <= 0) {
        throw CommandException("`${displayName}` must be a positive number.")
    }
}
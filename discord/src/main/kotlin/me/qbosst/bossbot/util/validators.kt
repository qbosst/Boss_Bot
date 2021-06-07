package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import dev.kord.core.entity.Member

fun Arguments.positiveInt(): Validator<Int> = { arg, int ->
    if(int <= 0) {
        throw CommandException("`${arg.displayName}` must be a positive number.")
    }
}

fun Arguments.notAuthor(errorMessage: String): Validator<Member?> = { _, member ->
    if(member != null && member.id != getUser()?.id) {
        throw CommandException(errorMessage)
    }
}
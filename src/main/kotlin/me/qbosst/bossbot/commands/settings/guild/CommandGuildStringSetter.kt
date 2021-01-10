package me.qbosst.bossbot.commands.settings.guild

import me.qbosst.bossbot.entities.Context

abstract class CommandGuildStringSetter: CommandGuildSetter<String>() {
    abstract val maxLength: Int

    override fun isValid(ctx: Context, value: String): Boolean {
        val isValidLength = value.length <= maxLength

        if(!isValidLength) {
            onInvalidLength(ctx, value)
        }

        return isValidLength
    }

    open fun onInvalidLength(ctx: Context, value: String) {
        ctx.messageChannel.sendMessage("$value must be less than or equal to $maxLength characters").queue()
    }
}
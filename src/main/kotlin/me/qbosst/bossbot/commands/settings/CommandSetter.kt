package me.qbosst.bossbot.commands.settings

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context

abstract class CommandSetter<K, V>: Command()
{
    @CommandFunction
    fun execute(ctx: Context, @Greedy value: V) {
        // check if value is valid, for example length of string needs to be certain size
        if(!isValid(ctx, value))
            return
        set(ctx, value)
    }

    @CommandFunction(includeUsage = false)
    fun execute(ctx: Context, @Greedy clear: Unit) = set(ctx, null)

    private fun set(ctx: Context, new: V?) {
        val key = getKey(ctx)
        set(key, new)

        onSuccessfulSet(ctx, new)
    }

    protected abstract fun set(key: K, value: V?): V?

    abstract fun get(key: K): V?

    protected abstract fun getKey(ctx: Context): K

    protected open fun onSuccessfulSet(ctx: Context, new: V?) {
        ctx.messageChannel.sendMessage("success").queue()
    }

    /**
     * If a value is not valid, you must display the error response in this method
     */
    open fun isValid(ctx: Context, value: V): Boolean = true
}
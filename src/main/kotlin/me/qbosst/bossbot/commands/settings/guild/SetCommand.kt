package me.qbosst.bossbot.commands.settings.guild

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context

class SetCommand: Command() {
    override val label: String = "set"

    init {
        addChildren(listOf(SetPrefixCommand()))
    }

    @CommandFunction
    fun execute(ctx: Context) {
        TODO()
    }
}
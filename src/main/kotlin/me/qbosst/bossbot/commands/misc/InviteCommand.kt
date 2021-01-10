package me.qbosst.bossbot.commands.misc

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context

class InviteCommand: Command() {
    override val label: String = "invite"
    override val description: String = "Provides the invite link used to invite boss bot"

    @CommandFunction
    fun execute(ctx: Context) {
        val allPermissions = ctx.client.allCommands.asSequence()
            .map { command -> command.botPermissions }
            .flatten()
            .toList()

        ctx.messageChannel.sendMessage(ctx.jda.getInviteUrl(allPermissions)).queue()
    }
}
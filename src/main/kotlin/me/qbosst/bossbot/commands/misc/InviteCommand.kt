package me.qbosst.bossbot.commands.misc

import me.qbosst.bossbot.BossBot
import me.qbosst.bossbot.util.loadObjectOrClass
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.Permission

class InviteCommand: Command() {
    override val label: String = "invite"
    override val description: String = "Provides the invite link used to invite boss bot"

    @CommandFunction
    fun execute(ctx: Context) {
        ctx.messageChannel.sendMessage(ctx.jda.getInviteUrl(allPermissions)).queue()
    }

    companion object {

        private val allPermissions: Collection<Permission> by lazy {
            loadObjectOrClass("${BossBot::class.java.`package`.name}.commands", Command::class.java)
                .map { command -> command.botPermissions }
                .flatten()
        }
    }
}
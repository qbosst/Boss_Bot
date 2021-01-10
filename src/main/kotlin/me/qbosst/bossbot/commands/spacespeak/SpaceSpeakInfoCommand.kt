package me.qbosst.bossbot.commands.spacespeak

import me.qbosst.bossbot.entities.Context
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import net.dv8tion.jda.api.EmbedBuilder

class SpaceSpeakInfoCommand: Command() {
    override val label: String = "info"

    @CommandFunction
    fun execute(ctx: Context, id: Int) {

        val embed = EmbedBuilder()
            .appendDescription("**Message**")
            .appendDescription("\n```custom message here```")
            .addField("Distance Travelled", "1278301923km away", true)
            .addField("Fun Fact", "adjawjndjkawndkl janwdkjanw dfun fact lol", true)
            .addField("Launched At", "198230912 hours ago", true)
            .setFooter("Message Id: $id")
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }
}
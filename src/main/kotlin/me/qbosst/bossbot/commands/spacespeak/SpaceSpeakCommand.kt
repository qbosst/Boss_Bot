package me.qbosst.bossbot.commands.spacespeak

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import java.time.OffsetDateTime

class SpaceSpeakCommand: Command() {
    override val label: String = "spacespeak"
    override val description: String = "Sends a message into space!"

    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_EMBED_LINKS)

    @CommandFunction
    fun execute(ctx: Context, @Greedy message: String) {
        //TODO: send message into space

        val embed = EmbedBuilder()
            .setAuthor(ctx.author.asTag, null, ctx.author.effectiveAvatarUrl)
            .appendDescription("**Your message has been sent into space!**")
            .appendDescription("\nThis message has been sent using [SpaceSpeak](https://www.spacespeak.com)")
            .appendDescription("\n```${message}```")
            .setFooter("Visit spacespeak.com for information on sending photos and audio")
            .setThumbnail(spaceSpeakLogoUrl)
            .setColor(spaceSpeakColour)
            .setTimestamp(OffsetDateTime.now())
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }

    companion object {
        const val spaceSpeakLogoUrl = "https://www.spacespeak.com/images/logo0b.png"
        const val spaceSpeakColour = 0x003f64
    }
}
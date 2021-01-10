package me.qbosst.bossbot.commands.spacespeak

import me.qbosst.bossbot.BossBot
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import me.qbosst.jda.ext.util.maxLength
import me.qbosst.spacespeak.SpaceSpeakAPI
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User

class SpaceSpeakRecentCommand: Command() {
    override val label: String = "recent"
    override val description: String = "Displays recently sent messages into spaces by Boss Bot"

    @CommandFunction
    suspend fun execute(ctx: Context, `page number`: Int = 0, user: User = ctx.author) {

        val messages = BossBot.spaceSpeakApi.getMessages()
        if(messages.isEmpty())
            return

        val embed = EmbedBuilder()
            .setTitle("Recent Messages Launched Into Space")
            .setDescription("These messages have been sent using [spacespeak.com](https://www.spacespeak.com)")
            .setThumbnail(spaceSpeakLogoUrl)
            .setColor(spaceSpeakColour)
            .setFooter("Page 1 / 1")
            .apply {
                val first = messages.first()
                addRecord(ctx.author, first.launchDate, first.messageId, first.messageText, true)

                messages.drop(1).forEach { message ->
                    addRecord(ctx.author, message.launchDate, message.messageId, message.messageText)
                }
            }
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }

    companion object {
        const val spaceSpeakLogoUrl = "https://www.spacespeak.com/images/logo0b.png"
        const val spaceSpeakColour = 0x2ca3fe

        private fun EmbedBuilder.addRecord(
            author: User,
            date: String,
            id: Int,
            message: String,
            isFirst: Boolean = false
        ) {
            addField(
                if(isFirst) "ID. Message" else EmbedBuilder.ZERO_WIDTH_SPACE,
                "`${id}` $message".maxLength(38),
                true
            )

            addField(
                if(isFirst) "Date" else EmbedBuilder.ZERO_WIDTH_SPACE,
                date,
                true
            )

            addField(
                if(isFirst) "Author" else EmbedBuilder.ZERO_WIDTH_SPACE,
                author.asTag,
                true
            )
        }
    }
}
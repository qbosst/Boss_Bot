package qbosst.bossbot.bot.commands.misc

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.util.getUserByString
import qbosst.bossbot.util.makeSafe
import java.time.OffsetDateTime

object AvatarCommand: Command(
        "avatar"
) {

    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        val target: User = if(args.isNotEmpty())
        {
            val arguments = args.joinToString(" ")
            BossBot.shards.getUserByString(arguments) ?: kotlin.run {
                event.channel.sendMessage("I could not find anyone by the id or tag of `${arguments.makeSafe()}`").queue()
                return
            }
        } else event.author

        event.channel.sendMessage(avatarEmbed(target).build()).queue()
    }

    private fun avatarEmbed(user: User): EmbedBuilder
    {
        return EmbedBuilder()
                .setTitle(user.asTag, user.avatarUrl)
                .setImage(user.avatarUrl + "?size=256")
                .setTimestamp(OffsetDateTime.now())
    }

}
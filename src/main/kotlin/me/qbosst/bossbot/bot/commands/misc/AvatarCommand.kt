package me.qbosst.bossbot.bot.commands.misc

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.misc.colour.nextColour
import me.qbosst.bossbot.util.getUserByString
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.random.Random

object AvatarCommand: Command(
        "avatar",
        "Displays a user's avatar",
        aliases = listOf("av"),
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {

        // Gets the user
        val target: User = if(args.isNotEmpty())
        {
            val arguments = args.joinToString(" ")
            BossBot.shards.getUserByString(arguments) ?: kotlin.run {
                event.channel.sendMessage("I could not find anyone by the id or tag of `${arguments.maxLength()}`").queue()
                return
            }
        } else
            event.author

        // Sends the avatar
        event.channel.sendMessage(EmbedBuilder()
                .setDescription("[${target.asTag}](${target.effectiveAvatarUrl})")
                .setImage(target.effectiveAvatarUrl + "?size=256")
                .setColor(if(event.isFromGuild) event.guild.selfMember.color else Random.nextColour())
                .build()).queue()
    }

}
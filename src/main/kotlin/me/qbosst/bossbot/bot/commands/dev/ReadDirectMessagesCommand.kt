package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.bot.userNotMentioned
import me.qbosst.bossbot.util.getUserByString
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ReadDirectMessagesCommand : DeveloperCommand(
        "readdms",
        description = "Shows message history between the bot and a user in direct messages",
        usage = listOf("@user"),
        examples = listOf("@boss"),
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES),
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val string = args.joinToString(" ")
            val user = event.jda.shardManager!!.getUserByString(string) ?: kotlin.run {
                event.channel.sendMessage(userNotFound(string)).queue()
                return
            }
            if(user != event.jda.selfUser)
                user.openPrivateChannel().flatMap { it.iterableHistory }.flatMap { event.channel.sendFile(it.toByteArray(), "${user.asTag} message history.txt") }.queue({},
                        {
                            event.channel.sendMessage("Something has went wrong while trying to obtain direct messages with ${user.asTag}...").queue()
                        })
            else
                event.channel.sendMessage("I cannot check message history with myself!").queue()
        }
        else
            event.channel.sendMessage(userNotMentioned()).queue()
    }

    private fun Collection<Message>.toByteArray(): ByteArray
    {
        val sb = StringBuilder()
        for(message in this)
            sb.append("\nMessage sent by ${message.author.asTag} at ${message.timeCreated}" +
                    " | Message Edited : ${message.isEdited}" +
                    " | Attachments (${message.attachments.size}) : ${message.attachments.joinToString("\n") { attachment ->  attachment.url }}" +
                    "\nContent : ${message.contentRaw}\n----------")
        return sb.toString().toByteArray()
    }
}
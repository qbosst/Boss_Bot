package me.qbosst.bossbot.entities

import me.qbosst.bossbot.listeners.handlers.CommandClient
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.IContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Context(
    override val client: CommandClient,
    override val event: MessageReceivedEvent,
    override val command: Command
): IContext {
    override val author: User = event.author

    override val guild: Guild? = if(event.isFromGuild) event.guild else null

    override val jda: JDA = event.jda

    override val member: Member? = event.member

    override val message: Message = event.message

    override val messageChannel: MessageChannel = event.channel

    override val privateChannel: PrivateChannel? = if(event.isFromType(ChannelType.PRIVATE)) event.privateChannel else null

    override val textChannel: TextChannel? = if(event.isFromType(ChannelType.TEXT)) event.textChannel else null
}
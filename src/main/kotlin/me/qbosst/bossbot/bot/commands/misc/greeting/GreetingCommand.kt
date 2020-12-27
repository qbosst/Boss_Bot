package me.qbosst.bossbot.bot.commands.misc.greeting

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.setters.CommandStringSetter
import me.qbosst.bossbot.database.managers.UserDataManager
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.database.tables.UserDataTable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object GreetingCommand: CommandStringSetter<User>(
        "greeting",
        description = "Shows or sets your custom greeting message",
        usages = listOf("", "<new message>"),
        guildOnly = false,
        children = listOf(GreetingUpdateCommand),
        displayName = "Greeting Message",
        maxLength = UserDataTable.max_greeting_length,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    override fun set(key: User, value: String?): String? = UserDataManager.update(key, UserDataTable.greeting, value)

    override fun get(key: User): String? = key.getUserData().greeting

    override fun getKey(event: MessageReceivedEvent, args: List<String>): User = event.author

    override fun onSuccessfulSet(channel: MessageChannel, old: String?, new: String?) =
            channel.sendMessage("Your greeting message has been updated.").queue()

    override fun onUnsuccessfulSet(channel: MessageChannel, reason: String) = channel.sendMessage(reason).queue()

    override fun onAlreadySet(channel: MessageChannel, value: String?) =
            channel.sendMessage("Your greeting is already set to this.").queue()

    override fun displayCurrent(channel: MessageChannel, key: User, value: String?) =
            channel.sendMessage(value ?: "You do not have a greeting setup.").queue()
}
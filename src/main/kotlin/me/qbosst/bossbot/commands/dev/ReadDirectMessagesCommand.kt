package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.util.withSingleLineCode
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

class ReadDirectMessagesCommand: Command() {

    override val label: String = "readdms"
    override val description: String = "Reads the last 100 messages from DMs with a user"
    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_ATTACH_FILES)
    override val developerOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context, user: User) {

        // we cannot read dms with ourselves
        if(user != ctx.jda.selfUser) {

            user.openPrivateChannel()
                    // get messages
                .flatMap { it.iterableHistory }
                .flatMap { ctx.messageChannel.sendFile(it.toByteArray(), "${user.asTag} message history.txt") }
                .submit().whenComplete { _, error ->
                    // if we received an error
                    if(error != null) {
                        ctx.messageChannel.sendMessage("Something has went wrong... ${error.localizedMessage
                                .withSingleLineCode()}").queue()
                    }
                }
        } else {
            ctx.messageChannel.sendMessage("I cannot check DMs with myself!").queue()
        }
    }

    companion object {

        /**
         * Converts a collection of messages into text representing the collection.
         */
        private fun Collection<Message>.toByteArray(): ByteArray = joinToString("\n") { message ->
            "\nMessage sent by ${message.author.asTag} at ${message.timeCreated} " +
                    "| Message Edited: ${message.isEdited} " +
                    "| Attachments (${message.attachments.size}): ${message.attachments.joinToString("\n") { it.url }}" +
                    "\nContent: ${message.contentRaw}\n---------"
        }.toByteArray()

    }

}
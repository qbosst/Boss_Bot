package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.long
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import me.qbosst.bossbot.converters.coalescedMaxLengthString
import me.qbosst.bossbot.database.models.getOrRetrieveUserData
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.util.embed.MenuEmbed
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.spacespeak.SpaceSpeakAPI
import me.qbosst.spacespeak.functions.Product
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class SpaceSpeakExtension(
    bot: ExtensibleBot,
    token: String,
    private val emailAddress: String,
    private val username: String
): Extension(bot) {
    override val name: String = "spacespeak"

    private val api = SpaceSpeakAPI(token)
    private lateinit var products: List<Product>
    private lateinit var messages: MutableList<SpaceSpeakMessage>

    class SpaceSpeakSendArgs: Arguments() {
        // set a minimum length needed to send a message to space, to prevent typos
        val message by coalescedMaxLengthString("message", "The message to send to space", maxLength = 500, minLength = 8, shouldThrow = true)
    }

    class SpaceSpeakInfoArgs: Arguments() {
        val id by long("id", "The id of the message to view info about")
    }

    class SpaceSpeakRecentArgs: Arguments() {
        val num by defaultingInt("page number", "", 0)
    }

    override suspend fun setup() {
        val scope = bot.kord

        val productsJob = scope.async { api.getProducts() }

        // retrieve data that linked a SpaceSpeak message id to a discord user id.
        val links = transaction {
            SpaceSpeakTable.selectAll().asSequence()
                .map { row -> row[SpaceSpeakTable.messageId] to row[SpaceSpeakTable.userId] }
                .toMap()
        }

        val messagesJob = scope.async {
            api.getMessages().asSequence()
                .map { message ->
                    SpaceSpeakMessage(
                        api = api,
                        messageId = message.messageId,
                        userId = links[message.messageId],
                        content = message.messageText,
                        launchDate = Instant.parse(message.launchDate + 'Z')
                    )
                }
                .sortedBy { message -> message.messageId }
                .toMutableList()
        }

        group {
            name = "spacespeak"

            action {
                message.replySpaceSpeakEmbed {
                    description = buildString {
                        append("[SpaceSpeak](https://www.spacespeak.com) is a service that allows you to send messages into space!")
                        //TODO: add more
                    }
                }
            }

            command(::SpaceSpeakSendArgs) {
                name = "send"
                description = "Sends a message into space!"

                action {
                    // send message into space
                    val result = api.sendMessage(
                        productId = products.first().productId,
                        email = emailAddress,
                        username = username,
                        message = arguments.message
                    )

                    val userId = message.data.authorId.value

                    // associate space speak message id to discord user id
                    transaction {
                        SpaceSpeakTable.insert {
                            it[SpaceSpeakTable.messageId] = result.messageId
                            it[SpaceSpeakTable.userId] = userId
                        }
                    }

                    // create space speak message
                    val spaceSpeakMessage = SpaceSpeakMessage(
                        api = api,
                        messageId = result.messageId,
                        userId = userId,
                        content = arguments.message,
                        launchDate = Instant.now()
                    )

                    // add message to list of sent space speak messages
                    messages.add(spaceSpeakMessage)

                    // reply success message to user
                    message.reply(false) {
                        embed {
                            author {
                                this.name = user!!.tag
                                this.icon = user!!.avatar.url
                            }

                            description = buildString {
                                append("**Your message has been sent into space!**")
                                append("\nSending images and audio is available at [spacespeak.com](https://www.spacespeak.com)")
                                append("\n`${arguments.message}`")
                            }

                            color = spaceSpeakColour
                            footer {
                                text = "Reach out to the Universe!"
                            }
                            timestamp = Instant.now()
                            thumbnail {
                                url = spaceSpeakLogoUrl
                            }
                        }
                    }
                }
            }

            command(::SpaceSpeakInfoArgs) {
                name = "info"
                description = "Gives more details about a particular message sent into space"

                aliases = arrayOf("view")

                action {
                    val spaceSpeakMessage: SpaceSpeakMessage? = messages.firstOrNull { message -> message.messageId == arguments.id }
                    when {
                        spaceSpeakMessage == null -> {
                            message.reply(false) {
                                content = "Could not find SpaceSpeak message with id: ${arguments.id}"
                            }
                        }
                        !spaceSpeakMessage.isPublic() -> {
                            message.reply(false) {
                                content = "This message is not public!"
                            }
                        }
                        else -> {
                            // request information about the message from SpaceSpeak
                            val distanceTravelled = scope.async { spaceSpeakMessage.getDistanceTravelled() }
                            val randomFact = scope.async { spaceSpeakMessage.getRandomSpaceFact() }

                            // wait for responses
                            awaitAll(distanceTravelled, randomFact)

                            // user who sent the message into space
                            val user = spaceSpeakMessage.getUser(event.kord)

                            message.replySpaceSpeakEmbed {
                                description = buildString {
                                    append("**Message**")
                                    append("\n${spaceSpeakMessage.content}")
                                }

                                field("Distance Travelled", true) { distanceTravelled.getCompleted() }
                                field("Fun Fact", true) { randomFact.getCompleted() }
                                field("User", true) {
                                    if(spaceSpeakMessage.isAnonymous()) {
                                        "Anonymous"
                                    } else {
                                        user?.tag ?: "N/A"
                                    }
                                }

                                footer {
                                    text = "Message ID: ${arguments.id} | Launch Date/Time"
                                }
                                timestamp = spaceSpeakMessage.launchDate
                            }
                        }
                    }
                }
            }

            command(::SpaceSpeakRecentArgs) {
                name = "recent"
                description = "Views the messages that have been sent into space using Boss Bot"

                aliases = arrayOf("messages")

                action {
                    val zoneId: ZoneId = user!!.getOrRetrieveUserData()?.zoneId?.let { ZoneId.of(it) } ?: ZoneId.of("UTC")

                    val builder = MenuEmbed<SpaceSpeakMessage>(3, messages.filter { it.isPublic() }.reversed()) { message, isFirst ->
                        field {
                            if(isFirst) {
                                this.name = "Message ID"
                            }
                            inline = true
                            value = "`${message.messageId}`"
                        }
                        field {
                            if(isFirst) {
                                this.name = "Launch Date"
                            }
                            inline = true
                            value = buildString {

                                append(message.launchDate.atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yy")))
                                val useDaylightSavings = zoneId.rules.isDaylightSavings(Instant.now())
                                append(" ${TimeZone.getTimeZone(zoneId).getDisplayName(useDaylightSavings, 0)}")
                            }
                        }
                        field {
                            if(isFirst) {
                                this.name = "Content"
                            }
                            inline = true
                            value = message.content
                        }
                    }
                    message.replySpaceSpeakEmbed {
                        builder.buildPage(embed = this, page = arguments.num)
                    }
                }
            }
        }

        // wait for calls to be finished before moving on, and set variables
        awaitAll(productsJob, messagesJob)

        this.messages = messagesJob.getCompleted()
        this.products = productsJob.getCompleted()
    }

    private suspend fun MessageBehavior.replySpaceSpeakEmbed(builder: EmbedBuilder.() -> Unit) = reply {
        embed {
            this.apply(builder)

            thumbnail {
                url = spaceSpeakLogoUrl
            }
            color = spaceSpeakColour
        }
        allowedMentions {
            repliedUser = false
        }
    }

    data class SpaceSpeakMessage(
        val api: SpaceSpeakAPI,
        val messageId: Long,
        val userId: Long?,
        val content: String,
        val launchDate: Instant
    ) {
        suspend fun getUser(kord: Kord): User? = userId?.let { id -> kord.getUser(Snowflake(id)) }

        suspend fun getDistanceTravelled(): String = api.getDistanceTraveled(messageId).removeSurrounding("\"")

        suspend fun getRandomSpaceFact(): String {
            var response = api.getRandomSpaceFact(messageId).removeSurrounding("\"").removePrefix("<br />")

            // html tag regex
            val pattern = "<a.*?href=\\\\?\"(.*?)\".*?>(.*?)<\\/a>".toPattern()

            // convert html to markdown
            var matcher = pattern.matcher(response)
            while(matcher.find()) {
                val link = matcher.group(1).replace("\\", "")
                val name = matcher.group(2)

                response = response.replaceRange(matcher.start(), matcher.end(), "[$name]($link)")
                matcher = pattern.matcher(response)
            }

            return response
        }

        fun isAnonymous(): Boolean {
            if(userId == null) {
                return false
            }

            return transaction {
                SpaceSpeakTable
                    .select { SpaceSpeakTable.userId.eq(userId) and SpaceSpeakTable.messageId.eq(messageId) }
                    .singleOrNull()
                    ?.get(SpaceSpeakTable.isAnonymous)
                    ?: false
            }
        }

        fun isPublic(): Boolean {
            if(userId == null) {
                return true
            }
            return transaction {
                SpaceSpeakTable
                    .select { SpaceSpeakTable.userId.eq(userId) and SpaceSpeakTable.messageId.eq(messageId) }
                    .singleOrNull()
                    ?.get(SpaceSpeakTable.isPublic)
                    ?: true
            }
        }
    }

    companion object {
        const val spaceSpeakLogoUrl = "https://www.spacespeak.com/images/logo0b.png"
        val spaceSpeakColour = Color(0x2ca3fe)
    }
}
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
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import me.qbosst.bossbot.converters.coalescedMaxLengthString
import me.qbosst.bossbot.database.models.getOrRetrieveData
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.embed.MenuEmbed
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.spacespeak.SpaceSpeakAPI
import me.qbosst.spacespeak.functions.Product
import org.jetbrains.exposed.sql.insert
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
        this.products = api.getProducts()

        // retrieve data that linked a SpaceSpeak message id to a discord user id.
        val links = transaction {
            SpaceSpeakTable.selectAll().asSequence()
                .map { row -> row[SpaceSpeakTable.messageId] to row[SpaceSpeakTable.userId] }
                .toMap()
        }

        this.messages = api.getMessages().asSequence()
            .map { message -> SpaceSpeakMessage(
                api = api,
                messageId = message.messageId,
                userId = links[message.messageId],
                content = message.messageText,
                launchDate = Instant.parse(message.launchDate+'Z')
            ) }
            .sortedBy { message -> message.messageId }
            .toMutableList()


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
                    if(spaceSpeakMessage == null) {
                        message.reply(false) {
                            content = "Could not find SpaceSpeak message with id: ${arguments.id}"
                        }
                    } else {

                        // request information here, as the embed builder scope does not suspend
                        val scope = event.kord

                        // request information about the message from SpaceSpeak
                        val distanceTravelled = scope.async { spaceSpeakMessage.getDistanceTravelled() }
                        val randomFact = scope.async { spaceSpeakMessage.getRandomSpaceFact() }

                        // wait for responses
                        awaitAll(distanceTravelled, randomFact)

                        message.replySpaceSpeakEmbed {
                            description = buildString {
                                append("**Message**")
                                append("\n${spaceSpeakMessage.content}")
                            }

                            field("Distance Travelled", true) { distanceTravelled.getCompleted() }
                            field("Fun Fact", true) { randomFact.getCompleted() }
                            field("Launched At", true) { spaceSpeakMessage.launchDate.toString() }

                            footer {
                                text = "Message ID: ${arguments.id}"
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
                    val zoneId = user!!.getOrRetrieveData().zoneId ?: ZoneId.of("UTC")

                    val builder = MenuEmbed<SpaceSpeakMessage>(3, messages.reversed()) { message, isFirst ->
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

        suspend fun getDistanceTravelled() = api.getDistanceTraveled(messageId).removeSurrounding("\"")

        suspend fun getRandomSpaceFact() = api.getRandomSpaceFact(messageId).removeSurrounding("\"")

    }

    companion object {
        const val spaceSpeakLogoUrl = "https://www.spacespeak.com/images/logo0b.png"
        val spaceSpeakColour = Color(0x2ca3fe)
    }
}
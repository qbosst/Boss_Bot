package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.long
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import me.qbosst.bossbot.commands.converters.SingleToCoalescingConverter
import me.qbosst.bossbot.commands.converters.impl.MaxStringConverter
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.dao.SpaceSpeakMessage
import me.qbosst.bossbot.database.dao.UserData
import me.qbosst.bossbot.database.dao.getUserData
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.util.Colour
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.snowflake
import me.qbosst.bossbot.util.ext.wrap
import me.qbosst.bossbot.util.kColour
import me.qbosst.spacespeak.SpaceSpeakAPI
import me.qbosst.spacespeak.functions.GetMessage
import me.qbosst.spacespeak.functions.Product
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private suspend fun MessageBehavior.replySpaceSpeakEmbed(builder: EmbedBuilder.() -> Unit): Message {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return channel.createMessage {
        messageReference = this@replySpaceSpeakEmbed.id
        allowedMentions {
            repliedUser = false
        }
        embed {
            builder()

            color = Colour.STEEL_BLUE.kColour
            thumbnail {
                url = "https://www.spacespeak.com/images/logo0b.png"
            }
        }
    }
}

class SpaceSpeakExtension(bot: ExtensibleBot, val config: BotConfig.SpaceSpeak): Extension(bot) {
    override val name: String = "spacespeak"

    private val spaceSpeakAPI = SpaceSpeakAPI(config.token)
    private lateinit var products: List<Product>
    private lateinit var spaceSpeakMessages: ConcurrentHashMap<Long, GetMessage>

    class SpaceSpeakRecentArgs: Arguments() {
        val pageNum by defaultingInt("page number", "", 0)
    }

    class SpaceSpeakInfoArgs: Arguments() {
        val id by long("id", "the id of the message to view info about")
    }

    class SpaceSpeakSendArgs: Arguments() {
        val message by arg("", "", SingleToCoalescingConverter(MaxStringConverter(minLength = 10, maxLength = 512)))
    }

    suspend fun getProducts(): List<Product> = when {
        ::products.isInitialized -> products
        else -> spaceSpeakAPI.getProducts().also { this.products = it }
    }

    override suspend fun setup() {
        val spaceSpeakMessagesJob = bot.kord.async { spaceSpeakAPI.getMessages() }

        group {
            name = "spacespeak"

            action {
                val prefix = bot.settings.messageCommandsBuilder.prefixCallback.invoke(event, bot.settings.messageCommandsBuilder.defaultPrefix)

                message.replySpaceSpeakEmbed {
                    description = buildString {
                        append("[SpaceSpeak](https://www.spacespeak.com) is a service that allows you to **send messages into space**!")
                        append("\nCheck out what else they're up to [here](https://spacespeak.com/NewsAndEvents)!")

                        append("\n\nTo send your message into space, use **${prefix}spacespeak send <message>**")
                    }
                }
            }

            command(::SpaceSpeakSendArgs) {
                name = "send"

                action {
                    channel.withTyping {
                        val product = getProducts().first()

                        // send message into space
                        val response = spaceSpeakAPI.sendMessage(
                            productId = product.productId,
                            email = config.emailAddress,
                            username = config.username,
                            message = arguments.message
                        )

                        // get the message we have just sent into space
                        val sentMessage = spaceSpeakAPI.getMessages(messageId = response.messageId).first()
                        spaceSpeakMessages[sentMessage.messageId] = sentMessage

                        // insert message into database
                        transaction {
                            SpaceSpeakMessage.new(sentMessage.messageId) {
                                userId = message.data.authorId.value
                                isPublic = true
                                isAnonymous = false
                            }
                        }

                        // send success message
                        message.replySpaceSpeakEmbed {
                            description = buildString {
                                append("**Your message has been sent into space!**")
                                append("\nSending images and audio is available at [spacespeak.com](https://www.spacespeak.com)")
                                append("\n`${arguments.message}`")
                            }
                            author {
                                name = user!!.tag
                                icon = user!!.avatar.url
                            }

                            footer {
                                text = "Reach out to the Universe!"
                            }
                            timestamp = Instant.now()
                        }
                    }
                }
            }

            command(::SpaceSpeakInfoArgs) {
                name = "info"

                action {
                    val message = transaction { SpaceSpeakMessage.findById(arguments.id) }
                    when {
                        // no message found
                        message == null -> {
                            event.message.reply(false) {
                                content = "Could not find a message with the id: ${arguments.id}"
                            }
                        }
                        // message is not public
                        !message.isPublic -> {
                            event.message.reply(false) {
                                content = "This message is not public!"
                            }
                        }
                        // display info
                        else -> {
                            // request information about the message
                            val getMessage = message.getMessage
                            val distanceTravelledJob = event.async { getMessage.getDistanceTravelled() }
                            val randomFactJob = event.async { getMessage.getRandomFact() }

                            val user = message.userId?.snowflake()?.let { event.kord.getUser(it) }

                            // wait for responses from requests
                            awaitAll(distanceTravelledJob, randomFactJob)

                            // send info about the message sent into space
                            event.message.replySpaceSpeakEmbed {
                                description = "**Message**\n${getMessage.messageText}"

                                footer {
                                    text = "Message ID: ${arguments.id} | Launch Date"
                                }
                                timestamp = getMessage.launchDateInstant

                                author {
                                    when {
                                        message.isAnonymous -> {
                                            name = "Sent by: Anonymous"
                                        }
                                        user == null -> {
                                            name = "Sent by: N/A"
                                        }
                                        else -> {
                                            name = user.tag
                                            icon = user.avatar.url
                                        }
                                    }
                                }

                                field("Distance Travelled", true) { distanceTravelledJob.getCompleted() }
                                field("Fun Fact", true) { randomFactJob.getCompleted() }
                            }
                        }
                    }
                }
            }

            command(::SpaceSpeakRecentArgs) {
                name = "recent"

                val maxPerPage = 3

                action {

                    val (pgNum: Long, maxPages: Long, messages: List<SpaceSpeakMessage>) = transaction {
                        val count = SpaceSpeakTable.selectAll().count()

                        val maxPages = run {
                            var max = count / maxPerPage
                            if(count % maxPerPage == 0L && max > 0) {
                                max -= 1
                            }
                            return@run max
                        }

                        val pgNum = (arguments.pageNum-1).toLong().coerceIn(0, maxPages)

                        val query = SpaceSpeakTable
                            .select { SpaceSpeakTable.isPublic.eq(true) }
                            .orderBy(SpaceSpeakTable.id, order = SortOrder.DESC)
                            .limit(maxPerPage, offset = (maxPerPage * pgNum))

                        Triple(pgNum, maxPages, SpaceSpeakMessage.wrapRows(query).toList())
                    }

                    fun EmbedBuilder.addRecord(
                        dbData: SpaceSpeakMessage, spaceData: GetMessage,
                        userData: UserData,
                        isFirst: Boolean
                    ) {

                        field(if(isFirst) "Message ID" else EmbedBuilder.ZERO_WIDTH_SPACE, true) {
                            dbData.messageId.toString().wrap("`")
                        }

                        field(if(isFirst) "Launch Date" else EmbedBuilder.ZERO_WIDTH_SPACE, true) {
                            buildString {
                                val zone = userData.zoneId ?: ZoneId.of("UTC")
                                append(spaceData.launchDateInstant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yy")))
                                val userDaylightSavings = zone.rules.isDaylightSavings(Instant.now())
                                append(" ${TimeZone.getTimeZone(zone).getDisplayName(userDaylightSavings, 0)}")
                            }
                        }

                        field(if(isFirst) "Content" else EmbedBuilder.ZERO_WIDTH_SPACE, true) { spaceData.messageText }
                    }


                    val userData = user!!.getUserData()
                    event.message.replySpaceSpeakEmbed {
                        var isFirst = true
                        messages.forEach { message ->
                            addRecord(message, message.getMessage, userData, isFirst)
                            isFirst = false
                        }

                        footer {
                            text = "Page ${pgNum+1} / ${maxPages+1}"
                        }
                    }
                }
            }
        }

        this.spaceSpeakMessages = ConcurrentHashMap(
            spaceSpeakMessagesJob.await()
                .map { message -> message.messageId to message }
                .toMap()
        )

        transaction {
            // turn the list of messages sent into space into a list of message ids
            val sentMessageIds = spaceSpeakMessages.keys

            // get the list of messages that we currently have stored
            val presentMessageIds = SpaceSpeakTable
                .select { SpaceSpeakTable.id.inList(sentMessageIds) }
                .map { row -> row[SpaceSpeakTable.id].value }

            // remove all the messages that we already store
            val missingMessageIds = sentMessageIds.toMutableList()
            missingMessageIds.removeIf { presentMessageIds.contains(it) }

            // create records for the messages we are missing
            SpaceSpeakTable.batchInsert(missingMessageIds) { messageId ->
                this[SpaceSpeakTable.id] = EntityID(messageId, SpaceSpeakTable)
                this[SpaceSpeakTable.userId] = null
                this[SpaceSpeakTable.isPublic] = true
                this[SpaceSpeakTable.isAnonymous] = true
            }
        }
    }

    private val SpaceSpeakMessage.getMessage: GetMessage get() = spaceSpeakMessages[messageId]!!

    private suspend fun GetMessage.getDistanceTravelled(): String = spaceSpeakAPI.getDistanceTraveled(messageId)
        .removeSurrounding("\"")

    private suspend fun GetMessage.getRandomFact(): String {
        var response = spaceSpeakAPI.getRandomSpaceFact(messageId).removeSurrounding("\"").removePrefix("<br />")

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

    val GetMessage.launchDateInstant: Instant get() = Instant.parse(launchDate+'Z')
}
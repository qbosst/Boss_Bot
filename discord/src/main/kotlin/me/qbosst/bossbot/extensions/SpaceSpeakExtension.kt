package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingLong
import com.kotlindiscord.kord.extensions.commands.converters.impl.long
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import me.qbosst.bossbot.commands.hybrid.HybridCommandContext
import me.qbosst.bossbot.database.dao.SpaceSpeakMessage
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.util.STEEL_BLUE
import me.qbosst.bossbot.util.hybridCommand
import me.qbosst.bossbot.util.idLong
import me.qbosst.spacespeak.SpaceSpeakAPI
import me.qbosst.spacespeak.entity.Product
import me.qbosst.spacespeak.functions.GetMessageResponse
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap
import dev.kord.common.Color as Colour

private suspend inline fun HybridCommandContext<out Arguments>.publicSpaceSpeakFollowUp(
    builder: EmbedBuilder.() -> Unit
) = publicFollowUp {
    allowedMentions {}
    embed {
        builder()

        color = Colour.STEEL_BLUE
        thumbnail { url = "https://www.spacespeak.com/images/logo0b.png" }
    }
}

class SpaceSpeakExtension: Extension() {
    override val name: String = "spacespeak"

    private val api = SpaceSpeakAPI(env("spacespeak.token")!!)
    private val emailAddress = env("spacespeak.emailAddress")!!
    private val username = env("spacespeak.username")!!
    private lateinit var products: List<Product>
    private lateinit var messages: ConcurrentHashMap<Long, GetMessageResponse>

    class SpaceSpeakSendArgs: Arguments() {
        val message by coalescedString("message", "The message you want to send into space") { arg, value ->
            when {
                value.length < 10 ->
                    throw CommandException("You are sending a message into space, be more descriptive!")
                value.length > 2000 ->
                    throw CommandException("Messages cannot be above 2000 characters!")
            }
        }
    }

    class SpaceSpeakInfoArgs: Arguments() {
        val id by long("message-id", "The id of the message you want to view info about")
    }

    class SpaceSpeakRecentArgs: Arguments() {
        val pageNumber by defaultingLong("page-number", "The page number", 0)
        val member by optionalMember("user", "The messages you want to see from this user")
    }

    private suspend fun getProducts() = when(::products.isInitialized) {
        true -> products
        else -> api.getProducts().also { this.products = it }
    }

    private fun SpaceSpeakMessage.getData(): GetMessageResponse = messages[messageId]!!

    private suspend fun GetMessageResponse.getDistanceTravelled(): String =
        api.getDistanceTravelled(messageId).removeSurrounding("\"")

    private suspend fun GetMessageResponse.getRandomFact(): String {
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

    override suspend fun setup() {
        val messagesJob = coroutineScope { async { newSuspendedTransaction {
            // gets the list of messages we have sent to SpaceSpeak
            val messages = api.getMessages {}

            // cache all the messages we store
            this@SpaceSpeakExtension.messages = ConcurrentHashMap(
                messages.associateBy { message -> message.messageId }
            )

            // turn the list of messages we have sent into a list of message ids
            val receivedMessageIds = messages.map { it.messageId }

            // get a list of message ids that we currently hold information about
            val presentMessageIds = SpaceSpeakTable
                .select { SpaceSpeakTable.id.inList(receivedMessageIds) }
                .map { row -> row[SpaceSpeakTable.id].value }

            // remove all the messages that we already store
            val missingMessageIds = receivedMessageIds.toMutableList().apply {
                removeIf { presentMessageIds.contains(it) }
            }

            // create records for the messages that we do not store
            SpaceSpeakTable.batchInsert(missingMessageIds) { messageId ->
                this[SpaceSpeakTable.id] = messageId
                this[SpaceSpeakTable.userId] = null
            }
        } } }

        hybridCommand {
            name = "spacespeak"
            description = "todo"

            action {
                publicSpaceSpeakFollowUp {
                    description = """
                        [SpaceSpeak](https://www.spacespeak.com) is a service that allows you to **send messages into 
                        space**!
                        Check out what else they're up to [here](https://spacespeak.com/NewsAndEvents)!
                        
                        To send your message into space, use **${getPrefix()}spacespeak send <message>**
                        
                    """.trimIndent()
                }
            }

            subCommand(::SpaceSpeakSendArgs) {
                name = "send"
                description = "Sends your message into outer space"
                slashSettings { autoAck = AutoAckType.PUBLIC }

                action {
                    val product = getProducts().first()

                    // send message into space
                    val response = api.sendMessage {
                        productId = product.productId
                        email = emailAddress
                        username = this@SpaceSpeakExtension.username
                        message = arguments.message
                    }

                    // store information about sent message (e.g.; who sent the message)
                    transaction {
                        SpaceSpeakMessage.new(response.messageId) {
                            userId = user!!.idLong
                        }
                    }

                    // send success message
                    publicSpaceSpeakFollowUp {
                        description = """
                            **Your message has been sent into space!**
                            Sending images and audio is available at [spacespeak.com](https://www.spacespeak.com)
                            `${arguments.message}`
                        """.trimIndent()

                        author {
                            val user = user!!.asUser()
                            name = user.tag
                            icon = user.avatar.url
                        }

                        footer { text = "Reach out to the Universe!" }
                        timestamp = Clock.System.now()
                    }

                    api.getMessages { messageId = response.messageId }.also {
                        messages[response.messageId] = it.first()
                    }
                }
            }

            subCommand(::SpaceSpeakInfoArgs) {
                name = "info"
                description = "Displays information about a certain message"

                action {
                    when (val spaceMessage = messages[arguments.id]) {
                        null -> {
                            publicFollowUp {
                                allowedMentions {}
                                content = "Could not find a message with the id: ${arguments.id}"
                            }
                        }
                        else -> {
                            val spaceMessageDAO = transaction { SpaceSpeakMessage.findById(arguments.id)!! }
                            val distanceTravelledJob = coroutineScope { async { spaceMessage.getDistanceTravelled() } }
                            val randomFactJob = coroutineScope { async { spaceMessage.getRandomFact() } }

                            publicSpaceSpeakFollowUp {
                                description = "**Message**\n${spaceMessage.messageText}"

                                footer { text = "Message Id \u2022 ${arguments.id} | Launch Date" }
                                timestamp = spaceMessage.launchDate

                                author {
                                    val user = spaceMessageDAO.userId?.let { eventObj.kord.getUser(Snowflake(it)) }

                                    name = "Sent by: ${user?.tag ?: "N/A"}"
                                    icon = user?.avatar?.url
                                }

                                awaitAll(distanceTravelledJob, randomFactJob)
                                field("Distance Travelled", true) { distanceTravelledJob.getCompleted() }
                                field("Fun Fact", true) { randomFactJob.getCompleted() }
                            }
                        }
                    }
                }
            }

            subCommand(::SpaceSpeakRecentArgs) {
                name = "recent"
                description = "Displays recent messages that have been sent to space from other Discord users"

                val maxPerPage: Long = 3

                fun EmbedBuilder.addRecord(data: GetMessageResponse, isFirst: Boolean) {
                    field(if(isFirst) "Message ID" else EmbedBuilder.ZERO_WIDTH_SPACE, true) {
                        data.messageId.toString()
                    }

                    field(if(isFirst) "Launched" else EmbedBuilder.ZERO_WIDTH_SPACE, true) {
                        data.launchDate.toString()
                    }

                    field(if(isFirst) "Content" else EmbedBuilder.ZERO_WIDTH_SPACE, true) {
                        data.messageText
                    }
                }

                action {
                    val (pageNumber: Long, maxPages: Long, messages: List<GetMessageResponse>) = transaction {
                        val query = when(val userId = arguments.member?.idLong) {
                            null -> SpaceSpeakTable.selectAll()
                            else -> SpaceSpeakTable.select { SpaceSpeakTable.userId.eq(userId) }
                        }.orderBy(SpaceSpeakTable.id, order = SortOrder.DESC)

                        val count = query.count()

                        val maxPages = (count / maxPerPage).let { when {
                            count % maxPerPage == 0L && it > 0 -> it-1
                            else -> it
                        } }

                        val pageNumber = (arguments.pageNumber-1).coerceIn(0, maxPages)

                        val messages = SpaceSpeakMessage.wrapRows(
                            query.limit(maxPerPage.toInt(), offset = (maxPerPage * pageNumber))
                        ).map { it.getData() }

                        Triple(pageNumber, maxPages, messages)
                    }

                    publicSpaceSpeakFollowUp {
                        var isFirst = true
                        messages.forEach { message ->
                            addRecord(message, isFirst)
                            isFirst = false
                        }

                        footer { text = "Page ${pageNumber+1} / ${maxPages+1}" }
                    }
                }
            }
        }

        getProducts() // initialize products
        messagesJob.await()
    }
}
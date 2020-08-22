package qbosst.bossbot.entities

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.MessageEmbed.*
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.internal.entities.EntityBuilder
import net.dv8tion.jda.internal.utils.Checks
import net.dv8tion.jda.internal.utils.Helpers
import org.json.JSONArray
import org.json.JSONObject
import qbosst.bossbot.bot.commands.misc.colour.getColourByHex
import qbosst.bossbot.bot.commands.misc.colour.systemColours
import qbosst.bossbot.util.isHex
import java.awt.Color
import java.time.*
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.regex.Pattern
import javax.annotation.Nonnull

class JSONEmbedBuilder
{
    val ZERO_WIDTH_SPACE = "\u200E"
    val URL_PATTERN = Pattern.compile("\\s*(https?|attachment)://\\S+\\s*", Pattern.CASE_INSENSITIVE)

    private val fields: MutableList<MessageEmbed.Field> = LinkedList()
    val description = StringBuilder()
    private var colour = Role.DEFAULT_COLOR_RAW
    private var url: String? = null
    private var title: String? = null
    private var timestamp: OffsetDateTime? = null
    private var thumbnail: Thumbnail? = null
    private var author: AuthorInfo? = null
    private var footer: Footer? = null
    private var image: ImageInfo? = null

    constructor(json: JSONObject)
    {
        if(json.has(JsonName.DESCRIPTION.directory)) setDescription(json.get(JsonName.DESCRIPTION.directory).toString())
        if(json.has(JsonName.IMAGE.directory)) setImage(json.get(JsonName.IMAGE.directory).toString())
        if(json.has(JsonName.THUMBNAIL.directory)) setThumbnail(json.get(JsonName.THUMBNAIL.directory).toString())

        if(json.has(JsonName.COLOUR.directory))
        {
            when(val colour = json.get(JsonName.COLOUR.directory))
            {
                is Int -> setColour(colour)
                is Long -> setColour(colour.toInt())
                is String -> setColour(systemColours[colour] ?: getColourByHex(colour) ?: throw IllegalStateException("${JsonName.COLOUR.directory} must be an numeric or hex value!"))
                else -> throw IllegalStateException("${JsonName.COLOUR.directory} must be an numeric or hex value!")
            }
        }

        if(json.has(JsonName.TITLE.directory))
        {
            val title = json.get(JsonName.TITLE.directory)
            if(title is JSONObject)
            {
                setTitle(
                        title = if(title.has(JsonName.TITLE_TEXT.directory)) title.get(JsonName.TITLE_TEXT.directory).toString() else null,
                        url = if(title.has(JsonName.TITLE_URL.directory)) title.get(JsonName.TITLE_URL.directory).toString() else null
                )
            }
            else
                throw IllegalStateException("${JsonName.TITLE.directory} must be a JSONObject")
        }

        if(json.has(JsonName.TIMESTAMP.directory))
        {
            when(val timestamp = json.get(JsonName.TIMESTAMP.directory))
            {
                is Long -> setTimestamp(Instant.ofEpochSecond(timestamp))
                is Int -> setTimestamp(Instant.ofEpochSecond(timestamp.toLong()))
                is String ->
                {
                    if(timestamp.toLongOrNull() != null)
                        setTimestamp(Instant.ofEpochSecond(timestamp.toLong()))
                    else
                        throw IllegalStateException("${JsonName.TIMESTAMP.directory} must be a Long value!")
                }
                else -> throw IllegalStateException("${JsonName.TIMESTAMP.directory} must be a Long value!")
            }
        }

        if(json.has(JsonName.AUTHOR.directory))
        {
            val author = json.get(JsonName.AUTHOR.directory)
            if(author is JSONObject)
            {
                setAuthor(
                        if(author.has(JsonName.AUTHOR_TEXT.directory)) author.get(JsonName.AUTHOR_TEXT.directory).toString() else null,
                        if(author.has(JsonName.AUTHOR_URL.directory)) author.get(JsonName.AUTHOR_URL.directory).toString() else null,
                        if(author.has(JsonName.AUTHOR_ICON_URL.directory)) author.get(JsonName.AUTHOR_ICON_URL.directory).toString() else null
                )
            }
            else
                throw IllegalStateException("${JsonName.AUTHOR.directory} must be presented a JSONObject!")
        }

        if(json.has(JsonName.FOOTER.directory))
        {
            val footer = json.get(JsonName.FOOTER.directory)
            if(footer is JSONObject)
            {
                setFooter(
                        if(footer.has(JsonName.FOOTER_TEXT.directory)) footer.get(JsonName.FOOTER_TEXT.directory).toString() else null,
                        if(footer.has(JsonName.FOOTER_URL.directory)) footer.get(JsonName.FOOTER_URL.directory).toString() else null
                )
            }
            else
                throw IllegalStateException("${JsonName.FOOTER.directory} must be a JSONObject!")
        }

        if(json.has(JsonName.FIELDS.directory))
        {
            val fields = json.get(JsonName.FIELDS.directory)
            if(fields is JSONArray)
            {
                for(field in fields)
                {
                    if(field is JSONObject)
                    {
                        addField(
                                if(field.has(JsonName.FIELDS_NAME.directory)) field.get(JsonName.FIELDS_NAME.directory).toString() else null,
                                if(field.has(JsonName.FIELDS_VALUE.directory)) field.get(JsonName.FIELDS_VALUE.directory).toString() else null,
                                if(field.has(JsonName.FIELDS_INLINE.directory)) {
                                    val inline = field.get(JsonName.FIELDS_INLINE.directory)
                                    if(inline is Boolean) inline else
                                        throw IllegalStateException("${JsonName.FIELDS_INLINE.directory} must be a boolean!")
                                } else
                                    throw IllegalStateException("Field must have an ${JsonName.FIELDS_INLINE.directory} option with a boolean value!")
                        )
                    }
                    else
                        throw IllegalStateException("${JsonName.FIELDS.directory} must be a JSONObject!")
                }
            }
        }
    }

    fun build(): MessageEmbed
    {
        check(!isEmpty()) { "Cannot build an empty embed!" }
        check(description.length <= TEXT_MAX_LENGTH) { String.format("Description is longer than %d! Please limit your input!", TEXT_MAX_LENGTH) }
        check(length() <= EMBED_MAX_LENGTH_BOT) { "Cannot build an embed with more than $EMBED_MAX_LENGTH_BOT characters!" }
        val description = if (description.isEmpty()) null else description.toString()

        return EntityBuilder.createMessageEmbed(url, title, description, EmbedType.RICH, timestamp,
                colour, thumbnail, null, author, null, footer, image, LinkedList(fields))
    }

    fun isEmpty(): Boolean {
        return title == null && timestamp == null && thumbnail == null && author == null && footer == null && image == null && colour == Role.DEFAULT_COLOR_RAW && description.isEmpty() && fields.isEmpty()
    }

    fun length(): Int {
        var length = description.length
        synchronized(fields) { length = fields.stream().map { f: Field -> f.name!!.length + f.value!!.length }.reduce(length) { a: Int, b: Int -> Integer.sum(a, b) } }
        if (title != null) length += title!!.length
        if (author != null) length += author!!.name!!.length
        if (footer != null) length += footer!!.text!!.length
        return length
    }

    fun setDescription(description: CharSequence?): JSONEmbedBuilder {
        this.description.setLength(0)
        if (description != null && description.isNotEmpty()) appendDescription(description)
        return this
    }

    fun appendDescription(description: CharSequence): JSONEmbedBuilder {
        Checks.notNull(description, "description")
        Checks.check(this.description.length + description.length <= TEXT_MAX_LENGTH,
                "Description cannot be longer than %d characters.", TEXT_MAX_LENGTH)
        this.description.append(description)
        return this
    }

    fun setTitle(title: String?, url: String?): JSONEmbedBuilder {
        var url = url
        if (title == null) {
            this.title = null
            this.url = null
        } else {
            Checks.notEmpty(title, "Title")
            Checks.check(title.length <= TITLE_MAX_LENGTH, "Title cannot be longer than %d characters.", TITLE_MAX_LENGTH)
            if (Helpers.isBlank(url)) url = null
            urlCheck(url)
            this.title = title
            this.url = url
        }
        return this
    }


    fun setTimestamp(temporal: TemporalAccessor?): JSONEmbedBuilder? {
        when(temporal)
        {
            null -> timestamp = null
            is OffsetDateTime -> timestamp = temporal
            else ->
            {
                val offset = try { ZoneOffset.from(temporal) } catch (e: DateTimeException) { ZoneOffset.UTC }
                timestamp = try
                {
                    val ldt = LocalDateTime.from(temporal)
                    OffsetDateTime.of(ldt, offset)
                } catch (e: DateTimeException)
                {
                    try
                    {
                        val instant = Instant.from(temporal)
                        OffsetDateTime.ofInstant(instant, offset)
                    } catch (e: DateTimeException)
                    {
                        throw DateTimeException("Unable to obtain OffsetDateTime from TemporalAccessor: " +
                                temporal + " of type " + temporal.javaClass.name, e)
                    }
                }
            }
        }
        return this
    }

    fun setAuthor(name: String?, url: String?, iconUrl: String?): JSONEmbedBuilder {
        // We only check if the name is null because its presence is what determines if the
        // the author will appear in the embed.
        author = if (name == null) {
            null
        } else {
            urlCheck(url)
            urlCheck(iconUrl)
            AuthorInfo(name, url, iconUrl, null)
        }
        return this
    }

    fun setImage(url: String?): JSONEmbedBuilder
    {
        if(url == null)
        {
            this.image = null
        }
        else
        {
            urlCheck(url)
            this.image = MessageEmbed.ImageInfo(url, null, 0, 0)
        }
        return this
    }

    fun setThumbnail(url: String?): JSONEmbedBuilder
    {
        if(url == null)
        {
            this.image = null
        }
        else
        {
            urlCheck(url)
            this.thumbnail = MessageEmbed.Thumbnail(url, null, 0, 0)
        }
        return this
    }

    fun setFooter(text: String?, iconUrl: String?): JSONEmbedBuilder {
        // We only check if the text is null because its presence is what determines if the
        // footer will appear in the embed.
        footer = if(text == null) {
            null
        } else {
            Checks.check(text.length <= TEXT_MAX_LENGTH, "Text cannot be longer than %d characters.", TEXT_MAX_LENGTH)
            urlCheck(iconUrl)
            Footer(text, iconUrl, null)
        }
        return this
    }

    fun addField(name: String?, value: String?, inline: Boolean): JSONEmbedBuilder {
        if (name == null && value == null) return this
        fields.add(Field(name, value, inline))
        return this
    }

    fun addBlankField(inline: Boolean): JSONEmbedBuilder {
        fields.add(Field(EmbedBuilder.ZERO_WIDTH_SPACE, EmbedBuilder.ZERO_WIDTH_SPACE, inline))
        return this
    }

    fun setColour(colour: Int?): JSONEmbedBuilder
    {
        this.colour = colour ?: Role.DEFAULT_COLOR_RAW
        return this
    }

    fun setColour(colour: Color?): JSONEmbedBuilder
    {
        this.colour = colour?.rgb ?: Role.DEFAULT_COLOR_RAW
        return this
    }

    private fun urlCheck(url: String?) {
        if (url != null) {
            Checks.check(url.length <= URL_MAX_LENGTH, "URL cannot be longer than %d characters.", URL_MAX_LENGTH)
            Checks.check(EmbedBuilder.URL_PATTERN.matcher(url).matches(), "URL must be a valid http(s) url.")
        }
    }

    enum class JsonName(val directory: String)
    {
        TITLE("title"),
        TITLE_TEXT("text"),
        TITLE_URL("url"),

        DESCRIPTION("description"),
        COLOUR("colour"),
        TIMESTAMP("timestamp"),
        THUMBNAIL("thumbnail_url"),
        IMAGE("image_url"),

        FOOTER("footer"),
        FOOTER_TEXT("text"),
        FOOTER_URL("icon_url"),

        AUTHOR("author"),
        AUTHOR_TEXT("text"),
        AUTHOR_URL("text_url"),
        AUTHOR_ICON_URL("icon_url"),

        FIELDS("fields"),
        FIELDS_NAME("name"),
        FIELDS_VALUE("value"),
        FIELDS_INLINE("inline")
    }
}

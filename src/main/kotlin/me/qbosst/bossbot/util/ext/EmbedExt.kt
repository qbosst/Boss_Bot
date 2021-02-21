package me.qbosst.bossbot.util.ext

import dev.kord.common.entity.optional.Optional
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.EmbedRequest
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
fun EmbedRequest.isEmpty(): Boolean {
    val props = (this::class.memberProperties as Collection<KProperty1<EmbedRequest, Optional<*>>>)

    return props.none { prop -> prop.get(this).value != null }
}

val EmbedRequest.length: Int
    get() {
        var length = 0

        length += description.value?.length ?: 0
        length += fields.value?.sumOf { field -> (field.name+field.value).length } ?: 0
        length += title.value?.length ?: 0
        length += author.value?.name?.value?.length ?: 0
        length += footer.value?.text?.length ?: 0

        return length
    }

fun EmbedRequest.validate(): Unit = when {
    length > EmbedBuilder.Limits.total ->
        throw IllegalArgumentException("The embed cannot be longer than ${EmbedBuilder.Limits.total} characters")
    fields.value?.size ?: 0 > EmbedBuilder.Limits.fieldCount ->
        throw IllegalArgumentException("The embed cannot have more than ${EmbedBuilder.Limits.fieldCount} fields!")
    description.value?.length ?: 0 > EmbedBuilder.Limits.description ->
        throw IllegalArgumentException("The description cannot be longer than ${EmbedBuilder.Limits.description} characters!")
    title.value?.length ?: 0 > EmbedBuilder.Limits.title ->
        throw IllegalArgumentException("The title cannot be longer than ${EmbedBuilder.Limits.title} characters!")
    footer.value?.text?.length ?: 0 > EmbedBuilder.Footer.Limits.text ->
        throw IllegalArgumentException("The footer text cannot be longer than ${EmbedBuilder.Footer.Limits.text} characters!")
    author.value?.name?.value?.length ?: 0 > EmbedBuilder.Author.Limits.name ->
        throw IllegalArgumentException("The author name cannot be longer than ${EmbedBuilder.Author.Limits.name} characters!")
    fields.value?.any { field -> field.name.length > EmbedBuilder.Field.Limits.name } ?: false ->
        throw IllegalArgumentException("Field names cannot be longer than ${EmbedBuilder.Field.Limits.name} characters!")
    fields.value?.any { field -> field.value.length > EmbedBuilder.Field.Limits.value } ?: false ->
        throw IllegalArgumentException("Field values cannot be longer than ${EmbedBuilder.Field.Limits.value} characters!")
    isEmpty() ->
        throw IllegalArgumentException("The embed cannot be empty!")
    else -> {}
}
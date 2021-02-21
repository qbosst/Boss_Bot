package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommandContext
import com.kotlindiscord.kord.extensions.commands.MessageSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.EmbedBuilder
import me.qbosst.bossbot.commands.converters.CoalescingToNullableConverter
import me.qbosst.bossbot.util.ext.maxLength
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.ext.replyEmbed

class ConfigCommandArgs<VALUE: Any>(
    displayName: String,
    description: String,
    converter: CoalescingConverter<VALUE>
): Arguments() {
    val param by arg(displayName, description, CoalescingToNullableConverter(converter))
}

/**
 * Command used for configuration database values.
 */
class ConfigSubCommand<PRIMARY_KEY: Any, CONFIG: Any?, VALUE: Any>(
    displayName: String,
    description: String,
    converter: CoalescingConverter<VALUE>,
    extension: Extension,
    parent: GroupCommand<out Arguments>
): MessageSubCommand<ConfigCommandArgs<VALUE>>(
    extension = extension,
    parent = parent,
    arguments = { ConfigCommandArgs(displayName, description, converter) }
) {
    lateinit var updateSettingBody: (id: PRIMARY_KEY, config: CONFIG?, new: VALUE?) -> Unit

    lateinit var getSettingBody: suspend CONFIG?.(MessageCommandContext<out ConfigCommandArgs<VALUE>>) -> VALUE?

    lateinit var getConfigBody: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.() -> CONFIG?

    lateinit var primaryKeyBody: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.() -> PRIMARY_KEY

    var noArgsBody: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(current: VALUE?) -> Unit = {
            current ->
        message.replyEmbed {
            val title = displayName.split("\\s+".toRegex()).joinToString(" ") { it.capitalize() }
            val value = formatValueBody.invoke(current)
            this.description = "$title is currently set to $value".maxLength(EmbedBuilder.Limits.description)
        }
    }

    var sameArgsBody: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(current: VALUE?) -> Unit = {
            current ->
        message.replyEmbed {
            val title = displayName.split("\\s+".toRegex()).joinToString(" ") { it.capitalize() }
            val value = formatValueBody.invoke(current)

            this.description = "$title is already set to $value".maxLength(EmbedBuilder.Limits.description)
        }
    }

    var onUpdateBody: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(old: VALUE?, new: VALUE?) -> Unit = {
            old, new ->
        message.replyEmbed {
            this.title = displayName.split("\\s+".toRegex()).joinToString(" ") { it.capitalize() }

            field("Old", true) { formatValueBody.invoke(old).maxLength(EmbedBuilder.Field.Limits.value) }
            field("New", true) { formatValueBody.invoke(new).maxLength(EmbedBuilder.Field.Limits.value) }
        }
    }

    var formatValueBody: (VALUE?) -> String = { value -> value?.toString() ?: "N/A" }

    fun formatValue(body: (VALUE?) -> String) {
        this.formatValueBody = body
    }

    fun updateSetting(body: (id: PRIMARY_KEY, config: CONFIG?, updated: VALUE?) -> Unit) {
        this.updateSettingBody = body
    }

    fun getSetting(body: suspend CONFIG?.(MessageCommandContext<out ConfigCommandArgs<VALUE>>) -> VALUE?) {
        this.getSettingBody = body
    }

    fun getConfig(body: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.() -> CONFIG?) {
        this.getConfigBody = body
    }

    fun getPrimaryKey(body: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.() -> PRIMARY_KEY) {
        this.primaryKeyBody = body
    }

    fun onNoArgs(body: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(current: VALUE?) -> Unit) {
        this.noArgsBody = body
    }

    fun onSameArgs(body: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(current: VALUE?) -> Unit) {
        this.sameArgsBody = body
    }

    fun onUpdate(body: suspend MessageCommandContext<out ConfigCommandArgs<VALUE>>.(old: VALUE?, new: VALUE?) -> Unit) {
        this.onUpdateBody = body
    }

    init {
        body = {
            val config: CONFIG? = getConfigBody.invoke(this)
            val currentSetting: VALUE? = config.getSettingBody(this)
            val newSetting: VALUE? = arguments.param

            when {
                // no args given
                argsList.isEmpty() -> noArgsBody.invoke(this, currentSetting)

                // setting is already the same
                newSetting == currentSetting -> sameArgsBody.invoke(this, currentSetting)

                // update the setting
                else -> {
                    val primaryKey: PRIMARY_KEY = primaryKeyBody.invoke(this)

                    updateSettingBody.invoke(primaryKey, config, newSetting)
                    onUpdateBody.invoke(this, currentSetting, newSetting)
                }
            }
        }
    }
}

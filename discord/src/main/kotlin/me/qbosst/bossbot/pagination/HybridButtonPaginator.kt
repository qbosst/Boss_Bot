package me.qbosst.bossbot.pagination

import com.kotlindiscord.kord.extensions.components.Components
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.BaseButtonPaginator
import com.kotlindiscord.kord.extensions.pagination.EXPAND_EMOJI
import com.kotlindiscord.kord.extensions.pagination.SWITCH_EMOJI
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import me.qbosst.bossbot.commands.hybrid.HybridCommandContext
import me.qbosst.bossbot.commands.hybrid.behaviour.edit
import me.qbosst.bossbot.commands.hybrid.entity.PublicHybridMessage
import java.util.*

class HybridButtonPaginator(
    extension: Extension,
    pages: Pages,
    owner: User? = null,
    timeoutSeconds: Long? = null,
    keepEmbed: Boolean = true,
    switchEmoji: ReactionEmoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,

    val parentContext: HybridCommandContext<*>,
): BaseButtonPaginator(extension, pages, owner, timeoutSeconds, keepEmbed, switchEmoji, bundle, locale) {

    override var components: Components = Components(extension)

    var embedInteraction: PublicHybridMessage? = null

    override suspend fun send() {
        components.stop()

        if(embedInteraction == null) {
            setup()

            embedInteraction = parentContext.publicFollowUp {
                embed(embedBuilder)

                with(parentContext) {
                    this@publicFollowUp.setup(this@HybridButtonPaginator.components, timeoutSeconds)
                }
            }
        } else {
            updateButtons()

            embedInteraction!!.edit {
                embed(embedBuilder)

                with(parentContext) {
                    this@edit.setup(this@HybridButtonPaginator.components, timeoutSeconds)
                }
            }
        }
    }

    override suspend fun destroy() {
        if(!active) {
            return
        }

        active = false
        components.stop()

        if(!keepEmbed) {
            embedInteraction!!.delete()
        } else {
            embedInteraction!!.edit {
                embed(embedBuilder)
                this.components = mutableListOf()
            }
        }

        runTimeoutCallbacks()
    }
}

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
fun HybridButtonPaginator(
    parentContext: HybridCommandContext<*>,
    builder: PaginatorBuilder
): HybridButtonPaginator = HybridButtonPaginator(
    extension = builder.extension,
    pages = builder.pages,
    owner = builder.owner,
    timeoutSeconds = builder.timeoutSeconds,
    keepEmbed = builder.keepEmbed,
    bundle = builder.bundle,
    locale = builder.locale,
    parentContext = parentContext,

    switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
inline fun HybridCommandContext<*>.paginator(
    extension: Extension,
    builder: PaginatorBuilder.() -> Unit
): HybridButtonPaginator = HybridButtonPaginator(this, PaginatorBuilder(extension).apply(builder))
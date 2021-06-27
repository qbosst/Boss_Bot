package me.qbosst.bossbot.pagination

import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder
import java.util.*

class PageBuilder {
    var description: String? = null
    var title: String? = null
    var author: String? = null
    var authorIcon: String? = null
    var authorUrl: String? = null
    var color: Color? = null
    var footer: String? = null
    var footerIcon: String? = null
    var image: String? = null
    var thumbnail: String? = null
    var url: String? = null
    var bundle: String? = null

    var builder: (EmbedBuilder.(
        locale: Locale,
        pageNum: Int,
        pages: Int,
        group: String?,
        groupIndex: Int,
        groups: Int
    ) -> Unit)? = null

    fun build(): Page = object: Page(
        description = description ?: "",
        title = title,
        author = author,
        authorIcon = authorIcon,
        authorUrl = authorUrl,
        color = color,
        footer = footer,
        footerIcon = footerIcon,
        image = image,
        thumbnail = thumbnail,
        url = url,
        bundle = bundle
    ) {
        override fun build(
            locale: Locale,
            pageNum: Int,
            pages: Int,
            group: String?,
            groupIndex: Int,
            groups: Int
        ): EmbedBuilder.() -> Unit = {
            builder?.invoke(this, locale, pageNum, pages, group, groupIndex, groups)

            super.build(locale, pageNum, pages, group, groupIndex, groups).invoke(this)
        }
    }

    fun build(builder: EmbedBuilder.(
        locale: Locale,
        pageNum: Int,
        pages: Int,
        group: String?,
        groupIndex: Int,
        groups: Int
    ) -> Unit) {
        this.builder = builder
    }
}

fun Page(builder: PageBuilder.() -> Unit): Page = PageBuilder().apply(builder).build()

fun PaginatorBuilder.page(builder: PageBuilder.() -> Unit) {
    val page = PageBuilder().apply(builder).build()
    page(page)
}
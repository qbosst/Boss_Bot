package me.qbosst.bossbot.util.ext

import dev.kord.common.entity.Snowflake

fun Long.snowflake(): Snowflake = Snowflake(this)

fun Long.userMention(): String = "<@${this}>"

fun Long.channelMention(): String = "<#${this}>"
package me.qbosst.bossbot.database.models

import com.gitlab.kordlib.cache.api.data.description
import java.time.ZoneId

data class UserData(
    val userId: Long,
    val zoneId: ZoneId? = null
) {
    companion object {
        val description = description(UserData::userId)
    }
}
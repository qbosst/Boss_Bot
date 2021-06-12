package me.qbosst.bossbot.extensions.image.deepdream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class DeepDreamResponse {
    @Serializable
    data class Ok(
        val id: String,
        @SerialName("output_url") val outputUrl: String
    )
}

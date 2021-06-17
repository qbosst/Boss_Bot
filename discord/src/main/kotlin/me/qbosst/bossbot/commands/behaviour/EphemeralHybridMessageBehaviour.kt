package me.qbosst.bossbot.commands.behaviour

import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.interaction.EphemeralFollowupMessageBehavior
import dev.kord.core.cache.data.toData
import dev.kord.core.entity.Message
import dev.kord.core.entity.Strategizable
import dev.kord.core.supplier.EntitySupplyStrategy
import me.qbosst.bossbot.commands.builder.HybridMessageModifyBuilder
import me.qbosst.bossbot.commands.entity.EphemeralHybridMessage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface EphemeralHybridMessageBehaviour: HybridMessageBehaviour {
    override fun withStrategy(strategy: EntitySupplyStrategy<*>): Strategizable = if(isInteraction) {
        EphemeralFollowupMessageBehavior(id, applicationId!!, token!!, channelId, kord, strategy.supply(kord))
    } else {
        MessageBehavior(channelId, id, kord, strategy)
    }
}

@OptIn(ExperimentalContracts::class)
suspend inline fun EphemeralHybridMessageBehaviour.edit(
    builder: HybridMessageModifyBuilder.() -> Unit
): EphemeralHybridMessage {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val builder = HybridMessageModifyBuilder().apply(builder)

    val response = if(isInteraction) {
        kord.rest.interaction.modifyFollowupMessage(applicationId!!, token!!, id, builder.toSlashRequest())
    } else {
        // TODO: wait for kord pr kord.rest.channel.editMessage(channelId, id, builder.toMessageRequest())
        kord.rest.channel.editMessage(channelId, id) {
            this.content = builder.content
            this.embed = builder.embed
            this.allowedMentions = builder.allowedMentions
            this.components.addAll(builder.components)
        }
    }

    return EphemeralHybridMessage(Message(response.toData(), kord), applicationId, token, kord)
}
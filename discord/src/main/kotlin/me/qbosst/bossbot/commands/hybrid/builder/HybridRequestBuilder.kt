package me.qbosst.bossbot.commands.hybrid.builder

interface HybridRequestBuilder<M, S> {
    fun toMessageRequest(): M

    fun toSlashRequest(): S
}
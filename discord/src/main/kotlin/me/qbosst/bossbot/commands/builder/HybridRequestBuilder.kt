package me.qbosst.bossbot.commands.builder

interface HybridRequestBuilder<M, S> {
    fun toMessageRequest(): M

    fun toSlashRequest(): S
}
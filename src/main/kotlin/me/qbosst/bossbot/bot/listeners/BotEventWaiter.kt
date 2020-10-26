package me.qbosst.bossbot.bot.listeners

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import me.qbosst.bossbot.bot.BossBot

object BotEventWaiter : EventWaiter(BossBot.scheduler, true)
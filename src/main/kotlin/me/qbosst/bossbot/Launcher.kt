package me.qbosst.bossbot

import me.qbosst.bossbot.bot.BossBot
import java.util.Scanner

/**
 * The main method that starts the boss bot singleton instance
 */

object Launcher
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        try
        {
            BossBot
        }
        finally
        {
            // input
            Scanner(System.`in`).next()
        }
    }
}


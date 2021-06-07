package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Transaction

interface InitTable {
    fun init(transaction: Transaction)
}
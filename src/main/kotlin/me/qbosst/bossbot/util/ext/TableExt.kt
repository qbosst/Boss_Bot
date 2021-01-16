package me.qbosst.bossbot.util.ext

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.SQLIntegrityConstraintViolationException

/**
 * Inserts a record into the table.
 *
 * @return True if the record was inserted. False if the record was not inserted because there of a duplicate record error (Code 1062).
 */
fun <T: Table> T.insertOrIgnore(body: T.(InsertStatement<Number>) -> Unit): Boolean =
    InsertStatement<Number>(this)
        .apply { body(this) }
        .run {
            try {
                execute(TransactionManager.current())
                return@run true
            } catch (e: ExposedSQLException) {
                val cause = e.cause ?: e
                if(cause is SQLIntegrityConstraintViolationException && cause.errorCode == 1062) {
                    return@run false
                } else {
                    throw e
                }
            }
        }
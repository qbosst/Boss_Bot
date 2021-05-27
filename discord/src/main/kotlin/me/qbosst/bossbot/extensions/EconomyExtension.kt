package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.converters.member
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.database.dao.insertOrUpdate
import me.qbosst.bossbot.events.VoteEvent
import me.qbosst.bossbot.idLong
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class EconomyExtension: Extension() {
    override val name: String get() = "economy"

    class WalletArgs: Arguments() {
        val member by optionalMember("user", "Views a user's wallet", outputError = true)
    }

    class StealArgs: Arguments() {
        val member by member("user", "The user who you want to steal from")
    }

    override suspend fun setup() {
        command(::WalletArgs) {
            name = "wallet"
            description = "Views your wallet"
            aliases = arrayOf("tokens", "balance")

            action {
                val target = (arguments.member ?: message.getAuthorAsMember())!!
                val tokenAmount = target.getUserDAO().tokens

                message.reply {
                    content = when (target.id) {
                        message.author!!.id -> "You have"
                        else -> "${target.mention} has"
                    } + " $tokenAmount `BT`"

                    allowedMentions {}
                }
            }
        }

        command(::StealArgs) {
            name = "steal"
            description = "Steals Boss Tokens from a user."
            aliases = arrayOf("rob")

            action {
                val author = message.author!!
                val target = arguments.member

                newSuspendedTransaction {
                    val targetDAO = target.getUserDAO(this)

                    message.reply {
                        allowedMentions {}

                        if(targetDAO.tokens == 0L) {
                            content = "${target.mention} does not have any tokens to steal!"
                        } else {
                            val authorDAO = author.getUserDAO(this@newSuspendedTransaction)

                            val percent = (0..5).random().toFloat() / 100
                            val tokens = (targetDAO.tokens * percent).toLong()

                            targetDAO.tokens -= tokens
                            authorDAO.tokens += tokens

                            content = if(tokens == 0L) {
                                "You have failed to rob ${target.mention}"
                            } else {
                                "You have stolen $tokens `BT` from ${target.mention}"
                            }
                        }
                    }
                }
            }
        }

        command {
            name = "daily"

            // TODO: wait for cooldown pr

            action {
                val author = message.author!!

                newSuspendedTransaction {
                    author.getUserDAO(this).insertOrUpdate(this, author.idLong) {
                        tokens += 20
                    }
                }

                message.respond {
                    content = "You have claimed your daily bonus! Come back tomorrow for more tokens."
                    allowedMentions {}
                }
            }
        }

        command {
            name = "weekly"

            // TODO: wait for cooldown pr

            action {
                val author = message.author!!

                newSuspendedTransaction {
                    author.getUserDAO(this).insertOrUpdate(this, author.idLong) {
                        tokens += 100
                    }
                }

                message.respond {
                    content = "You have claimed your weekly bonus! Come back next week for more tokens."
                    allowedMentions {}
                }
            }
        }

        event<VoteEvent> {
            action {
                val user = event.getUser()

                newSuspendedTransaction {
                    user?.getUserDAO(this).insertOrUpdate(this, event.userId) {
                        tokens += 10
                    }
                }
            }
        }
    }
}
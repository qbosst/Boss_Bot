package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.database.dao.insertOrUpdate
import me.qbosst.bossbot.events.UserVoteEvent
import me.qbosst.bossbot.util.idLong
import me.qbosst.bossbot.util.positiveInt
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.math.log10
import kotlin.math.pow

class EconomyExtension: Extension() {
    override val name: String get() = "economy"

    class WalletArgs: Arguments() {
        val member by optionalMember("user", "Views a user's wallet", outputError = true)
    }

    class StealArgs: Arguments() {
        val member by member("user", "The user who you want to steal from")
    }

    class SendArgs: Arguments() {
        val amount by int("amount", "The amount of tokens you want to send", validator = positiveInt())
        val member by member("user", "The user you want to send tokens to")
    }

    override suspend fun setup() {
        slashCommand(::WalletArgs) {
            name = "wallet"
            description = "Views your wallet"
            autoAck = AutoAckType.PUBLIC

            action {
                val target = arguments.member?.asUser() ?: user
                val tokenAmount = target.getUserDAO().tokens

                publicFollowUp {
                    content = buildString {
                        append(
                            when(target.id) {
                                user.id -> "You have"
                                else -> "${target.mention} has"
                            }
                        )

                        append(" $tokenAmount tokens")
                    }
                }
            }
        }

        slashCommand(::StealArgs) {
            name = "steal"
            description = "Steals Boss Tokens from a user."
            autoAck = AutoAckType.PUBLIC

            action {
                val target = arguments.member

                if(target.id == user.id) {
                    publicFollowUp {
                        content = "You cannot create a bet against yourself."
                    }
                    return@action
                }

                newSuspendedTransaction {
                    val targetDAO = target.getUserDAO(this)

                    publicFollowUp {
                        if(targetDAO.tokens == 0L) {
                            content = "${target.mention} does not have any tokens to steal!"
                        } else {
                            val authorDAO = user.getUserDAO(this@newSuspendedTransaction)
                            val targetTokens = targetDAO.tokens

                            val calc = targetTokens * (0.05 * (0.5).pow(log10(targetTokens.toDouble()) - 3))
                            val margin = (95..105).random() / 100.0

                            val robbedAmount = (calc * margin).toLong()

                            targetDAO.tokens -= robbedAmount
                            authorDAO.tokens += robbedAmount

                            content = if(robbedAmount == 0L) {
                                "You have failed to rob ${target.mention}"
                            } else {
                                "You have stolen $robbedAmount `BT` from ${target.mention}"
                            }
                        }
                    }
                }
            }
        }

        slashCommand(::SendArgs) {
            name = "send"
            description = "Sends tokens to another user"
            autoAck = AutoAckType.PUBLIC

            action {
                val target = arguments.member.asUser()

                if(target.id == user.id) {
                    publicFollowUp {
                        content = "You cannot create a bet against yourself."
                    }
                    return@action
                }

                newSuspendedTransaction {
                    val authorDAO = user.getUserDAO(this)

                    publicFollowUp {
                        if(authorDAO.tokens < arguments.amount) {
                            content = "You do not have `${arguments.amount}` tokens to send."
                        } else {
                            val targetDAO = target.getUserDAO(this@newSuspendedTransaction)
                            authorDAO.tokens -= arguments.amount
                            targetDAO.tokens += arguments.amount

                            content = "You have sent `${arguments.amount}` to ${target.mention}"
                        }
                    }
                }
            }
        }

        slashCommand {
            name = "daily"
            description = "Claims your daily tokens"
            autoAck = AutoAckType.PUBLIC

            // TODO: wait for cooldown pr

            action {
                newSuspendedTransaction {
                    user.getUserDAO(this).insertOrUpdate(this, user.idLong) {
                        tokens += 200
                    }
                }

                publicFollowUp {
                    content = "You have claimed your daily bonus! Come back tomorrow for more tokens."
                }
            }
        }

        slashCommand {
            name = "weekly"
            description = "Claims your weekly tokens"
            autoAck = AutoAckType.PUBLIC

            // TODO: wait for cooldown pr

            action {
                newSuspendedTransaction {
                    user.getUserDAO(this).insertOrUpdate(this, user.idLong) {
                        tokens += 2000
                    }
                }

                publicFollowUp {
                    content = "You have claimed your weekly bonus! Come back next week for more tokens."
                }
            }
        }

        event<UserVoteEvent> {
            action {
                val user = event.getUser()

                newSuspendedTransaction {
                    user?.getUserDAO(this).insertOrUpdate(this, event.userId) {
                        tokens += 150
                    }
                }
            }
        }
    }
}
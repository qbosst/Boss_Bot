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
            aliases = arrayOf("tokens")

            action {
                val target = (arguments.member ?: message.getAuthorAsMember())!!

                val tokenAmount = target.getUserDAO().tokens

                message.reply {
                    content = "${target.mention} has $tokenAmount `BT`"
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

                TODO()
            }
        }

        command {
            name = "claim"

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
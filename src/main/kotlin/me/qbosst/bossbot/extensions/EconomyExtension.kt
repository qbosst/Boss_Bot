package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.converters.member
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.database.dao.getUserData

class EconomyExtension: Extension() {
    override val name: String get() = "economy"

    class WalletArgs: Arguments() {
        val member by optionalMember("user", "Views a user's wallet")
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

                val tokenAmount = target.getUserData().tokens

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
                val targetData = arguments.member.getUserData()

                message.reply {
                    content = if(targetData.tokens == 0L) {
                        "${arguments.member.mention} does not have any `BT`!"
                    } else {
                        "You have stolen X `BT` from ${arguments.member.mention}"
                    }

                    allowedMentions {}
                }
            }
        }
    }
}
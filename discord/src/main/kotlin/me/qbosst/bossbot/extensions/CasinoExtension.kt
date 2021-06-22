package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ButtonStyle
import me.qbosst.bossbot.commands.hybrid.behaviour.edit
import me.qbosst.bossbot.commands.hybrid.entity.PublicHybridMessage
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.util.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CasinoExtension: Extension() {
    override val name: String get() = "casino"

    class CoinFlipArgs: Arguments() {
        enum class CoinSide(override val readableName: String): ChoiceEnum {
            HEADS("heads"),
            TAILS("tails")
        }

        val betSide by enumChoice<CoinSide>("coin-side", "The coin side you predict it will land on", "heads, tails")
        val betAmount by int("bet-amount", "The amount of tokens you want to bet", validator = positiveInt())
        val opponent by optionalMember(
            "opponent",
            "The user you want to bet against",
            required = true,
            validator = notAuthor("You cannot bet against yourself.")
        )
    }

    override suspend fun setup() {
        hybridCommand(::CoinFlipArgs) {
            name = "coinflip"
            description = "Flips a coin and allows you to bet on the outcome"

            check(::anyGuild)

            action {
                val flippedSide = CoinFlipArgs.CoinSide.values().random()
                val opponent = arguments.opponent
                val user = user!!

                lateinit var followUp: PublicHybridMessage
                followUp = publicFollowUp {
                    newSuspendedTransaction {
                        val authorDAO = user.getUserDAO(this)

                        when {
                            authorDAO.tokens < arguments.betAmount ->
                                content = "You do not have ${arguments.betAmount} tokens to bet."
                            opponent != null -> {
                                val opponentDAO = opponent.getUserDAO(this)

                                if(opponentDAO.tokens < arguments.betAmount) {
                                    content = "${opponent.mention} does not have enough tokens."
                                } else {
                                    //allowedMentions { add(AllowedMentionType.UserMentions) }
                                    content = "${opponent.mention}, ${user.asUser().tag} has challenged you."
                                    components(30) {
                                        interactiveButton {
                                            style = ButtonStyle.Success
                                            label = "Accept"
                                            deferredAck = true

                                            check(isUser(opponent.idLong))

                                            action {
                                                this@components.stop()
                                                followUp.edit {
                                                    components {}

                                                    val winningUser = newSuspendedTransaction {
                                                        if(flippedSide == arguments.betSide) {
                                                            authorDAO.tokens += arguments.betAmount
                                                            opponentDAO.tokens -= arguments.betAmount

                                                            user
                                                        } else {
                                                            authorDAO.tokens -= arguments.betAmount
                                                            opponentDAO.tokens += arguments.betAmount

                                                            opponent.asUser()
                                                        }
                                                    }

                                                    content = buildString {
                                                        append("The coin has landed on `${flippedSide.readableName}`. ")
                                                        append("${ winningUser.mention } has won the bet, ")
                                                        append("winning `${arguments.betAmount}` tokens.")
                                                    }
                                                }
                                            }
                                        }

                                        interactiveButton {
                                            style = ButtonStyle.Danger
                                            label = "Deny"
                                            deferredAck = true

                                            check(isUser(opponent.idLong))

                                            action {
                                                this@components.stop()
                                                followUp.edit {
                                                    allowedMentions {}
                                                    components {}
                                                    content = "${opponent.mention} has not accepted the bet."
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                content = buildString {
                                    append("The coin has landed on `${flippedSide.readableName}`.")

                                    if(flippedSide == arguments.betSide) {
                                        authorDAO.tokens += arguments.betAmount
                                        append("You have won the bet, winning `${arguments.betAmount}` tokens.")
                                    } else {
                                        authorDAO.tokens -= arguments.betAmount
                                        append("You have lost the bet, losing `${arguments.betAmount}` tokens.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

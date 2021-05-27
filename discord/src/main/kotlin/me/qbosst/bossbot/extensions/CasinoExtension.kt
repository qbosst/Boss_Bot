package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.converters.enum
import com.kotlindiscord.kord.extensions.commands.converters.int
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.extensions.Extension
import me.qbosst.bossbot.database.dao.getUserDAO
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.random.Random

class CasinoExtension: Extension() {
    override val name: String = "casino"

    class CoinFlipArgs: Arguments() {
        enum class CoinSide(override val readableName: String): ChoiceEnum {
            HEADS("heads"),
            TAILS("tails")
        }

        val betSide by enum<CoinSide>("coin-side", "The coin side you predict it will land on", "heads, tails")
        val betAmount by int("bet-amount", "The amount of tokens you want to bet") { betAmount ->
            if(betAmount <= 0) {
                throw CommandException("Your bet must be a positive number.")
            }
        }
        val opponent by optionalMember("user", "The user you want to bet against", outputError = true)
    }

    override suspend fun setup() {
        slashCommand(::CoinFlipArgs) {
            name = "coinflip"
            description = "Flips a coin and allows you to bet on the outcome"
            autoAck = AutoAckType.PUBLIC
            guild(714482588005171200)

            action {
                val flippedSide = CoinFlipArgs.CoinSide.values().random()
                val opponent = arguments.opponent

                publicFollowUp {

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
                                    // TODO: ask opponent if they want to accept the bet
                                    val accepted = Random.nextBoolean() // placeholder

                                    if(accepted) {
                                        val winningUser = if(flippedSide == arguments.betSide) {
                                            authorDAO.tokens += arguments.betAmount
                                            opponentDAO.tokens -= arguments.betAmount

                                            user
                                        } else {
                                            authorDAO.tokens -= arguments.betAmount
                                            opponentDAO.tokens += arguments.betAmount

                                            opponent.asUser()
                                        }

                                        content = buildString {
                                            append("The coin has landed on `${flippedSide.readableName}`. ")
                                            append("${ winningUser.mention } has won the bet, ")
                                            append("winning `${arguments.betAmount}` tokens.")
                                        }

                                    } else {
                                        content = "${opponent.mention} has not accepted the bet."
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

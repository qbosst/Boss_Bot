package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.PublicFollowupMessage
import me.qbosst.bossbot.commands.entity.PublicHybridMessage
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.util.hybridCommand
import me.qbosst.bossbot.util.notAuthor
import me.qbosst.bossbot.util.positiveInt
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.random.Random

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
                                    actionRow {
                                        button(ButtonStyle.Success) {
                                            label = "Accept"

                                            action(AutoAckType.PUBLIC) {
                                                followUp.delete()
                                                publicFollowUp {
                                                    allowedMentions {}

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
                                                }
                                            }
                                        }

                                        button(ButtonStyle.Danger) {
                                            label = "Deny"

                                            action(AutoAckType.PUBLIC) {
                                                followUp.delete()
                                                publicFollowUp {
                                                    allowedMentions {}
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

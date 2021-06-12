package me.qbosst.bossbot.extensions.image

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Member
import me.qbosst.bossbot.commands.HybridCommandContext
import me.qbosst.bossbot.extensions.image.deepdream.DeepDreamAPI
import me.qbosst.bossbot.util.getColour
import me.qbosst.bossbot.util.hybridCommand

class ImageExtension: Extension() {
    override val name: String get() = "image"

    private val deepDreamApi = DeepDreamAPI(env("deepdream")!!)

    class DeepDreamArgs: Arguments() {
        val imageUrl by optionalUnion(
            "image-url",
            "The url of the image you want to deep-dream",
            converters = arrayOf(MemberConverter(), StringConverter()),
            shouldThrow = true
        )
    }

    class DeepDreamUserArgs: Arguments() {
        val user by member("user", "TODO")
    }

    class DeepDreamUrlArgs: Arguments() {
        val url by string("url", "TODO")
    }

    override suspend fun setup() {
        group {
            name = "image"
            description = "Applies effects onto your images"
            action { sendHelp() }

            group(::DeepDreamArgs) {
                name = "deepdream"
                description = "Applies the deepdream effect onto an image"

                action {
                    val imageUrl: String = when(val arg = arguments.imageUrl) {
                        is Member -> arg.avatar.url
                        is String -> arg
                        null -> message.attachments.firstOrNull(Attachment::isImage)?.url
                            ?: throw CommandException("Please provide a user, url or attachment to deepdream.")
                        else -> error("This should not happen...")
                    }

                    val deepDreamImageUrl = deepDreamApi.process(imageUrl)

                    message.reply {
                        allowedMentions {}
                        embed {
                            url = deepDreamImageUrl
                            color = guild?.getMemberOrNull(event.kord.selfId)?.getColour()
                        }
                    }
                }

                command(::DeepDreamUserArgs) {
                    name = "user"
                    description = "Applies the deepdream effect onto a user's pfp"

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.user.avatar.url)
                        message.reply {
                            allowedMentions {}
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(event.kord.selfId)?.getColour()
                            }
                        }
                    }
                }

                command(::DeepDreamUrlArgs) {
                    name = "url"
                    description = "Applies the deepdream effect onto a url linking to an image"

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.url)
                        message.reply {
                            allowedMentions {}
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(event.kord.selfId)?.getColour()
                            }
                        }
                    }
                }
            }
        }

        slashCommand {
            name = "image"
            description = "Applies effects onto your images"

            group("deepdream") {
                description = "Applies the deepdream effect onto an image"

                subCommand(::DeepDreamUserArgs) {
                    name = "user"
                    description = "Applies the deepdream effect onto a user's pfp"
                    autoAck = AutoAckType.PUBLIC

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.user.avatar.url)
                        println(deepDreamImageUrl)
                        publicFollowUp {
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(event.kord.selfId)?.getColour()
                            }
                        }
                    }
                }

                subCommand(::DeepDreamUrlArgs) {
                    name = "url"
                    description = "Applies the deepdream effect onto a url linking to an image"
                    autoAck = AutoAckType.PUBLIC

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.url)
                        publicFollowUp {
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(event.kord.selfId)?.getColour()
                            }
                        }
                    }
                }
            }
        }
    }
}
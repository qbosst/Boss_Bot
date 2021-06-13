package me.qbosst.bossbot.extensions.image

import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
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
        hybridCommand {
            name = "image"
            description = "Applies effects onto your images"

            group(::DeepDreamArgs) {
                name = "deepdream"
                description = "Applies the deepdream effect onto an image"
                slashSettings { autoAck = AutoAckType.PUBLIC }

                subCommand(::DeepDreamUserArgs) {
                    name = "user"
                    description = "Applies the deepdream effect onto a user's pfp"
                    slashSettings { autoAck = AutoAckType.PUBLIC }

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.user.avatar.url)
                        publicFollowUp {
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(kord.selfId)?.getColour()
                            }
                        }
                    }
                }

                subCommand(::DeepDreamUrlArgs) {
                    name = "url"
                    description = "Applies the deepdream effect onto a url"
                    slashSettings { autoAck = AutoAckType.PUBLIC }

                    action {
                        val deepDreamImageUrl = deepDreamApi.process(arguments.url)
                        publicFollowUp {
                            embed {
                                url = deepDreamImageUrl
                                color = guild?.getMemberOrNull(kord.selfId)?.getColour()
                            }
                        }
                    }
                }
            }
        }
    }
}
package me.qbosst.bossbot.bot.commands.settings.set.setters

import me.qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetterCommand
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.JSONEmbedBuilder
import me.qbosst.bossbot.entities.database.GuildSettingsData
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONException
import org.json.JSONObject

object SetWelcomeMessageCommand : SetterCommand<JSONObject>(
        "welcomemessage",
        description = "Sets the welcome message for the server\n${listOf(
                "%user_mention% -> Gets the user's mention",
                "%user_name -> Gets the user's name",
                "%user_discriminator% -> Gets the user's tag numbers",
                "%user_avatar_url% -> Gets the users avatar url",
                "%guild_name% -> Gets the guild name",
                "%guild_count% -> Gets the guild's total member count",
                "%guild_icon_url% -> Gets the guild icon url. Will return a default avatar if server does not have one.",
                "%user_join% -> The time of which the user joined"
        ).joinToString("\n")}",
        displayName = "Welcome Message"
) {
    override fun get(guild: Guild): JSONObject? {
        return GuildSettingsData.get(guild).welcome_message
    }

    override fun getAsString(guild: Guild): String {
        return get(guild)?.toString() ?: "none"
    }

    override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): JSONObject? {
        val content = args.joinToString(" ")
        return try
        {
            JSONObject(content)
        }
        catch (e: JSONException)
        {
            JSONObject()
                    .put("content", content)
        }
    }

    override fun isValid(event: MessageReceivedEvent, args: List<String>): Boolean {
        return if(args.isNotEmpty())
        {
            val builder = MessageBuilder()
            val member = event.member!!
            val content = args.joinToString(" ")
                    .replace("%user_mention%", member.asMention)
                    .replace("%user_name%", member.user.name)
                    .replace("%user_discriminator%", member.user.discriminator)
                    .replace("%user_id%", member.id)
                    .replace("%guild_name%", member.guild.name)
                    .replace("%guild_count%", member.guild.members.size.toString())
                    .replace("%user_avatar_url%", member.user.effectiveAvatarUrl)
                    .replace("%guild_icon_url%", member.guild.iconUrl ?: member.user.defaultAvatarUrl)
                    .replace("%user_join%", member.timeJoined.toEpochSecond().toString())
            try
            {
                val json = JSONObject(content)

                if(json.has("content"))
                {
                    builder.setContent(json.get("content").toString())
                }

                if(json.has("embed"))
                {
                    if(json.get("embed") is JSONObject)
                        builder.setEmbed(JSONEmbedBuilder(json.getJSONObject("embed")).build())
                    else if(!json.get("embed").toString().isNullOrBlank())
                        throw IllegalStateException("Embed must be a JSONObject!")
                }

                builder.build()
                true

            } catch (e: Exception) {
                event.channel.sendMessage("Caught Exception: $e").queue()
                false
            }
        } else false
    }

    override fun set(guild: Guild, obj: JSONObject?): JSONObject? {
        return try {
            JSONObject(GuildSettingsData.update(guild, GuildSettingsDataTable.welcome_message, obj?.toString()))
        } catch (e: JSONException)
        {
            return null
        }
    }
}

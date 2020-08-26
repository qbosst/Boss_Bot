package me.qbosst.bossbot.bot.commands.misc.deepai

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.config.Config
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

abstract class DeepaiCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        guildOnly: Boolean = true,
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        private val url: String
): Command(
        name, description, usage, examples, aliases, guildOnly, userPermissions, botPermissions
) {

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(Config.Values.DEEPAI_TOKEN.getString().isNullOrEmpty())
        {
            event.channel.sendMessage("This command cannot be used as there is no deepai token provided").queue()
        }
        else
        {
            val params = getParameters(event, args)
            if(params.isNotEmpty()) event.channel.sendTyping().queue(); execute(event, getResult(params))
        }
    }

    abstract fun execute(event: MessageReceivedEvent, json: CompletableFuture<JSONObject>)

    abstract fun getParameters(event: MessageReceivedEvent, args: List<String>): List<NameValuePair>

    private fun getResult(parameters: List<NameValuePair>): CompletableFuture<JSONObject> {
        return CompletableFuture.supplyAsync( Supplier
        {
            val post = HttpPost(url)
            post.setHeader("api-key", Config.Values.DEEPAI_TOKEN.toString())
            try
            {
                post.entity = UrlEncodedFormEntity(parameters, "UTF-8")
                HttpClients.createDefault().use { client -> client.execute(post).use { response -> JSONObject(EntityUtils.toString(response.entity)) } }
            }
            catch (e: UnsupportedEncodingException)
            {
                e.printStackTrace()
                JSONObject()
            }
            catch (e: IOException)
            {
                e.printStackTrace()
                JSONObject()
            }
        }, BossBot.threadpool)
    }
}
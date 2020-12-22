package me.qbosst.bossbot.bot.commands.misc.deepai

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.config.BotConfig
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

abstract class DeepAiCommand(
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
)
{
    final override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        // Gets the parameters.
        val parameters = getParameters(event, args)
        if(parameters != null)
        {
            event.channel.sendTyping().queue()
            val request = getResult(event.jda.httpClient, parameters)
            request.thenAccept()
            { response ->
                val json = JSONObject(response.body()?.string() ?: "{}")

                // Checks if the response is valid
                if(json.has("status") && json.getString("status").startsWith("Out of free credits"))
                    event.channel.sendMessage("I have ran out of credits for deepai services :( Try again later!").queue()
                else if(response.code() == 401)
                    event.channel.sendMessage("I do not have access to deepai services, please fix it boss :(").queue()
                else
                    execute(event, json)
            }
        }
    }

    /**
     *  Method invoked when the request was successfully retrieved
     *
     *  @param event The event of which the command came from
     *  @param json The json response of the request
     */
    abstract fun execute(event: MessageReceivedEvent, json: JSONObject)

    /**
     *  Gets the parameters that the request may need.
     *
     *  @param event the event of which the command came from
     *  @param args the arguemnts of the command
     *
     *  @return the parameters of the request. Null if a mistake was made by the user.
     */
    abstract fun getParameters(event: MessageReceivedEvent, args: List<String>): Map<String, String>?

    /**
     *  Queries the deepai url and returns the response it gave
     *
     *  @param client The client to use to connect to the website
     *  @param parameters any parameters needed for the request
     *
     *  @return The response
     */
    private fun getResult(client: OkHttpClient, parameters: Map<String, String>): CompletableFuture<Response>
    {
        // Creates the request
        val request = Request.Builder()
                .url(this.url)
                .addHeader("api-key", BotConfig.deepai_token)

        if(parameters.isNotEmpty())
        {
            val body = FormBody.Builder()
            for(parameter in parameters)
                body.add(parameter.key, parameter.value)

            request.post(body.build())
        }

        // Returns in completable future so that it does not block the main thread.
        return CompletableFuture.supplyAsync()
        {
            client.newCall(request.build()).execute()
        }

    }
}
package gay.spiders

import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gay.spiders.OmnipresentKontrolObject")

fun main() = runBlocking {
    logger.info("Bot is starting...")

    val token = System.getenv("DISCORD_TOKEN")
    if (token == null) {
        println("Error: DISCORD_TOKEN environment variable not set.")
        return@runBlocking
    }

    val kord = Kord(token)

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${self.tag}")
        logger.info("Connected to ${guilds.count()} guilds.")
    }

    kord.on<MessageCreateEvent> {
        onMessage(message)
    }

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }

    logger.info("Bot is shutting down.")
}

suspend fun onMessage(message: Message) {
    if (message.author?.isBot == true) return

    if (message.content == "!test") {
        logger.debug("Received !ping from ${message.author?.tag}")
        message.channel.createMessage("Test successful.")
    }
}
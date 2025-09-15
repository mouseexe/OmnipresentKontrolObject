package gay.spiders

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
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
    val maushold = System.getenv("MAUSHOLD")
    val testServer = Snowflake(maushold)
    if (token == null) {
        println("Error: DISCORD_TOKEN environment variable not set.")
        return@runBlocking
    }

    val kord = Kord(token)

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${self.tag}")
        logger.info("Connected to ${guilds.count()} guilds.")
    }

    logger.info("Registering slash commands...")
//    kord.createGlobalApplicationCommands {
    kord.createGuildApplicationCommands(testServer) {
        input("balance", "Check your current balances.")
    }.collect { command ->
        logger.info("Successfully registered guild command: /${command.name}")
    }

    kord.on<InteractionCreateEvent> {
        val interaction = this as? ApplicationCommandInteraction ?: return@on

        when (interaction.invokedCommandName) {
            "balance" -> {
                interaction.respondEphemeral {
                    content = "You have no funds currently."
                }
            }
        }
    }

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }

    logger.info("Bot is shutting down.")
}
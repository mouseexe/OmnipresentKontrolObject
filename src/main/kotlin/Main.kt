package gay.spiders

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gay.spiders.OKO")

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
    kord.createGuildApplicationCommands(testServer) {
        Actions.entries.forEach {
            input(it.name.lowercase(), it.description)
        }
    }.collect { command ->
        logger.info("Successfully registered guild command: /${command.name}")
    }
//    kord.createGlobalApplicationCommands {
//        Actions.entries.forEach {
//            input(it.name.lowercase(), it.description)
//        }
//    }.collect { command ->
//        logger.info("Successfully registered global command: /${command.name}")
//    }

    val players = mutableListOf(
        Player(
            id = "test1",
            credits = 100,
            tokens = 100,
            shares = 100,
            factions = emptyList()
        ),
        Player(
            id = "test2",
            credits = 100,
            tokens = 100,
            shares = 100,
            factions = emptyList()
        )
    )

    kord.on<InteractionCreateEvent> {
        val interaction = (this as? GuildChatInputCommandInteractionCreateEvent)?.interaction ?: return@on
        val id = interaction.data.message.value?.author?.username.orEmpty()
        val player = players.firstOrNull { it.id == id }

        logger.info("Received command: /${interaction.invokedCommandName}")

        when (Actions.get(interaction.invokedCommandName)) {
            Actions.REGISTER -> {
                if (player != null) {
                    interaction.respondEphemeral {
                        content = "You are already registered."
                    }
                } else {
                    players += Player(
                        id = id,
                        credits = 100,
                        tokens = 100,
                        shares = 100,
                        factions = emptyList()
                    )
                    interaction.respondEphemeral {
                        content = "You have been registered."
                    }
                }
            }

            Actions.BALANCE -> {
                if (player != null) {
                    interaction.respondEphemeral {
                        content = "Credits: ${player.credits}, Tokens: ${player.tokens}, Shares: ${player.shares}"
                    }
                } else {
                    interaction.respondEphemeral {
                        content = "You need to register first!"
                    }
                }
            }

            Actions.RENT -> {
                if (player != null) {
                    val credits = player.credits - 1
                    players.replaceAll { if (it.id == id) it.copy(credits = credits) else it }
                    interaction.respondEphemeral {
                        content = "Rent paid."
                    }
                } else {
                    interaction.respondEphemeral {
                        content = "You need to register first!"
                    }
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
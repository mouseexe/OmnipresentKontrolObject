package gay.spiders

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.*
import gay.spiders.Action.Admin
import gay.spiders.Users.getPlayer
import gay.spiders.Users.toPlayer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gay.spiders.OKO")

@OptIn(PrivilegedIntent::class, KordExperimental::class)
fun main() = runBlocking {
    logger.info("Bot is starting...")

    DatabaseFactory.init()

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

    kord.createGuildApplicationCommands(testServer) {
        Action.entries.forEach {
            input(it.name.lowercase(), it.description) {
                it.params.forEach { param ->
                    when (param.type) {
                        Param.Type.STRING -> string(param.name, param.description) {
                            required = param.required
                            param.options?.let {
                                param.options.forEach { option ->
                                    choice(option.toString(), option.toString())
                                }
                            }
                        }

                        Param.Type.INTEGER -> integer(param.name, param.description) {
                            required = param.required
                            param.options?.let {
                                param.options.forEach { option ->
                                    choice(option.toString(), option.toString().toLong())
                                }
                            }
                        }

                        Param.Type.BOOLEAN -> boolean(param.name, param.description) {
                            required = param.required
                        }

                        Param.Type.USER -> user(param.name, param.description) {
                            required = param.required
                        }

                        Param.Type.CHANNEL -> channel(param.name, param.description) {
                            required = param.required
                        }

                        Param.Type.ROLE -> role(param.name, param.description) {
                            required = param.required
                        }
                    }
                }
            }
        }
    }.collect { command ->
        logger.info("Successfully registered guild command: /${command.name}")
    }

    kord.on<InteractionCreateEvent> {
        val interaction = (this as? ChatInputCommandInteractionCreateEvent)?.interaction ?: return@on
        val user = interaction.user.data
        val player = Users.getPlayer(user.discordId)

        suspend fun validatePlayer(call: suspend (Player) -> Unit) {
            player?.let { call(it) } ?: interaction.respondEphemeral { content = "You need to register first!" }
        }

        logger.info("Received command: /${interaction.invokedCommandName}")

        when (val action = Action.get(interaction.invokedCommandName)) {
            Action.REGISTER -> {
                val (userParam) = action.params
                val targetData = interaction.command.users[userParam.name]?.data ?: user
                val target = Users.getPlayer(targetData.discordId)
                if (target != null) {
                    interaction.respondEphemeral {
                        content = "${targetData.username} is already registered."
                    }
                } else {
                    transaction {
                        Users.insert {
                            it[Users.discordId] = targetData.discordId
                            it[Users.username] = targetData.username
                            it[Users.credits] = 100
                            it[Users.tokens] = 100
                            it[Users.shares] = 100
                        }
                    }
                    interaction.respondEphemeral {
                        content = "${targetData.username} has been registered."
                    }
                }
            }

            Action.BALANCE -> {
                validatePlayer {
                    interaction.respondEphemeral {
                        content = "Credits: ${it.credits}, Tokens: ${it.tokens}, Shares: ${it.shares}"
                    }
                }
            }

            Action.TRANSFER -> {
                validatePlayer { source ->
                    val (recipientParam, accountParam, amountParam) = action.params
                    val recipientUser = interaction.command.users[recipientParam.name]!!
                    val recipient = Users.getPlayer(recipientUser.data.discordId) ?: run {
                        interaction.respondEphemeral {
                            content = "Recipient is not registered."
                        }
                        return@validatePlayer
                    }
                    val accountName = interaction.command.strings[accountParam.name]!!
                    val account = Player.Account.get(accountName)

                    val amount = interaction.command.integers[amountParam.name]!!.toInt()

                    val (playerSource, recipientSource) = when (account) {
                        Player.Account.CREDITS -> source.credits to recipient.credits
                        Player.Account.TOKENS -> source.tokens to recipient.tokens
                        Player.Account.SHARES -> source.shares to recipient.shares
                    }

                    if (playerSource < amount) {
                        interaction.respondEphemeral {
                            content = "Insufficient funds."
                        }
                    } else {
                        val playerBalance = playerSource - amount
                        val recipientBalance = recipientSource + amount
                        transaction {
                            Users.update({ Users.discordId eq source.userId }) {
                                when (account) {
                                    Player.Account.CREDITS -> it[Users.credits] = playerBalance
                                    Player.Account.TOKENS -> it[Users.tokens] = playerBalance
                                    Player.Account.SHARES -> it[Users.shares] = playerBalance
                                }
                            }
                            Users.update({ Users.discordId eq recipient.userId }) {
                                when (account) {
                                    Player.Account.CREDITS -> it[Users.credits] = recipientBalance
                                    Player.Account.TOKENS -> it[Users.tokens] = recipientBalance
                                    Player.Account.SHARES -> it[Users.shares] = recipientBalance
                                }
                            }
                        }
                        interaction.respondEphemeral {
                            content = "Transferred $amount ${account.name.lowercase()} to ${recipient.username}."
                        }
                    }
                }
            }

            Action.LIFESTYLE -> {
                validatePlayer { target ->
                    val (lifestyleParam) = action.params
                    val lifestyleName = interaction.command.strings[lifestyleParam.name]!!
                    val lifestyle = Player.Lifestyle.get(lifestyleName)
                    if (target.credits < lifestyle.cost) {
                        interaction.respondEphemeral {
                            content = "You cannot afford a ${lifestyle.name} lifestyle."
                        }
                    } else {
                        transaction {
                            Users.update({ Users.discordId eq target.userId }) {
                                it[Users.lifestyle] = lifestyle.name
                            }
                        }
                        interaction.respondEphemeral {
                            content = "You have adopted a ${lifestyle.name} lifestyle."
                        }
                    }
                }
            }

            Action.ADMIN -> {
                val (commandParam) = action.params
                val commandName = interaction.command.strings[commandParam.name]!!
                val command = Admin.get(commandName)
                when (command) {
                    Admin.FORWARD -> {
                        val response = interaction.deferEphemeralResponse()
                        transaction {
                            Users.selectAll().where { Users.alive eq true }.forEach {
                                val player = it.toPlayer()
                                var balance = player.credits
                                Users.update({ Users.discordId eq player.userId }) { user ->
                                    balance -= 10 // 02 fee
                                    if (balance >= player.lifestyle.cost) {
                                        balance -= player.lifestyle.cost
                                    } else {
                                        Player.Lifestyle.affordableLifestyle(balance)?.let { lifestyle ->
                                            balance -= lifestyle.cost
                                            user[Users.lifestyle] = lifestyle.name
                                        } ?: run {
                                            user[Users.alive] = false
                                            // TODO: inform a user if they die?
                                        }
                                    }
                                    user[Users.credits] = balance
                                }
                            }
                        }
                        response.respond { content = "Day progressed." }
                    }
                }
            }
        }
    }

    kord.login {
        intents += Intent.MessageContent
        intents += Intent.GuildMembers
    }

    logger.info("Bot is shutting down.")
}
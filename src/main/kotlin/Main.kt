package gay.spiders

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import gay.spiders.data.*
import gay.spiders.data.Action.Admin
import gay.spiders.data.Users.getPlayer
import gay.spiders.data.Users.toPlayer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
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

    val games = mutableMapOf<Long, Game>()

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${self.tag}")
        logger.info("Connected to ${guilds.count()} guilds.")
    }

    kord.createGuildApplicationCommands(testServer) {
        Action.entries.forEach {
            input(it.name.lowercase(), it.description) { addParameters(it.params) }
        }
    }.collect { command ->
        logger.info("Successfully registered guild command: /${command.name}")
    }

    kord.on<InteractionCreateEvent> {
        val interaction = (this as? ChatInputCommandInteractionCreateEvent)?.interaction ?: return@on
        val userData = interaction.user.data
        val player = Users.getPlayer(userData.discordId)

        suspend fun validatePlayer(call: suspend (Player) -> Unit) {
            player?.let { call(it) } ?: interaction.respondEphemeral { content = "You need to register first!" }
        }

        suspend fun requirePermission(
            permissionCheck: (Player) -> Boolean,
            noPermissionMessage: String,
            action: suspend (Player) -> Unit
        ) {
            validatePlayer { user ->
                if (permissionCheck(user)) action(user)
                else interaction.respondEphemeral { content = noPermissionMessage }
            }
        }

        val validateAdmin: suspend (suspend (Player) -> Unit) -> Unit =
            { action -> requirePermission(Player::admin, "This command is for admins only.", action) }
        val validateBratva: suspend (suspend (Player) -> Unit) -> Unit = { action ->
            requirePermission(
                { it.bratva?.rank == Bratva.Boss.rank },
                "This command is for vory only.",
                action
            )
        }
        val validateTempest: suspend (suspend (Player) -> Unit) -> Unit = { action ->
            requirePermission(
                { it.tempest?.rank == Tempest.Boss.rank },
                "This command is for commanders only.",
                action
            )
        }

        logger.info("${userData.username} used command: /${interaction.invokedCommandName}")

        when (val action = Action.get(interaction.invokedCommandName)) {
            Action.REGISTER -> {
                val (userParam) = action.params
                val targetData = interaction.command.users[userParam.name]?.data ?: userData
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
                        }
                    }
                    interaction.respondEphemeral {
                        content = "${targetData.username} has been registered."
                    }
                }
            }

            Action.BALANCE -> validatePlayer { user ->
                interaction.respondEphemeral {
                    content = """Status: ${if (user.alive) "Alive" else "Dead"}
                        |Lifestyle: ${user.lifestyle.name}
                        |Credits: ${user.credits}
                        |Tokens: ${user.tokens}
                        |Shares: ${user.shares}
                        |${user.factionString()}""".trimMargin()
                }
            }

            Action.TRANSFER -> validatePlayer { source ->
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

                if (amount < 1) {
                    interaction.respondEphemeral {
                        content = "Transfer amount must be greater than 0."
                    }
                } else if (playerSource < amount) {
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

            Action.LIFESTYLE -> validatePlayer { user ->
                val (lifestyleParam) = action.params
                val lifestyleName = interaction.command.strings[lifestyleParam.name]!!
                val lifestyle = Player.Lifestyle.get(lifestyleName)
                if (user.credits < lifestyle.cost) {
                    interaction.respondEphemeral {
                        content = "You cannot afford a ${lifestyle.name} lifestyle."
                    }
                } else {
                    transaction {
                        Users.update({ Users.discordId eq user.userId }) {
                            it[Users.lifestyle] = lifestyle
                        }
                    }
                    interaction.respondEphemeral {
                        content = "You have adopted a ${lifestyle.name} lifestyle."
                    }
                }
            }

            Action.ADMIN -> validateAdmin {
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
                                            user[Users.lifestyle] = lifestyle
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

            Action.JOIN -> validatePlayer { user ->
                val (factionParam) = action.params
                val factionName = interaction.command.strings[factionParam.name]!!
                val faction = getFaction(factionName)!!
                if (user.isMember(faction)) {
                    interaction.respondEphemeral {
                        content = "You are already in the $factionName."
                    }
                } else {
                    transaction {
                        Users.update({ Users.discordId eq user.userId }) {
                            when (faction) {
                                is Bratva -> it[Users.bratva] = faction
                                is Local -> it[Users.local] = faction
                                is Solarian -> it[Users.solarian] = faction
                                is Tempest -> it[Users.tempest] = faction
                                is Canyonheavy -> it[Users.canyonheavy] = faction
                                is Stratemeyer -> it[Users.stratemeyer] = faction
                            }
                        }
                    }
                    interaction.respondEphemeral {
                        content = "You are now in the $factionName."
                    }
                }
            }

            Action.LEAVE -> validatePlayer { user ->
                val (factionParam) = action.params
                val factionName = interaction.command.strings[factionParam.name]!!
                val faction = getFaction(factionName)!!
                if (user.isMember(faction).not()) {
                    interaction.respondEphemeral {
                        content = "You are not in the $factionName."
                    }
                } else {
                    transaction {
                        Users.update({ Users.discordId eq user.userId }) {
                            when (faction) {
                                is Bratva -> it[Users.bratva] = null
                                is Local -> it[Users.local] = null
                                is Solarian -> it[Users.solarian] = null
                                is Tempest -> it[Users.tempest] = null
                                is Canyonheavy -> it[Users.canyonheavy] = null
                                is Stratemeyer -> it[Users.stratemeyer] = null
                            }
                        }
                    }
                    interaction.respondEphemeral {
                        content = "You are no longer in the $factionName."
                    }
                }
            }

            Action.RECRUIT -> validateBratva {
                val (userParam) = action.params
                val targetData = interaction.command.users[userParam.name]?.data!!
                val target = Users.getPlayer(targetData.discordId)
                if (target == null) {
                    interaction.respondEphemeral {
                        content = "${targetData.username} is not registered."
                    }
                } else {
                    if (target.isMember(Bratva.Member)) {
                        interaction.respondEphemeral {
                            content = "${targetData.username} is already a droog."
                        }
                    } else {
                        transaction {
                            Users.update({ Users.discordId eq target.userId }) {
                                it[Users.bratva] = Bratva.Member
                            }
                        }
                        interaction.respondEphemeral {
                            content = "${targetData.username} is now a droog."
                        }
                    }
                }
            }

            Action.DISCHARGE -> validateTempest {
                val (userParam) = action.params
                val targetData = interaction.command.users[userParam.name]?.data!!
                val target = Users.getPlayer(targetData.discordId)
                if (target == null) {
                    interaction.respondEphemeral {
                        content = "${targetData.username} is not registered."
                    }
                } else {
                    if (target.isMember(Tempest.Member).not()) {
                        interaction.respondEphemeral {
                            content = "${targetData.username} is not an officer."
                        }
                    } else {
                        transaction {
                            Users.update({ Users.discordId eq target.userId }) {
                                it[Users.tempest] = null
                            }
                        }
                        interaction.respondEphemeral {
                            content = "${targetData.username} is no longer an officer."
                        }
                    }
                }
            }

            Action.MINE -> validatePlayer {
                val (problem, answer) = getProblem()
                logger.info(" ### Problem answer: $answer")
                interaction.modal("Canyonheavy Proof of Work", answer.toString()) {
                    actionRow {
                        textInput(TextInputStyle.Paragraph, "problem-display", "Source matrices:") {
                            value = problem
                            required = false
                        }
                    }
                    actionRow {
                        textInput(TextInputStyle.Short, "pow-answer", "Sum of product matrix") {
                            placeholder = "Sum"
                            required = true
                        }
                    }
                }
            }

//            Action.GAMBLE -> validatePlayer { user ->
//                if (user.credits >= 5) {
//                    games[user.userId] = Game.newGame()
//                    val game = games[user.userId]!!
//                    game.deal()
//                    interaction.respondEphemeral {
//                        content = blackjackString(game)
//                        blackjackButtons()
//                    }
//                } else {
//                    interaction.respondEphemeral {
//                        content = "Insufficient funds. Entry is 5 credits."
//                    }
//                }
//            }
        }
        syncUserStatus()
    }

//    kord.on<ButtonInteractionCreateEvent> {
//        val message = interaction.deferEphemeralMessageUpdate()
//        val game = games[interaction.user.id.value.toLong()]
//
//        val userData = interaction.user.data
//        val player = Users.getPlayer(userData.discordId)!!
//
//        when (interaction.componentId) {
//            "hit" -> {
//                if (game!!.hit()) {
//                    message.edit {
//                        content = blackjackString(game)
//                        blackjackButtons()
//                    }
//                } else {
//                    message.edit {
//                        content = blackjackString(game, "**BUSTED AT ${game.hand.calculate()}**")
//                        rematchButton("BUSTED")
//                    }
//                }
//            }
//
//            "stand" -> {
//                if (game!!.stand()) {
//                    message.edit {
//                        content = blackjackString(
//                            game,
//                            "**YOU WIN AT ${game.hand.calculate()}${if (game.hand.isBlackjack()) " (BLACKJACK)" else ""} TO DEALER'S ${game.dealer.calculate()}**\n*10 credits awarded*"
//                        )
//                        rematchButton("YOU WON")
//                    }
//                    transaction {
//                        Users.update({ Users.discordId eq player.userId }) {
//                            it[Users.credits] = Users.credits.plus(10)
//                        }
//                    }
//                } else {
//                    if ((game.dealer.calculate() == game.hand.calculate() && game.dealer.isBlackjack()
//                            .not()) || game.hand.isBlackjack()
//                    ) {
//                        message.edit {
//                            content = blackjackString(
//                                game,
//                                "**TIE AT ${game.hand.calculate()} TO DEALER'S ${game.dealer.calculate()}**\n*5 credits refunded*"
//                            )
//                            rematchButton("TIE")
//                        }
//                        transaction {
//                            Users.update({ Users.discordId eq player.userId }) {
//                                it[Users.credits] = Users.credits.plus(5)
//                            }
//                        }
//                    } else {
//                        message.edit {
//                            content = blackjackString(
//                                game,
//                                "**DEALER WINS AT ${game.dealer.calculate()}${if (game.dealer.isBlackjack()) " (BLACKJACK)" else ""} TO YOUR ${game.hand.calculate()}**"
//                            )
//                            rematchButton("YOU LOST")
//                        }
//                    }
//                }
//            }
//
//            "rematch" -> {
//                if (player.credits >= 5) {
//                    game!!.reset()
//                    game.deal()
//                    message.edit {
//                        content = blackjackString(game)
//                        blackjackButtons()
//                    }
//                    transaction {
//                        Users.update({ Users.discordId eq player.userId }) {
//                            it[Users.credits] = Users.credits.minus(5)
//                        }
//                    }
//                } else {
//                    message.edit {
//                        content = "Insufficient funds. Entry is 5 credits."
//                    }
//                }
//            }
//        }
//    }

    kord.on<ModalSubmitInteractionCreateEvent> {
        val correctAnswer = interaction.modalId
        val userAnswer = interaction.textInputs["pow-answer"]?.value

        if (userAnswer != null) {
            val userData = interaction.user.data
            val player = Users.getPlayer(userData.discordId)!!
            if (userAnswer == correctAnswer) {
                transaction {
                    Users.update({ Users.discordId eq player.userId }) {
                        it[tokens] = tokens.plus(1)
                    }
                }
                interaction.respondEphemeral {
                    content = "`<WORK VERIFIED. 1 TOKEN TRANSFERRED/>`"
                }
            } else {
                interaction.respondEphemeral {
                    content = "`<WORK NOT VERIFIED. INCORRECT ANSWER/>`"
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

fun syncUserStatus() {
    transaction {
        val canyonheavyMismatches = Users.selectAll().where {
            (Users.tokens greaterEq 1) and (Users.canyonheavy.isNull()) or
                    ((Users.tokens eq 0) and (Users.canyonheavy.isNotNull()))
        }.toList()

        if (canyonheavyMismatches.isNotEmpty()) {
            canyonheavyMismatches.forEach { row ->
                Users.update({ Users.id eq row[Users.id] }) {
                    it[canyonheavy] = if (row[Users.tokens] > 0) Canyonheavy.Member else null
                }
            }
            logger.info("Synchronized ${canyonheavyMismatches.size} Canyonheavy members.")
        }

        val stratemeyerMismatches = Users.selectAll().where {
            (Users.shares greaterEq 1) and (Users.stratemeyer.isNull()) or
                    ((Users.shares eq 0) and (Users.stratemeyer.isNotNull()))
        }.toList()

        if (stratemeyerMismatches.isNotEmpty()) {
            stratemeyerMismatches.forEach { row ->
                Users.update({ Users.id eq row[Users.id] }) {
                    it[stratemeyer] = if (row[Users.shares] > 0) Stratemeyer.Member else null
                }
            }
            logger.info("Synchronized ${stratemeyerMismatches.size} Stratemeyer members.")
        }
    }
}
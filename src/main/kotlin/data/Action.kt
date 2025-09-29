package gay.spiders.data

enum class Action(val description: String, val params: List<Param> = emptyList()) {
    REGISTER(
        description = "Register as a player",
        params = listOf(
            Param(
                name = "user",
                description = "User to register",
                type = Param.Type.USER
            )
        )
    ),
    BALANCE("Check your current balances"),
    LIFESTYLE(
        description = "Adjust your lifestyle",
        params = listOf(
            Param(
                name = "lifestyle",
                description = "Lifestyle to adopt",
                type = Param.Type.STRING,
                options = Player.Lifestyle.entries.map { it.name.lowercase() },
                required = true
            )
        )
    ),
    ADMIN(
        description = "Moderator administrative tasks",
        params = listOf(
            Param(
                name = "subcommand",
                description = "Command to run",
                type = Param.Type.STRING,
                options = Admin.entries.map { it.name.lowercase() },
                required = true
            )
        )
    ),
    TRANSFER(
        description = "Transfer funds",
        params = listOf(
            Param(
                name = "recipient",
                description = "Receiver of funds",
                type = Param.Type.USER,
                required = true
            ),
            Param(
                name = "account",
                description = "Account to pull from",
                type = Param.Type.STRING,
                options = Player.Account.entries.map { it.name.lowercase() },
                required = true
            ),
            Param(
                name = "amount",
                description = "Amount of funds",
                type = Param.Type.INTEGER,
                required = true
            )
        )
    ),
    JOIN(
        "Join a faction",
        params = listOf(
            Param(
                name = "faction",
                description = "Faction to join",
                type = Param.Type.STRING,
                options = listOf(Local::class.simpleName, Solarian::class.simpleName, Tempest::class.simpleName),
                required = true
            )
        )
    ),
    LEAVE(
        "Leave a faction",
        params = listOf(
            Param(
                name = "faction",
                description = "Faction to leave",
                type = Param.Type.STRING,
                options = listOf(Local::class.simpleName, Solarian::class.simpleName),
                required = true
            )
        )
    ),
    RECRUIT(
        "Recruit a droog into the Bratva",
        params = listOf(
            Param(
                name = "recruit",
                description = "Recruit to add to the Bratva",
                type = Param.Type.USER,
                required = true
            ),
        )
    ),
    DISCHARGE(
        "Discharge an officer from the Tempest",
        params = listOf(
            Param(
                name = "officer",
                description = "Officer to discharge from the Tempest",
                type = Param.Type.USER,
                required = true
            )
        )
    ),

    //    GAMBLE("Let's go gambling"),
    MINE("Demonstrate proof of work for Canyonheavy tokens");

    companion object {
        fun get(action: String) = entries.first { it.name.lowercase() == action }
    }

    enum class Admin {
        FORWARD;

        companion object {
            fun get(admin: String) = entries.first { it.name.lowercase() == admin }
        }
    }
}

data class Param(
    val name: String,
    val description: String,
    val type: Type,
    val options: List<*>? = null,
    val required: Boolean = false
) {
    enum class Type {
        STRING,
        INTEGER,
        BOOLEAN,
        USER,
        CHANNEL,
        ROLE
    }
}
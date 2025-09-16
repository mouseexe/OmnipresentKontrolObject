package gay.spiders

enum class Action(val description: String, val params: List<Param> = emptyList()) {
    REGISTER(
        description = "Register as a player.",
        params = listOf(
            Param(
                name = "user",
                description = "User to register",
                type = Param.Type.USER
            )
        )
    ),
    BALANCE("Check your current balances."),
    LIFESTYLE(
        description = "Adjust your lifestyle.",
        params = listOf(
            Param(
                name = "lifestyle",
                description = "Lifestyle to adopt.",
                type = Param.Type.STRING,
                options = Player.Lifestyle.entries.map { it.name.lowercase() },
                required = true
            )
        )
    ),
    TRANSFER(
        description = "Transfer funds.",
        params = listOf(
            Param(
                name = "recipient",
                description = "Receiver of funds.",
                type = Param.Type.USER,
                required = true
            ),
            Param(
                name = "account",
                description = "Account to pull from.",
                type = Param.Type.STRING,
                options = Player.Account.entries.map { it.name.lowercase() },
                required = true
            ),
            Param(
                name = "amount",
                description = "Amount of funds.",
                type = Param.Type.INTEGER,
                required = true
            )
        )
    );

    companion object {
        fun get(action: String) = entries.first { it.name.lowercase() == action }
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
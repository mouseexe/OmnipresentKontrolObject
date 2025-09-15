package gay.spiders

data class Player(
    val id: String,
    val credits: Int,
    val tokens: Int,
    val shares: Int,
    val factions: List<String>
)

enum class Actions(val description: String) {
    REGISTER("Register as a player."),
    BALANCE("Check your current balances."),
    RENT("Pay your rent.");

    companion object {
        fun get(action: String) = entries.first { it.name.lowercase() == action }
    }
}
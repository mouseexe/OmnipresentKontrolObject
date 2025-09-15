package gay.spiders

data class Player(
    val id: String,
    val credits: Int,
    val tokens: Int,
    val shares: Int,
    val factions: List<String>
)
package gay.spiders.data

data class Player(
    val username: String,
    val userId: Long,
    val admin: Boolean,
    val credits: Int,
    val tokens: Int,
    val shares: Int,
    val alive: Boolean,
    val lifestyle: Lifestyle,
    val bratva: Bratva?,
    val local: Local?,
    val solarian: Solarian?,
    val tempest: Tempest?,
    val canyonheavy: Canyonheavy?,
    val stratemeyer: Stratemeyer?
) {
    enum class Account {
        CREDITS, TOKENS, SHARES;

        companion object {
            fun get(account: String) = entries.first { it.name.lowercase() == account }
        }
    }

    enum class Lifestyle(val cost: Int) {
        Squalor(1), Borderline(20), Citizen(50), Decadent(200), Luxurious(1000);

        companion object {
            fun get(lifestyle: String) = entries.first { it.name.equals(lifestyle, ignoreCase = true) }
            fun affordableLifestyle(balance: Int) = entries.lastOrNull { it.cost <= balance }
        }
    }

    fun isMember(faction: Faction) = when (faction) {
        is Bratva -> bratva != null
        is Local -> local != null
        is Solarian -> solarian != null
        is Tempest -> tempest != null
        is Canyonheavy -> canyonheavy != null
        is Stratemeyer -> stratemeyer != null
    }

    fun factionString(): String {
        val activeFactions = listOfNotNull(
            bratva?.let { FactionType.Bratva.displayName to it.rank },
            local?.let { FactionType.Local.displayName to it.rank },
            solarian?.let { FactionType.Solarian.displayName to it.rank },
            tempest?.let { FactionType.Tempest.displayName to it.rank },
            canyonheavy?.let { FactionType.Canyonheavy.displayName to it.rank },
            stratemeyer?.let { FactionType.Stratemeyer.displayName to it.rank }
        )

        return if (activeFactions.isNotEmpty()) {
            "Factions: " + activeFactions.joinToString(", ") { (name, rank) -> "$name $rank" }
        } else {
            "Factions: None"
        }
    }
}

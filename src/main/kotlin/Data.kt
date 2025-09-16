package gay.spiders

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table() {
    val discordId = long("discord_id")
    val username = varchar("username", 32)
    val credits = integer("credits").default(0)
    val tokens = integer("tokens").default(0)
    val shares = integer("shares").default(0)
    val factions = varchar("factions", 255).default("").nullable()
    val lifestyle = varchar("lifestyle", 32).default("Citizen")
    val alive = bool("alive").default(true)


    override val primaryKey = PrimaryKey(discordId)

    fun Users.getPlayer(discordId: Long): Player? {
        var player: Player? = null
        transaction { player = Users.selectAll().where { Users.discordId eq discordId }.singleOrNull()?.toPlayer() }
        return player
    }

    fun ResultRow.toPlayer() = Player(
        username = this[username],
        userId = this[discordId],
        credits = this[credits],
        tokens = this[tokens],
        shares = this[shares],
        factions = this[factions]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        lifestyle = Player.Lifestyle.get(this[lifestyle]),
        alive = this[alive]
    )
}

data class Player(
    val username: String,
    val userId: Long,
    val credits: Int,
    val tokens: Int,
    val shares: Int,
    val factions: List<String>,
    val lifestyle: Lifestyle,
    val alive: Boolean
) {
    enum class Account {
        CREDITS, TOKENS, SHARES;

        companion object {
            fun get(account: String) = Account.entries.first { it.name.lowercase() == account }
        }
    }

    enum class Lifestyle(val cost: Int) {
        Squalor(1), Borderline(20), Citizen(50), Decadent(200), Luxurious(1000);

        companion object {
            fun get(lifestyle: String) = Lifestyle.entries.first { it.name.equals(lifestyle, ignoreCase = true) }
            fun affordableLifestyle(balance: Int) = Lifestyle.entries.lastOrNull { it.cost <= balance }
        }
    }
}

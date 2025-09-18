package gay.spiders.data

import gay.spiders.pgEnum
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 32)
    val discordId = long("discord_id").uniqueIndex()
    val admin = bool("admin").default(false)
    val alive = bool("alive").default(true)
    val credits = integer("credits").default(100)
    val tokens = integer("tokens").default(0)
    val shares = integer("shares").default(0)
    val lifestyle = pgEnum(Player.Lifestyle::class).default(Player.Lifestyle.Citizen)
    val bratva = pgEnum(Bratva::class).nullable()
    val local = pgEnum(Local::class).nullable()
    val solarian = pgEnum(Solarian::class).nullable()
    val tempest = pgEnum(Tempest::class).nullable()
    val canyonheavy = pgEnum(Canyonheavy::class).nullable()
    val stratemeyer = pgEnum(Stratemeyer::class).nullable()

    override val primaryKey = PrimaryKey(id)

    fun Users.getPlayer(discordId: Long): Player? {
        var player: Player? = null
        transaction { player = Users.selectAll().where { Users.discordId eq discordId }.singleOrNull()?.toPlayer() }
        return player
    }

    fun ResultRow.toPlayer() = Player(
        username = this[username],
        userId = this[discordId],
        admin = this[admin],
        alive = this[alive],
        credits = this[credits],
        tokens = this[tokens],
        shares = this[shares],
        lifestyle = this[lifestyle],
        bratva = this[bratva],
        local = this[local],
        solarian = this[solarian],
        tempest = this[tempest],
        canyonheavy = this[canyonheavy],
        stratemeyer = this[stratemeyer]
    )
}
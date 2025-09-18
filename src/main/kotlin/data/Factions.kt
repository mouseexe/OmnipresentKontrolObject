package gay.spiders.data

import kotlin.reflect.KClass

sealed interface Faction {
    val rank: String

    companion object {
        val factionTypeMap = FactionType.entries.associateBy { it.factionClass.simpleName }
    }
}

@Suppress("RemoveRedundantQualifierName")
enum class FactionType(val displayName: String, val factionClass: KClass<out Enum<*>>) {
    Bratva("Bratva", gay.spiders.data.Bratva::class),
    Local("Local", gay.spiders.data.Local::class),
    Solarian("Solarian", gay.spiders.data.Solarian::class),
    Tempest("Tempest", gay.spiders.data.Tempest::class),
    Canyonheavy("Canyonheavy", gay.spiders.data.Canyonheavy::class),
    Stratemeyer("Stratemeyer", gay.spiders.data.Stratemeyer::class)
}

enum class Bratva(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

enum class Local(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

enum class Solarian(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

enum class Tempest(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

enum class Canyonheavy(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

enum class Stratemeyer(override val rank: String) : Faction {
    Boss("Boss"), Member("Member")
}

fun getFaction(faction: String): Faction? = when (faction) {
    Bratva::class.simpleName -> Bratva.Member
    Local::class.simpleName -> Local.Member
    Solarian::class.simpleName -> Solarian.Member
    Tempest::class.simpleName -> Tempest.Member
    Canyonheavy::class.simpleName -> Canyonheavy.Member
    Stratemeyer::class.simpleName -> Stratemeyer.Member
    else -> null
}
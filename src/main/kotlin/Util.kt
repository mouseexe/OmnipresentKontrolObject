package gay.spiders

import dev.kord.core.cache.data.UserData

val UserData.discordId get() = this.id.value.toLong()
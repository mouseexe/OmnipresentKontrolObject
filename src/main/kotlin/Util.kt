package gay.spiders

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.cache.data.UserData
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import gay.spiders.data.Param
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

fun ChatInputCreateBuilder.addParameters(params: List<Param>) {
    params.forEach { param ->
        when (param.type) {
            Param.Type.STRING -> string(param.name, param.description) {
                required = param.required
                param.options?.forEach { choice(it.toString(), it.toString()) }
            }

            Param.Type.INTEGER -> integer(param.name, param.description) {
                required = param.required
                param.options?.forEach { choice(it.toString(), it.toString().toLong()) }
            }

            Param.Type.BOOLEAN -> boolean(param.name, param.description) { required = param.required }
            Param.Type.USER -> user(param.name, param.description) { required = param.required }
            Param.Type.CHANNEL -> channel(param.name, param.description) { required = param.required }
            Param.Type.ROLE -> role(param.name, param.description) { required = param.required }
        }
    }
}

val UserData.discordId get() = this.id.value.toLong()

inline fun <reified T : Enum<T>> Table.pgEnum(enumClass: KClass<T>) = customEnumeration(
    name = T::class.simpleName!!.lowercase(),
    sql = "CREATE TYPE ${T::class.simpleName!!.uppercase()} AS ENUM (${enumClass.java.enumConstants.joinToString { "'${it.name}'" }})",
    fromDb = { value -> enumClass.java.enumConstants.first { it.name == value } },
    toDb = { it.name }
)

fun <T> MutableList<T>.popLast(n: Int): List<T> {
    val count = n.coerceIn(0, this.size)
    if (count == 0) return emptyList()
    val fromIndex = this.size - count
    val takenElements = this.subList(fromIndex, this.size).toList()
    this.subList(fromIndex, this.size).clear()

    return takenElements
}

fun MessageBuilder.rematchButton() {
    actionRow {
        interactionButton(
            style = ButtonStyle.Primary, customId = "rematch"
        ) {
            label = "Play again"
        }
    }
}
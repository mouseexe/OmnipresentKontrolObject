package gay.spiders

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gay.spiders.data.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {
        Database.connect(createHikariDataSource())

        transaction {
            SchemaUtils.create(Users)
        }

    }

    private fun createHikariDataSource(): HikariDataSource {
        val dbHost = System.getenv("DB_HOST")
            ?: error("Error: DB_HOST environment variable not set...")
        val dbUser = System.getenv("DB_USER")
            ?: error("Error: DB_USER environment variable not set...")
        val dbPassword = System.getenv("POSTGRES_PASSWORD")
            ?: error("Error: POSTGRES_PASSWORD environment variable not set...")
        val dbName = System.getenv("DB_NAME")
            ?: error("Error: DB_NAME environment variable not set...")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://$dbHost:5432/$dbName"
            username = dbUser
            password = dbPassword
            maximumPoolSize = 10
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}
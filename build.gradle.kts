plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    alias(libs.plugins.shadow)
}

group = "gay.spiders"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.kotlin.test)
    implementation(libs.kord.core)
    implementation(libs.logback.classic)
    implementation(libs.bundles.exposed)
    implementation(libs.postgresql.jdbc)
    implementation(libs.hikaricp)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    // Requires Java 11 for OKD4
    jvmToolchain(11)
}

application {
    mainClass.set("gay.spiders.MainKt")
}
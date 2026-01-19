plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.ktor.plugin") version "3.2.2"
    id("com.gradleup.shadow") version "8.3.9"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val mcpVersion = "0.8.1"
val slf4jVersion = "2.0.9"
val anthropicVersion = "2.11.1"
val junitVersion = "5.12.0"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    implementation("com.anthropic:anthropic-java:${anthropicVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "org.example.client.McpClientKt"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
}

val shadowJarClient = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarClient") {
    group = "shadow"
    description = "Builds a shadow jar for the McpClient"
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Main-Class"] = "org.example.client.McpClientKt"
    }
    archiveBaseName.set("McpClient")
}

val shadowJarServer = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarServer") {
    group = "shadow"
    description = "Builds a shadow jar for the McpServer"
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Main-Class"] = "org.example.server.McpServerKt"
    }
    archiveBaseName.set("McpServer")
}

tasks.build {
    dependsOn(shadowJarClient, shadowJarServer)
}
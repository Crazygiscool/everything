import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    java
    id("com.gradleup.shadow") version "9.4.2"
}

group = "me.crazyg"
version = "1.6.10"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases")
    maven("https://repo.luckperms.net/") {
        content { includeGroup("net.luckperms") }
    }
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.20")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "version" to project.version.toString(),
                "name" to project.name
            )
        )
    }
}

fun resolveBuildNumber(): String {
    val localFile = rootProject.file("buildNumber.properties")
    if (localFile.exists()) {
        val props = Properties()
        FileInputStream(localFile).use { props.load(it) }
        return props.getProperty("buildNumber", "1")
    }
    return project.findProperty("buildNumber")?.toString() ?: "1"
}

fun incrementBuildNumber(current: String) {
    val next = (current.toIntOrNull() ?: 1) + 1
    rootProject.file("buildNumber.properties").writeText("buildNumber=$next\n")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("${project.name}-${project.version}-unshaded.jar")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    val buildNumber = resolveBuildNumber()
    archiveFileName.set("${project.name}-${project.version}-b${buildNumber}.jar")
    relocate("net.kyori", "me.crazyg.everything.libs.kyori")
    relocate("com.google.gson", "me.crazyg.everything.libs.gson")
    doLast {
        incrementBuildNumber(buildNumber)
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.register("printCp") {
    doLast {
        println(sourceSets["main"].compileClasspath.asPath)
    }
}

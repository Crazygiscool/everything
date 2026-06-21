import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    java
}

group = "me.crazyg"
version = "1.5.22"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.helpch.at/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
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
    val buildNumber = resolveBuildNumber()
    archiveFileName.set("${project.name}-${project.version}-b${buildNumber}.jar")
    doLast {
        incrementBuildNumber(buildNumber)
    }
}

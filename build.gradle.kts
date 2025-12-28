plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "me.crazyg"
version = "1.3.4"

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
    // Paperweight dev bundle
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // Only bStats goes into the shaded jar
    add("shade", "org.bstats:bstats-bukkit:3.1.0")
}

// Create a dedicated shading configuration
configurations {
    create("shade")
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

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("everything")
    archiveClassifier.set("")

    // Shadow ONLY the "shade" configuration
    configurations = listOf(project.configurations.getByName("shade"))

    // Relocate bStats
    relocate("org.bstats", "${project.group}.bstats")
}

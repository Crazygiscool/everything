plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "me.crazyg"
version = "1.2.0"

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
    // ✅ Correct Paperweight userdev dependency
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.shadowJar {
    archiveBaseName.set("everything")
    archiveClassifier.set("")
}

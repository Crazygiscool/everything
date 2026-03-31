import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipOutputStream

plugins {
    java
}

group = "me.crazyg"
version = "1.5.18"

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

val shadedDeps = configurations.create("shadedDeps")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    
    shadedDeps("com.google.code.gson:gson:2.10.1")
    shadedDeps("net.kyori:adventure-text-minimessage:4.26.1")
    shadedDeps("net.kyori:adventure-text-serializer-legacy:4.26.1")
    shadedDeps("net.kyori:adventure-api:4.26.1")
    shadedDeps("net.kyori:adventure-nbt:4.26.1")
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

tasks.register<Jar>("shadedJar") {
    archiveFileName.set("${project.name}.jar")
    dependsOn(tasks.classes, shadedDeps)
    
    doLast {
        val outputFile = archiveFile.get().asFile
        val tempFile = File(outputFile.parent, outputFile.name + ".tmp")
        
        ZipOutputStream(FileOutputStream(tempFile)).use { zipOut: ZipOutputStream ->
            // Add all compiled classes
            File("${layout.buildDirectory.get()}/classes/java/main").walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "class") {
                    val entryName = file.relativeTo(File("${layout.buildDirectory.get()}/classes/java/main")).path.replace("\\", "/")
                    zipOut.putNextEntry(JarEntry(entryName))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
            
            // Add resources
            File("${layout.buildDirectory.get()}/resources/main").walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(File("${layout.buildDirectory.get()}/resources/main")).path.replace("\\", "/")
                    zipOut.putNextEntry(JarEntry(entryName))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
            
            // Add shaded dependencies
            shadedDeps.files.forEach { jarFile ->
                JarInputStream(FileInputStream(jarFile)).use { jarIn ->
                    var entry: JarEntry? = jarIn.nextJarEntry
                    while (entry != null) {
                        if (!entry.name.startsWith("META-INF/") && entry.name.endsWith(".class")) {
                            zipOut.putNextEntry(JarEntry(entry.name))
                            jarIn.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                        entry = jarIn.nextJarEntry
                    }
                }
            }
        }
        
        tempFile.renameTo(outputFile)
        println("Shaded JAR created: ${outputFile.absolutePath}")
    }
}

tasks.named<Jar>("jar") {
    archiveFileName.set("${project.name}-${version}.jar")
}

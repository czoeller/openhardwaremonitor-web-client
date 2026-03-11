plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    `java-library`
    `maven-publish`
}

group = "de.czoeller"

val releaseVersion = providers.gradleProperty("releaseVersion").orNull
    ?: providers.environmentVariable("GITHUB_REF_NAME").orNull
        ?.takeIf { providers.environmentVariable("GITHUB_EVENT_NAME").orNull == "release" }

version = releaseVersion ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        register<MavenPublication>("java") {
            from(components["java"])
        }
    }
}

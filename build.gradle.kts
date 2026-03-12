plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("org.jetbrains.dokka") version "2.1.0"
    id("org.jetbrains.dokka-javadoc") version "2.1.0"
    `java-library`
    `maven-publish`
}

group = providers.gradleProperty("group").orNull ?: "com.github.czoeller"

val releaseVersion = providers.gradleProperty("version").orNull
    ?: providers.gradleProperty("releaseVersion").orNull
    ?: providers.environmentVariable("JITPACK_TAG").orNull
    ?: providers.environmentVariable("GITHUB_REF_NAME").orNull
        ?.removePrefix("refs/tags/")
        ?.takeIf { it.isNotBlank() }

version = releaseVersion ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
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

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaGeneratePublicationJavadoc"))
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

publishing {
    publications {
        register<MavenPublication>("java") {
            from(components["java"])
            artifact(javadocJar)
        }
    }
}

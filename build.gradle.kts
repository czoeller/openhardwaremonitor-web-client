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

val libraryArtifactId = "openhardwaremonitor-web-client"

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
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = libraryArtifactId

            pom {
                name.set("OpenHardwareMonitor Web Client")
                description.set("A JVM client for the OpenHardwareMonitor web server data.json endpoint.")
                url.set("https://github.com/czoeller/openhardwaremonitor-web-client")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("czoeller")
                        name.set("czoeller")
                        url.set("https://github.com/czoeller")
                    }
                }

                scm {
                    url.set("https://github.com/czoeller/openhardwaremonitor-web-client")
                    connection.set("scm:git:https://github.com/czoeller/openhardwaremonitor-web-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com/czoeller/openhardwaremonitor-web-client.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/czoeller/openhardwaremonitor-web-client")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "mogware.keyPressCounter"
version = "1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


tasks {
    withType<ShadowJar> {
        archiveFileName.set("keyPressCounterApplication.jar")

        manifest {
            attributes["Main-Class"] = "mogware.KeyPressCounterKt"
        }
    }
}
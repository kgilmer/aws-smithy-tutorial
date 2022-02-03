import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
}

group = "example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val smithyVersion = "1.16.3"
    api("software.amazon.smithy:smithy-codegen-core:$smithyVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
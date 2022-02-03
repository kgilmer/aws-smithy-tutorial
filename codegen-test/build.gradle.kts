plugins {
    id("software.amazon.smithy") version "0.5.3"
}

buildscript {
    val smithyVersion = "1.16.3"
    dependencies {
        classpath("software.amazon.smithy:smithy-model:$smithyVersion")
        classpath("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    }
}

group = "example"
version = "1.0-SNAPSHOT"

tasks["jar"].enabled = false

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":codegen"))
}

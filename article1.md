An intro to codegen with AWS Smithy
Smithy is an open source project created by AWS as a tool for modeling service APIs. It can be considered in the same general category as OpenAPI or Swagger in that APIs are described as models, and tools are then able to provide various functions against those models, such as documentation generation, test generation, and client and server code generation.
In this post we'll cover getting started with a Smithy codegen project in the Kotlin programming language to produce a basic "hello world" example.
Setup
Smithy is available as a set of Java libraries for parsing models, validating them, and of course performing codegen. We'll setup an empty Kotlin project in IntelliJ with the dependencies needed. A Smithy-based codegen is typically composed of at least two modules: the codegen implementation itself, and a dependent module that provides API models and settings to produce codegen output. As such, we will build a project with two modules. The root settings.gradle.kts :
rootProject.name = "example-codegen"

include("codegen", "codegen-test")
Then we'll create two directories from the root for each module: codegen, and codegen-test . Here are the build.gradle.kts files for the codegen module. Notice we depend on the artifact smithy-codegen-core which provides the Smithy code needed for a codegen project:
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
For the codegen-test module we depend on the codegen module and use Smithy's Gradle plugin to bootstrap our code generator into the Gradle build lifecycle:
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
Our project structure should look something like this now:
.
├── codegen
│  └── build.gradle.kts
│      
├── codegen-test
│   └── build.gradle.kts
├── gradle
│   └── ...
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
Let's dive into codegen now. In the codegen module we'll add a class that implements SmithyBuildPlugin :
package org.example.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
class CodegenPlugin : SmithyBuildPlugin {
override fun getName(): String = "example-codegen"

    override fun execute(context: PluginContext?) {
        // this is where the magic happens
    }
}
This is our entry point into a codegen session, as controlled by the Smithy Gradle plugin. The PluginContext in execute() will provide the API model and other types that are used in codegen. We need to declare our plugin in a file called software.amazon.smithy.build.SmithyBuildPlugin in the META-INF resource directory so that Smithy will load it at runtime:
# codegen/src/main/resources/META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin
org.example.smithy.CodegenPlugin
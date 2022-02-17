An intro to codegen with AWS Smithy — Setup
===========================================

[Smithy](https://github.com/awslabs/smithy) is an [open source project created and maintained by AWS](https://github.com/awslabs/smithy) as a tool for modeling service APIs. It can be considered in the same general category as [OpenAPI](https://www.openapis.org/) or [Swagger](https://swagger.io/) in that APIs are described as models, and tools are then able to provide various functions against those models, such as documentation generation, test generation, and client and server code generation. AWS has a lot of experience with very large scale web service systems, and Smithy encapsulates much of these learnings.

This is a series on writing code generators with [AWS Smithy](https://awslabs.github.io/smithy/). In this part (part one) we get a basic Kotlin project up that integrates with Smithy via Gradle. The source code referenced in this post is also available on GitHub here: [https://github.com/kgilmer/aws-smithy-tutorial](https://github.com/kgilmer/aws-smithy-tutorial)

_NOTE: This series is intended as an introduction to code generation with Smithy. As such the sample code is written concisely to get certain points across. Often this comes at the cost of a better, more complete design. The sample code here should not be taken as “best practice” for writing code generators, generally or for Smithy in particular._

![Photo by https://unsplash.com/@alvarocalvofoto](https://miro.medium.com/max/1400/1*VPK6QugYwubdGQ_4vHSoqQ.jpeg)

Smithy is available as a set of [Java libraries](https://mvnrepository.com/artifact/software.amazon.smithy) for parsing models, validating them, and of course generating code with them (codegen). We’ll setup an empty Kotlin project in IntelliJ with the dependencies needed. A Smithy-based codegen is typically composed of at least two modules: the codegen implementation itself, and a dependent module that provides [API model](https://awslabs.github.io/smithy/1.0/spec/core/model.html)s and settings to produce codegen output. As such, we will build a project with two modules. The root `settings.gradle.kts` :

```kotlin
rootProject.name = "example-codegen"

include("codegen", "codegen-test")
```

Then we’ll create two directories from the root for each module: `codegen`, and `codegen-test` . Here are the `build.gradle.kts` files for the `codegen` module. Notice we depend on the artifact `smithy-codegen-core` which provides the Smithy code needed for a codegen project:

```kotlin
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
```

For the `codegen-test` module we depend on the `codegen` module and use Smithy’s Gradle plugin to bootstrap our code generator into the Gradle build lifecycle:

```kotlin
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
```

Our project structure should look something like this now:

```
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
```

Let’s dive into codegen now. In the `codegen` module we’ll add a class that implements `SmithyBuildPlugin` :

```kotlin
class CodegenPlugin : SmithyBuildPlugin {
    override fun getName(): String = "example-codegen"

    override fun execute(context: PluginContext?) {
        // this is where the magic happens
    }
}
```

This is our entry point into a codegen session, as controlled by the Smithy Gradle plugin. The `PluginContext` in `execute()` will provide the API model and other types that are used in codegen. We need to declare our plugin in a file called software.amazon.smithy.build.SmithyBuildPlugin in the META-INF resource directory so that Smithy will load it at runtime:

`codegen/src/main/resources/META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin` :

```
org.example.smithy.CodegenPlugin
```

Notice that the package and class name of our plugin must match what we declare in the file.

In order to know if our plugin is working, let’s print all of the discovered services that Smithy finds at runtime when our codegen plugin is applied to a service model:

```kotlin
class CodegenPlugin : SmithyBuildPlugin {
    ...

    override fun execute(context: PluginContext?) {
        println("Hello  ${context?.model?.serviceShapes?.joinToString()}")
    }
}
```

That should be all that’s needed for our `codegen` plugin. Let’s setup our other module with a sample model to see our plugin run. We’ll use the [example Smithy model](https://awslabs.github.io/smithy/quickstart.html#weather-service) `weather.smithy` . By convention Smithy will automatically discover models in the `/models` directory of a module.

`/models/weather.smithy` :

```kotlin
namespace example.weather.simple

service Weather {
    version: "2006-03-01",
    operations: [GetCurrentTemp]
}

@readonly
@http(method: "GET", uri: "/?format", code: 200)
operation GetCurrentTemp {
    input: GetCurrentTempInput,
    output: GetCurrentTempOutput
}

structure GetCurrentTempInput {
    @httpQuery("format")
    @required
    format: String
}

structure GetCurrentTempOutput {
    @required
    temp: String
}
```

This is the Smithy model language IDL. You can learn more about it at the [Smithy website](https://awslabs.github.io/smithy/).

The last thing we need is a `smithy-build.json` file, which represents configuration that may be applied to a given codegen/model set:

`smithy-build.json` :

```json

{
  "version": "1.0",
  "plugins": {
    "example-codegen": {
      "service": "org.example.weather.simple#Weather",
      "module": "weather",
      "moduleVersion": "0.0.1"
    }
  }
}
```

Notice that the object under `plugins` must match the name our plugin returns in the `getName()` function.

And with that, we should be able to drive a Smithy codegen session. Our project should now look like this:

```
├── codegen  
│   ├── build.gradle.kts  
│   └── src  
│       ├── main  
│       │   ├── kotlin  
│       │   │   └── org  
│       │   │       └── example  
│       │   │           └── smithy  
│       │   │               └── CodegenPlugin.kt  
│       │   └── resources  
│       │       └── META-INF  
│       │           └── services  
│       │               └── software.amazon.smithy.build.SmithyBuildPlugin  
│       └── test  
│           └── kotlin  
├── codegen-test  
│   ├── build.gradle.kts  
│   ├── model  
│   │   └── weather.smithy  
│   └── smithy-build.json  
├── gradle  
│   └── ...  
├── gradle.properties  
├── gradlew  
├── gradlew.bat  
└── settings.gradle.kts
```

Let’s run the Gradle task `build` against `codegen-test` and see our new codegen plugin in action:

```
$ ./gradlew :codegen-test:build  
...  
Hello (service: \`example.weather.simple#Weather\`)  
...  
BUILD SUCCESSFUL in 2s
```

In the build output we can see that our plugin executed and printed the list of the single service found in our test `weather.smithy` model.

Summary
=======

In this first part introduction to Smithy, we’ve built a simple codegen plugin in Kotlin that integrates with the Smithy gradle plugin. This plugin is then called by Smithy to generate code from the model and setting inputs. I hope you enjoyed this quick introduction to service codegen with Smithy. This is just the tip of the iceberg and is simply the first step! As mentioned above, all this work is available in GitHub here: [https://github.com/kgilmer/aws-smithy-tutorial](https://github.com/kgilmer/aws-smithy-tutorial)

Next up: [Part II — GraphViz](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-2/README.md)

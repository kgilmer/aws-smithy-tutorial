This Gradle module represents the generation of an artifact based on:
* Codegen input parameters (/generator/smithy-build.json)
* A Smithy model (/generator/model/weather.smithy)
* A Codegen plugin (defined in /generator/build.gradle.kts as `implementation(project(":codegen"))`)

The codegen plugin that's specified in `smithy-build.json` is applied to the model.
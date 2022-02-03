This Gradle module represents the generation of an artifact based on:
* Codegen input parameters (/codegen-test/smithy-build.json)
* A Smithy model (/codegen-test/model/weather.smithy)
* A Codegen plugin (defined in /codegen-test/build.gradle.kts as `implementation(project(":codegen"))`)

The codegen plugin that's specified in `smithy-build.json` is applied to the model.
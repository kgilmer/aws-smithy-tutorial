package org.example.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin

class CodegenPlugin : SmithyBuildPlugin {
    override fun getName(): String = "example-codegen"

    override fun execute(context: PluginContext?) {
        println("I see ${context?.model?.serviceShapes?.joinToString()}")
    }
}
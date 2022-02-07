package org.example.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class CodegenPlugin : SmithyBuildPlugin {
    override fun getName(): String = "example-codegen"

    override fun execute(context: PluginContext?) {
        requireNotNull(context) { "Smithy PluginContext was unexpectedly null" }
        require(context.model.serviceShapes.size == 1) {
            "Can only generate a graph for a single service, but model contains ${context.model.serviceShapes.size} services."
        }

        generateDotDiagramForService(context.model)
    }

    private fun generateDotDiagramForService(model: Model) {
        val service = model.serviceShapes.first()

        println("""
            digraph D {
                subgraph cluster_service {
                    label = "${service.id.name}";

                    ${generateOperationContainer(model, service)}
                }
            }
        """.trimIndent())
    }

    private fun generateOperationContainer(model: Model, service: ServiceShape): String {
        return """
            subgraph cluster_operations {
            label = "Operations";

            ${service
                .allOperations
                .map {  generateOperation(model, it) }.joinToString("\n") { it }
            }
            }
        """.trimIndent()
    }

    private fun generateOperation(model: Model, operationShapeId: ShapeId): String {
        val operationShape = model.expectShape(operationShapeId, OperationShape::class.java)
        val inputShape = operationShape.input.orElseGet { null } ?: error("Expected input shape")
        val outputShape = operationShape.output.orElseGet { null } ?: error("Expected output shape")
        return """
            subgraph cluster_${operationShapeId.name}Operation {
                label = "${operationShapeId.name}";

                subgraph cluster_o1 {
                    label = "Inputs";

                    subgraph cluster_${inputShape.name} {
                        label = "${inputShape.name}";

                        ${generateMembers(model, inputShape)}
                    }
                }

                subgraph cluster_o2 {
                    label = "Outputs";

                    subgraph cluster_${outputShape.name} {
                        label = "${outputShape.name}";

                        ${generateMembers(model, inputShape)}
                    }
                }
            }
        """.trimIndent()
    }

    private fun generateMembers(model: Model, shapeId: ShapeId): String {
        val shape = model.expectShape(shapeId, StructureShape::class.java)

        return shape.members().joinToString(";\n") { it.id.name }
    }
}
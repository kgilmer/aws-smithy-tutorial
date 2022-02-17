
An intro to codegen with AWS Smithy — II GraphViz
=================================================

In the [first part](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-1/README.md) of this series, we covered setting up a Smithy codegen project such that the Smithy Gradle plugin calls into our codegen plugin when a model + settings are supplied into a codegen session. In the follow-up post, we’ll implement a simple codegen that produces a [GraphViz dot](https://graphviz.org/doc/info/lang.html) diagram that can then be used to generate an image if some aspects of a Smithy model. This work will demonstrate the basics of accessing model state from the Smithy APIs.

The code listed in this post is available on GitHub here: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-2](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-2)

GraphViz Generator Intro
========================

Implementing a codegen that produces a diagram is relatively simple in comparison to producing code for a programming language. There is less to express and the Dot language itself is simple. Our toy codegen will produce diagrams for Smithy entity types: service, operation, input, and output shapes. Before we get into the Smithy implementation, it is often constructive to implement a portion or all of an example of how we expect a codegen output to look. This makes it much simpler when doing the codegen implementation, as we can already test if a given codegen emission is correct by comparing it against our hand-written version. Without getting into the details of the Dot language, here is a handwritten example of the Weather example, which has been adapted from an existing Dot diagram:

```

digraph D {
    subgraph cluster_service {
        label = "Weather";

        subgraph cluster_operations {
            label = "Operations";

            subgraph cluster_GetCurrentTempOperation {
                label = "GetCurrentTemp";

                subgraph cluster_o1 {
                    label = "Inputs";

                    subgraph cluster_GetCurrentTempInput {
                        label = "GetCurrentTempInput";

                        format;
                    }
                }

                subgraph cluster_o2 {
                    label = "Outputs";

                    subgraph cluster_GetCurrentTempOutput {
                        label = "GetCurrentTempOutput";

                        temp;
                    }
                }
            }
        }
    }
}
```

Now that we have the output we wish to generate, the work is to simply implement the Smithy codegen plugin logic necessary to produce this string output given the input of the Weather model. We’ll store this hand-written model in the codegen project so that we can refer to it from unit tests in the future. It will go in codegen/src/test/resources/weather-by-hand.dot.

Dot Generation
==============

We’ll naively implement the codegen by first getting access to the Smithy Service via the Model instance provided by Smithy in our plugin. Once we have the service we can then use the Model API to get the operations, and other shapes associated with the service. Smithy offers other ways of accessing models including various indexes which are more powerful and convenient for real-world codegen plugins, however we’ll keep it simple here and just use simple APIs to get at our model entities.

We can augment our `execute()`function to query the model for a service. If we do not find a single service then we’ll fail. If we do find one, we’ll use functions to produce successive levels of detail as we traverse the model. Here is the updated plugin function `execute()` override:

```kotlin
override fun execute(context: PluginContext?) {
    requireNotNull(context) { "Smithy PluginContext was unexpectedly null" }
    require(context.model.serviceShapes.size == 1) {
        "Can only generate a graph for a single service, but model contains ${context.model.serviceShapes.size} services."
    }

    generateDotDiagramForService(context.model)
}
```

Now we can implement `generateDotDiagramForService(Model)` . We’ll first get a reference to the service, produce the top-level of the Dot program, and then call another function to codegen deeper model details:

```kotlin
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
```

In the above code we read from the model to generate the Dot label for the service, and then call another function to generate operations:

```kotlin
private fun generateOperationContainer(model: Model, service: ServiceShape) =
     """
        subgraph cluster_operations {
        label = "Operations";
        ${service
            .allOperations
            .map {  generateOperation(model, it) }.joinToString("\n") { it }
        }
        }
    """.trimIndent()
```

Here we use the model to read the operations associated with the service and then call additional functions to produce the Dot language output for each service. This is implemented like:

```kotlin
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
                    ${generateMembers(model, outputShape)}
                }
            }
        }
    """.trimIndent()
}
```

Again we use the Smithy model API to query for specific properties and write them into the Dot language strings. We use specific functions on the Operation type to get access to some of it’s associated members `input` and `output`. We descend from the operation into the inputs and outputs, but in this last function we simply print out the name of each member and terminate with a semicolon:

```kotlin
private fun generateMembers(model: Model, shapeId: ShapeId): String {
    val shape = model.expectShape(shapeId, StructureShape::class.java)

    return shape.members().joinToString(";\n") { it.id.name }
}
```

Here is the complete implementation of `CodegenPlugin`:

```kotlin
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

    private fun generateOperationContainer(model: Model, service: ServiceShape) =
         """
            subgraph cluster_operations {
            label = "Operations";
    
            ${service
                .allOperations
                .map {  generateOperation(model, it) }.joinToString("\n") { it }
            }
            }
        """.trimIndent()

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
                        ${generateMembers(model, outputShape)}
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
```

We can then run our `example-test` module’s build target from the command-line to now generate a GraphViz Dot diagram of our Weather model, printed directly to the console output (stdout):

```

$ ./gradlew :codegen-test:build
...
            digraph D {
                subgraph cluster_service {
                    label = "Weather";
subgraph cluster_operations {
            label = "Operations";
    
            subgraph cluster_GetCurrentTempOperation {
    label = "GetCurrentTemp";
subgraph cluster_o1 {
        label = "Inputs";
subgraph cluster_GetCurrentTempInput {
            label = "GetCurrentTempInput";
GetCurrentTempInput
        }
    }
subgraph cluster_o2 {
        label = "Outputs";
subgraph cluster_GetCurrentTempOutput {
            label = "GetCurrentTempOutput";
GetCurrentTempOutput
        }
    }
}
            }
                }
            }
...
BUILD SUCCESSFUL in 38s
5 actionable tasks: 4 executed, 1 up-to-date
```

The final test is to copy/paste the codegen output into a file (say `test.dot`) and then run the Dot compiler against it to produce a PostScript document:

```
$ dot -Tps test.dot -o test.ps
```

And then we can view the final document to verify that it worked:

![Smithy Weather sample as a GraphViz dot graph](https://miro.medium.com/max/1400/1*XEidjxA6bixUsnHgxKPiZg.png)

Summary
=======

In this post we’ve demonstrated how to use Smithy’s basic model APIs to produce a trivial codegen that produces a diagram of a service model. This example only scratches the surface of Smithy’s codegen capabilities. For example, you’ll notice that the output is not properly formatted, that the output is not written to files, and that we have done only simple queries against the model. In an upcoming post we’ll dig into a more realistic codegen example and produce some C++ entity types.

Next up: Part III — [C++ Entity Codegen](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-3/README.md)

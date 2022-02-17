An intro to codegen with AWS Smithy — Symbols
=============================================

This is the 4th part of a series on writing code generators with [AWS Smithy](https://awslabs.github.io/smithy/). In [part I](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-1/README.md) we get a basic project up that integrates with Smithy via Gradle. In [part II](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-2/README.md) we implement a simple codegen for the GraphViz Dot language. In [part III](https://github.com/kgilmer/aws-smithy-tutorial/blob/part-3/README.md) we implement another simple codegen for C++ entity types. In this post we’ll add some additional functionality into our C++ codegen that is important for a clean and maintainable codegen implementation.

NOTE: The code mentioned in this article is available on GitHub here: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4)

In part III of this series, we implemented entity type codegen in C++. The code to produce the C++ is littered with minutia such as capitalizing strings for use in class names, formatting parameter list expressions, and mapping Smithy [types](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes) to C++ types. Because this C++ knowledge is directly embedded in the code that produces strings, it becomes difficult to modify and maintain. Additionally, what if something provided by a model conflicts with a language keyword, for example a service called “class”? If we blindly write out whatever the model provides, we have no assurance that the output is valid. Ideally there would be a way to encapsulate the logic that “knows” about C++ in such a way that it could be changed once in a consistent way, and the rest of the codegen would just work without further modification. Smithy provides an abstraction exactly for this, called a [Symbol](https://awslabs.github.io/smithy/javadoc/1.8.0/software/amazon/smithy/codegen/core/Symbol.html).

Mapping Names
=============

A simple first thing we can clean up with Smithy Symbols is how C++ names are produced in our codegen. In Part III we simply embedded some string expressions directly. We’ll extract this logic into a Symbol, and use the [SymbolProvider](https://awslabs.github.io/smithy/javadoc/1.8.0/software/amazon/smithy/codegen/core/SymbolProvider.html) as the mapper between our model domain and our language domain. For the purposes of this post, we’ll add our `SymbolProvider` to our `CodegenPlugin` however you may wish to extract this as an independent type in a real codegen project.

```kotlin
class CodegenPlugin : SmithyBuildPlugin, ShapeVisitor<Unit> {

    companion object SymbolProvider : software.amazon.smithy.codegen.core.SymbolProvider {
        override fun toSymbol(shape: Shape): Symbol =
            when (shape.type) {
                ShapeType.STRUCTURE -> {
                    Symbol.builder()
                        .name(shape.id.name)
                        .build()
                }
                else -> error("Unhandled shape $shape")
            }
    }
    ...
}
```

Here we specify that the name of a `Structure` shape to be the `name` segment of it’s ID in Smithy. This is likely too simple for a real codegen project but is sufficient for our purposes here.

Now that we can retrieve a `Symbol` for our structure shapes, we can use it in the `HeaderGenerator` codegen:

```kotlin
fun generateEntityHeader(struct: StructureShape, writer: CppWriter) {
    val classSymbol = CodegenPlugin.toSymbol(struct)
    ...
    writer.write("class $classSymbol {")
    ...
}
```

With this change we’ve moved the logic that determines the name of a C++ class from the internals of the header emitter to the Symbol. We then refactor the remaining codegen to use our `Structure` symbol to determine the name of the C++ class.

Next let’s extract the knowledge that a Smithy `String` should map to a C++ `std::string` from the codegen emission functions into our `SymbolProvider`. For this to work we’ll need to get access to the `Model` instance from our `SymbolProvider` in order to, among other tasks, resolve a `Shape` from it’s id:

```kotlin
companion object SymbolProvider : software.amazon.smithy.codegen.core.SymbolProvider {
    private var pluginContext: PluginContext? = null;

    override fun toSymbol(shape: Shape): Symbol =
        when (shape.type) {
            ShapeType.STRUCTURE -> {
                Symbol.builder()
                    .name(shape.id.name)
                    .build()
            }
            ShapeType.STRING -> {
                Symbol.builder()
                    .name("std::string")
                    .build()
            }
            else -> error("Unhandled shape $shape")
        }

    fun toSymbol(shapeId: ShapeId): Symbol {
        val model = pluginContext!!.model
        return toSymbol(model.getShape(shapeId).orElseThrow())
    }
}
```

And we’ll need to set the value of `pluginContext` when we get access to it from the `execute(PluginContext)` function:

```kotlin

override fun execute(context: PluginContext?) {
    ...
    pluginContext = context
    ...
}
```

Finally we can remove the mapping from our codegen to the `SymbolProvider` in `HeaderGenerator.kt`:

```kotlin
// Return the CPP type of given shape
fun cppTypeForShape(shapeId: ShapeId): String =
    CodegenPlugin.toSymbol(shapeId).name
```

And with this change we now have a single place to provide knowledge about C++ that can be use in any codegen logic throughout our project.

Another detail of C++ we can move into our `SymbolProvider` is the notion of the source files themselves. In `CodegenPlugin` we have the following code:

```kotlin
override fun structureShape(struct: StructureShape?) {
    val structName = struct?.id?.name ?: error("Unexpected null shape")

    val headerFile = "$structName.h"
    ...
}
```

Smithy `Symbol`s already have the notion of a source file. We can augment our `toSymbol()` function in the case of `Structure` to specify the names of the header and source files:

```kotlin
ShapeType.STRUCTURE -> {
    Symbol.builder()
        .name(shape.id.name)
        .declarationFile("${shape.id.name}.h")
        .definitionFile("${shape.id.name}.cpp")
        .build()
}
```

Note: `Symbol` supports the ability to specify whatever state you may find useful via a `Map` of properties.

Then we can utilize this by getting the `Symbol` from our structures and resolving the source filenames from it:

```kotlin
override fun structureShape(struct: StructureShape?) {
    requireNotNull(struct)
    val symbol = toSymbol(struct)
    generateEntityHeader(struct, CppWriter.forFile(symbol.declarationFile))
    generateEntityCpp(struct, CppWriter.forFile(symbol.definitionFile))
}
```

The process of refactoring language-specific type information into `Symbol` s can continue until the functions that generate each source file are only concerned with the overall structure, and not the type information. An important note about `Symbol`s we won’t cover here is that they can also model dependency relationships. For example, notice how we statically emit the string `#include <iostream>` at the top of our `generateEntityHeader()` function. It would be better if our dependencies were modeled in our Symbols and the act of adding a Symbol to a codegen output automatically handled the necessary work to include any headers. Additionally, a superset of that information could be used to generate build files as well. We won’t go into these details here but keep in mind that Smithy provides functionality for complex aspects of codegen.

Summary
=======

In this post we learned about Smithy’s `Symbol` abstraction, and how we can use it to extract language-specific information out of various codegen functions into a single place. The code from this post is available at: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4)


Next Steps
==========

In these four parts, we've learned about AWS Smithy and written a few simple codegen projects.  There is much more to Smithy than that can be covered here.  Items worthy of investigation are: [Indexes](https://awslabs.github.io/smithy/javadoc/1.0.6/software/amazon/smithy/model/knowledge/package-summary.html) (convienent ways of accessing Smithy model state), [traits](https://awslabs.github.io/smithy/1.0/spec/core/model.html#traits) which allow for model elements to be decorated with additional information, and [SymbolDependency](https://awslabs.github.io/smithy/javadoc/1.8.0/software/amazon/smithy/codegen/core/SymbolDependency.html) which further increases the capabilities of Symbols to provide dependency information.
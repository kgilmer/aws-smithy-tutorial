package org.example.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.shapes.BigDecimalShape
import software.amazon.smithy.model.shapes.BigIntegerShape
import software.amazon.smithy.model.shapes.BlobShape
import software.amazon.smithy.model.shapes.BooleanShape
import software.amazon.smithy.model.shapes.ByteShape
import software.amazon.smithy.model.shapes.DocumentShape
import software.amazon.smithy.model.shapes.DoubleShape
import software.amazon.smithy.model.shapes.FloatShape
import software.amazon.smithy.model.shapes.IntegerShape
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.LongShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ResourceShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.SetShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ShapeType
import software.amazon.smithy.model.shapes.ShapeVisitor
import software.amazon.smithy.model.shapes.ShortShape
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.TimestampShape
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.transform.ModelTransformer

class CodegenPlugin : SmithyBuildPlugin, ShapeVisitor<Unit> {

    companion object SymbolProvider : software.amazon.smithy.codegen.core.SymbolProvider {
        private var pluginContext: PluginContext? = null;

        override fun toSymbol(shape: Shape): Symbol =
            when (shape.type) {
                ShapeType.STRUCTURE -> {
                    Symbol.builder()
                        .name(shape.id.name)
                        .declarationFile("${shape.id.name}.h")
                        .definitionFile("${shape.id.name}.cpp")
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

    override fun getName(): String = "example-codegen"

    override fun execute(context: PluginContext?) {
        requireNotNull(context) { "Smithy PluginContext was unexpectedly null" }
        require(context.model.serviceShapes.size == 1) {
            "Can only generate a graph for a single service, but model contains ${context.model.serviceShapes.size} services."
        }

        pluginContext = context
        val modelWithoutTraits = ModelTransformer.create().getModelWithoutTraitShapes(context.model)
        val shapesInService = Walker(modelWithoutTraits).walkShapes(context.model.serviceShapes.first())
        shapesInService.forEach { it.accept(this) } // cause [ShapeVisitor] functions to be called

        CppWriter.flushAll(context.fileManifest) // cause writer contents to be written to disk
    }

    override fun blobShape(p0: BlobShape?) {
        println("I see $p0")
    }

    override fun booleanShape(p0: BooleanShape?) {
        println("I see $p0")
    }

    override fun listShape(p0: ListShape?) {
        println("I see $p0")
    }

    override fun setShape(p0: SetShape?) {
        println("I see $p0")
    }

    override fun mapShape(p0: MapShape?) {
        println("I see $p0")
    }

    override fun byteShape(p0: ByteShape?) {
        println("I see $p0")
    }

    override fun shortShape(p0: ShortShape?) {
        println("I see $p0")
    }

    override fun integerShape(p0: IntegerShape?) {
        println("I see $p0")
    }

    override fun longShape(p0: LongShape?) {
        println("I see $p0")
    }

    override fun floatShape(p0: FloatShape?) {
        println("I see $p0")
    }

    override fun documentShape(p0: DocumentShape?) {
        println("I see $p0")
    }

    override fun doubleShape(p0: DoubleShape?) {
        println("I see $p0")
    }

    override fun bigIntegerShape(p0: BigIntegerShape?) {
        println("I see $p0")
    }

    override fun bigDecimalShape(p0: BigDecimalShape?) {
        println("I see $p0")
    }

    override fun operationShape(p0: OperationShape?) {
        println("I see $p0")
    }

    override fun resourceShape(p0: ResourceShape?) {
        println("I see $p0")
    }

    override fun serviceShape(p0: ServiceShape?) {
        println("I see $p0")
    }

    override fun stringShape(p0: StringShape?) {
        println("I see $p0")
    }

    override fun structureShape(struct: StructureShape?) {
        requireNotNull(struct)
        val symbol = toSymbol(struct)
        generateEntityHeader(struct, CppWriter.forFile(symbol.declarationFile))
        generateEntityCpp(struct, CppWriter.forFile(symbol.definitionFile))
    }

    override fun unionShape(p0: UnionShape?) {
        println("I see $p0")
    }

    override fun memberShape(p0: MemberShape?) {
        println("I see $p0")
    }

    override fun timestampShape(p0: TimestampShape?) {
        println("I see $p0")
    }
}
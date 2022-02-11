package org.example.smithy

import software.amazon.smithy.model.shapes.StructureShape

fun generateEntityCpp(struct: StructureShape, writer: CppWriter) {
    val classSymbol = CodegenPlugin.toSymbol(struct)
    writer.write("""#include "$classSymbol.h"""")
    writer.write("")

    // Generate constructor implementation
    writer.write("$classSymbol::$classSymbol(${generateMembersAsParams(struct)}) : ${generateInitializer(struct)} { }")
    writer.write("")

    // Generate entity accessor implementations
    struct
        .members()
        .forEach { memberShape ->
            writer.write("${cppTypeForShape(memberShape.target)} $classSymbol::get${memberShape.memberName.capitalize()}() { return _${memberShape.memberName}; }")
            writer.write("void $classSymbol::set${memberShape.memberName.capitalize()}(${cppTypeForShape(memberShape.target)} ${memberShape.memberName}) { _${memberShape.memberName} = ${memberShape.memberName};  } ")
        }
}

fun generateInitializer(struct: StructureShape): String =
    struct
        .members()
        .joinToString(separator = ",") { memberShape -> "_${memberShape.memberName} { ${memberShape.memberName} }" }

package org.example.smithy

import software.amazon.smithy.model.shapes.StructureShape

fun generateEntityCpp(struct: StructureShape, writer: CppWriter) {
    writer.write("""#include "${struct.id.name}.h"""")
    writer.write("")

    // Generate constructor implementation
    writer.write("${struct.id.name}::${struct.id.name}(${generateMembersAsParams(struct)}) : ${generateInitializer(struct)} { }")
    writer.write("")

    // Generate entity accessor implementations
    struct
        .members()
        .forEach { memberShape ->
            writer.write("${cppTypeForShape(memberShape.target)} ${struct.id.name}::get${memberShape.memberName.capitalize()}() { return _${memberShape.memberName}; }")
            writer.write("void ${struct.id.name}::set${memberShape.memberName.capitalize()}(${cppTypeForShape(memberShape.target)} ${memberShape.memberName}) { _${memberShape.memberName} = ${memberShape.memberName};  } ")
        }
}

fun generateInitializer(struct: StructureShape): String =
    struct
        .members()
        .joinToString(separator = ",") { memberShape -> "_${memberShape.memberName} { ${memberShape.memberName} }" }


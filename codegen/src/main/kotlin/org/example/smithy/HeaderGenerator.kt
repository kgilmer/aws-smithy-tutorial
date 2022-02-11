package org.example.smithy

import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

// Generate an entity type header
fun generateEntityHeader(struct: StructureShape, writer: CppWriter) {
    val className = struct.id.name

    writer.write("#include <iostream>")
    writer.write("")
    writer.write("class $className {")
    writer.write("public:")
    writer.indent()
    writer.write("$className(${generateMembersAsParams(struct)});")
    writer.write("")
    generateFieldAccessors(struct, writer)
    writer.dedent()

    writer.write("private:")
    writer.indent()
    generateMemberDeclarations(struct, writer)

    writer.dedent()
    writer.write("};")
}

// Generate the members of a structure as input parameters
fun generateMembersAsParams(struct: StructureShape): String =
    struct
        .members()
        .map { memberShape -> "${cppTypeForShape(memberShape.target)} ${memberShape.memberName}" }
        .joinToString(separator = ",") { it }

// Generate the functions to get and set values of members of the entity
private fun generateFieldAccessors(struct: StructureShape, writer: CppWriter) =
    struct
        .members()
        .forEach { memberShape ->
            val getterName = "get${memberShape.memberName.capitalize()}"
            val setterName = "set${memberShape.memberName.capitalize()}"

            writer.write("${cppTypeForShape(memberShape.target)} $getterName();")
            writer.write("void $setterName(${cppTypeForShape(memberShape.target)} ${memberShape.memberName});")
        }

// Generate the private variables used to store the entity's members
private fun generateMemberDeclarations(struct: StructureShape, writer: CppWriter) =
    struct
        .members()
        .forEach { memberShape ->
            writer.write("${cppTypeForShape(memberShape.target)} _${memberShape.memberName};")
        }

// Return the CPP type of given shape
fun cppTypeForShape(shapeId: ShapeId): String =
    when (shapeId.name) {
        "String" -> "std::string"
        else -> error("Unhandled $shapeId")
    }
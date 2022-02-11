package org.example.smithy

import software.amazon.smithy.build.FileManifest
import software.amazon.smithy.utils.CodeWriter

class CppWriter : CodeWriter() {

    init {
        trimBlankLines()
        trimTrailingSpaces()
        setIndentText("    ")
    }

    companion object Writers {
        private val writerByFileMap = mutableMapOf<String, CppWriter>()

        fun forFile(file: String) : CppWriter {
            if (!writerByFileMap.containsKey(file)) writerByFileMap[file] = CppWriter()

            return writerByFileMap[file]!!
        }

        fun flushAll(manifest: FileManifest) {
            writerByFileMap.forEach { (filename, writer) ->
                val fileContents = writer.toString()
                manifest.writeFile(filename, fileContents)
            }
        }
    }
}
package com.example.datagripjson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

object JsonFormatter {
    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    private val fieldLineRegex = Regex("""^(\s*)"((?:\\.|[^"\\])*)"\s*:\s*(.+?)(,?)\s*$""")

    fun format(
        rowData: LinkedHashMap<String, Any?>,
        comments: Map<String, String>,
        showComments: Boolean
    ): String {
        val json = mapper.writeValueAsString(rowData)
        if (!showComments || comments.isEmpty()) {
            return json
        }

        val lines = json.lines()
        return lines.joinToString(separator = "\n") { line ->
            val match = fieldLineRegex.matchEntire(line) ?: return@joinToString line
            val fieldName = match.groupValues[2]
            val comment = comments[fieldName]?.trim().orEmpty()
            if (comment.isBlank()) {
                return@joinToString line
            }
            val inlineComment = comment.replace("\r", " ").replace("\n", " ")
            val indent = match.groupValues[1]
            "$indent// $inlineComment\n$line"
        }
    }
}

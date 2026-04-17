package com.example.datagripjson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

object JsonFormatter {
    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    fun format(rowData: LinkedHashMap<String, Any?>): String {
        return mapper.writeValueAsString(rowData)
    }
}

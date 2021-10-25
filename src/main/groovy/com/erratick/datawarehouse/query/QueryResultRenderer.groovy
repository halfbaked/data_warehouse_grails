package com.erratick.datawarehouse.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import grails.rest.render.*
import grails.web.mime.MimeType

import java.text.SimpleDateFormat

/**
 * The standard Grails renderer had difficulty rendering the data list, which is a list of lists of different types.
 * Hence, the trusty Jackson library was employed to take is place when rendering QueryResult.
 */
class QueryResultRenderer extends AbstractRenderer<QueryResult> {
    QueryResultRenderer() {
        super(QueryResult, [MimeType.JSON,MimeType.JSON_API] as MimeType[])
    }

    void render(QueryResult object, RenderContext context) {
        context.contentType = MimeType.JSON.name
        ObjectMapper objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JavaTimeModule())
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))

        SimpleModule module = new SimpleModule()
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer())
        objectMapper.registerModule(module)

        context.writer.write(objectMapper.writer().writeValueAsString(object))
    }
}

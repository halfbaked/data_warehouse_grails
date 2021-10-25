package com.erratick.datawarehouse


import com.erratick.datawarehouse.query.Query
import com.erratick.datawarehouse.query.QueryResult
import com.erratick.datawarehouse.query.QueryService
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.CompileStatic
import org.springframework.http.HttpStatus

@CompileStatic
class QueryController {

    static responseFormats = ['json']

    QueryService queryService

    @Transactional
    def get() {
        Query query = new Query()
        bindData(query, params)
        if(query.hasErrors() || !query.validate()) throw new ValidationException("Invalid query", query.errors)
        QueryResult queryResult = queryService.runQuery(query)
        respond queryResult, status: HttpStatus.OK
    }

}

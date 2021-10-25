package com.erratick.datawarehouse.query

import groovy.transform.ToString

/**
 * Encapsulates the result of a query
 */
@ToString
class QueryResult {
    /**
     * The structure of the data depends on the query. The list of columns helps to describe that data to the client.
     */
    List<String> columns
    /**
     * The actual number crunched result data of the query.
     */
    List<List> data
}

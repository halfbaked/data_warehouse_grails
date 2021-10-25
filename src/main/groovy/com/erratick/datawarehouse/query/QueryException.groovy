package com.erratick.datawarehouse.query

class QueryException extends Exception {

    Query query

    QueryException(Query query, String msg){
        super(msg)
        query = query
    }

    QueryException(Query query, String msg, Throwable cause){
        super(msg, cause)
        query = query
    }
}

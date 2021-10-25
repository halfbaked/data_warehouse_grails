package com.erratick.datawarehouse

class UrlMappings {

    static mappings = {
        "/"(controller: "welcome")
        "/users"(controller: "user")
        "/query"(controller: "query")
        "/load"(controller: "load")
        "/virtualMetrics"(resources:'virtualMetric', includes:['save', 'index'])
        "/measuredMetrics"(resources: "measuredMetric", includes:['save', 'index'])
        "500"(controller: "error")
        "401"(view: "401")
        "404"(view:'/notFound')
    }
}

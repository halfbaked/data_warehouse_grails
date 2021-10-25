package com.erratick.datawarehouse.query

import com.erratick.datawarehouse.measurement.Measurement
import com.erratick.datawarehouse.measurement.metrics.Metric
import com.erratick.datawarehouse.measurement.metrics.VirtualMetric
import grails.gorm.transactions.Transactional


class QueryService {

    @Transactional
    Query saveQuery(Query query) {
        query.save(failOnError:true)
    }

    QueryResult runQuery(Query query){

        log.debug("runQuery $query")

        if(!query.metrics)
            throw new Exception("Cannot run a query where no metrics provided")

        Map<String, Object> queryParams = [:]
        String queryString = "SELECT "

        // Selections
        queryString += [
            query.metrics.collect { metric ->
                switch(metric){
                    case VirtualMetric:
                        return "${metric.formula}"
                    default:
                        return "SUM(${metric.name}.value)"
                }
            },
            query.groupBy.withIndex().collect { group, idx ->
                group == "date" ? "measurement.date" : "group_${idx}.value"
            }
        ].flatten().join(",")

        // Main table
        queryString += " FROM Measurement AS measurement "

        // Join Filters
        query.filterBy.eachWithIndex { filter, idx ->
            String tableName = "filters_${idx}"
            queryParams["filterValue_$idx"] = filter.value
            queryParams["filterKey_$idx"] = filter.key
            queryString += """
            INNER JOIN measurement.dimensionValues AS $tableName ON ${tableName}.value = :filterValue_$idx 
            INNER JOIN ${tableName}.dimension AS ${tableName}_dimension ON ${tableName}_dimension.name = :filterKey_$idx
            """
        }

        // Join Group Columns
        queryString += query.groupBy.withIndex().collect { group, idx ->
            if(group == "date") return ""
            String tableName = "group_$idx"
            queryParams[tableName] = group
            """
            LEFT OUTER JOIN measurement.dimensionValues as $tableName 
            INNER JOIN ${tableName}.dimension AS ${tableName}_dimension ON ${tableName}_dimension.name = :group_$idx
            """
        }.join(" ")

        // Join Field Columns - one for each metric
        List<Metric> requiredMetrics = query.metrics.collect {metric ->
            switch(metric){
                case VirtualMetric:
                    return metric.requiredMetrics
                default:
                    return metric
            }
        }.flatten().minus(null).toUnique{ it.id }
        queryString += requiredMetrics.collect { metric ->
            """
            LEFT OUTER JOIN measurement.fields as ${metric.name} 
            INNER JOIN ${metric.name}.metric AS ${metric.name}_metric ON ${metric.name}_metric.name='${metric.name}'
            """
        }.join(" ")

        // Date Range
        queryString += " WHERE measurement.date BETWEEN :rangeStart AND :rangeEnd "
        queryParams["rangeStart"] = query.rangeStart
        queryParams["rangeEnd"] = query.rangeEnd

        // Group By
        queryString += "GROUP BY " + query.groupBy.withIndex().collect { group, idx ->
            group == "date" ? "measurement.date" : "group_${idx}.value"
        }.join(", ")

        log.debug("Query HQL: \n $queryString")

        return new QueryResult(
            columns: query.metrics.collect{it.name } + query.groupBy,
            data: Measurement.executeQuery(queryString, queryParams)
        )
    }

}
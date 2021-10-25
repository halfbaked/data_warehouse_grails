package com.erratick.datawarehouse.query

import com.erratick.datawarehouse.measurement.Dimension
import com.erratick.datawarehouse.measurement.DimensionValue
import com.erratick.datawarehouse.measurement.metrics.Metric
import grails.databinding.BindUsing
import grails.validation.Validateable
import groovy.transform.ToString

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ToString(includeNames=true, includePackage=false, includes='metrics,filterBy,groupBy,rangeStart,rangeEnd')
class Query implements Validateable {
    private static final Instant NULL_INSTANT = Instant.EPOCH

    @BindUsing({ query, source ->
        if(source["metrics"] instanceof String) {
            return source["metrics"].toString().split(",").collect { metricName ->
                Metric.findByName(metricName)
            }
        } else {
            return source["metrics"]
        }
    })
    List<Metric> metrics = []

    @BindUsing({ query, source ->
        if(source["filterBy"]){
            if(source["filterBy"] instanceof String)
                return [:].tap { map ->
                    source["filterBy"].toString().split(",")
                        .collect { it.split(":") }
                        .each {keypair ->
                            map.put(keypair[0], keypair[1])
                    }
                }
            else
                return source["filterBy"]
        }
    })
    Map<String, String> filterBy = [:]

    @BindUsing({ query, source ->
        if(source["groupBy"]){
            if(source["groupBy"] instanceof String)
                return source["groupBy"].toString().split(",")
            else
                return source["groupBy"]
        }
    })
    List<String> groupBy = ["date"]

    LocalDate rangeStart = LocalDate.now().minus(7, ChronoUnit.DAYS)
    LocalDate rangeEnd = LocalDate.now()
    Instant createdAt = Instant.now()

    def beforeInsert() {
        if (createdAt == NULL_INSTANT) {
            createdAt = Instant.now()
        }
    }

    static constraints = {
        metrics validator: {metricList, obj, errors ->
            if(metricList.size() == 0){
                errors.rejectValue("metrics", "required")
            } else if (metricList.contains(null)){
                errors.rejectValue("metrics", "notFound")
            }
        }
        groupBy validator: { groupByList, obj, errors ->
            groupByList?.each {  group ->
                if(group != "date" && !Dimension.findByName(group))
                    errors.rejectValue("groupBy", group, "Invalid group $group")
            }
        }
        filterBy validator: {filterByMap, obj, errors ->
            filterByMap?.each { filter ->
                def dimension = Dimension.findByName(filter.key)
                if(!dimension) {
                    errors.rejectValue("filterBy", filter.key, "Invalid group ${filter.key}")
                } else if(!DimensionValue.findByDimensionAndValue(dimension, filter.value)) {
                    errors.rejectValue("filterBy", filter.value, "Invalid value ${filter.value}")
                }
            }
        }
    }

}

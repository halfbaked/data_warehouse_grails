package com.erratick.datawarehouse.measurement

import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import grails.databinding.BindUsing
import grails.validation.Validateable

class VirtualMetricCreate implements Validateable {

    String name
    String formula

    @BindUsing({ query, source ->
        if(source["requiredMetrics"] instanceof ArrayList) {
            return source["requiredMetrics"].collect { requiredMetric ->
                if(requiredMetric instanceof Number) {
                    return MeasuredMetric.get(requiredMetric)
                } else {
                    return requiredMetric
                }
            }
        } else {
            return source["requiredMetrics"]
        }
    })
    List<MeasuredMetric> requiredMetrics

    static constraints = {
        requiredMetrics validator: { metricList, obj, errors ->
            if (metricList.size() == 0) {
                errors.rejectValue("requiredMetrics", "required")
            } else if (metricList.contains(null)) {
                errors.rejectValue("requiredMetrics", "notFound")
            }
        }
    }
}

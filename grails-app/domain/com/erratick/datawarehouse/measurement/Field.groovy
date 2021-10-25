package com.erratick.datawarehouse.measurement

import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import groovy.transform.CompileStatic
import groovy.transform.ToString

@ToString(includes="metric,value", includeNames=true, includePackage=false)
@CompileStatic
class Field {
    static belongsTo = Measurement
    MeasuredMetric metric
    BigDecimal value
}

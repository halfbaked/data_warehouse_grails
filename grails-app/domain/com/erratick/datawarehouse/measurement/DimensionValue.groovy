package com.erratick.datawarehouse.measurement

import groovy.transform.CompileStatic

@CompileStatic
class DimensionValue {
    static hasMany = [ dataPoints: Measurement ]
    static belongsTo = Measurement
    Dimension dimension
    String value
}

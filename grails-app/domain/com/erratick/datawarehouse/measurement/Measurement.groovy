package com.erratick.datawarehouse.measurement

import groovy.transform.CompileStatic
import groovy.transform.ToString

import java.time.LocalDate

/**
 * Represents a measurement recorded, where a measurement can have one or more values (a field) where
 * each value is for a specific metric.
 */
@ToString(includeNames=true, includePackage=false)
@CompileStatic
class Measurement {
    static hasMany = [
        dimensionValues: DimensionValue,
        fields: Field
    ]
    LocalDate date
}

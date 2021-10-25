package com.erratick.datawarehouse.measurement

import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
class Dimension {
    static hasMany = [dimensionValues: DimensionValue]
    String name

    static constraints = {
        name unique: true
    }
}

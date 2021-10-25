package com.erratick.datawarehouse.measurement.metrics

import grails.compiler.GrailsCompileStatic
import groovy.transform.ToString

@ToString(includes="name", includePackage = false)
@GrailsCompileStatic
class Metric {
    String name

    static constraints = {
        name unique: true
    }
}
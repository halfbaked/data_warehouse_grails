package com.erratick.datawarehouse.measurement.metrics

import grails.compiler.GrailsCompileStatic
import groovy.transform.ToString

@GrailsCompileStatic
@ToString(includes="name", includePackage = false)
class MeasuredMetric extends Metric {}

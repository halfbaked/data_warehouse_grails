package com.erratick.datawarehouse.measurement.metrics

import grails.compiler.GrailsCompileStatic
import groovy.transform.ToString

/**
 * A virtual metric is not itself measured but calculated from metrics that are.
 * For example where a click-through-rate = clicks / impressions the click-through-rate is a virtual metric
 * calculated based on the measurements of the metrics clicks and impressions.
 */
@ToString(includes="name", includePackage = false)
@GrailsCompileStatic
class VirtualMetric extends Metric {

    static hasMany = [
        /**
         * The metrics required in order to  produce the virtual metric
         */
        requiredMetrics: MeasuredMetric
    ]

    /**
     * The formula used to calculate a value from the values of the measured metrics
     */
    String formula
}

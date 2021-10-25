package com.erratick.datawarehouse

import com.erratick.datawarehouse.measurement.VirtualMetricCreate
import com.erratick.datawarehouse.measurement.metrics.VirtualMetric
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.http.HttpStatus

@GrailsCompileStatic
class VirtualMetricController {

    @Transactional
    def save(VirtualMetricCreate cmd) {

        if(cmd.hasErrors() || !cmd.validate())
            throw new ValidationException("Invalid request to create virtual metric", cmd.errors)

        VirtualMetric virtualMetric = new VirtualMetric(
            name: cmd.name,
            formula: cmd.formula,
            requiredMetrics: cmd.requiredMetrics
        ).tap { save(flush:true) }
        respond virtualMetric, status: HttpStatus.CREATED
    }

    def index(){
        respond VirtualMetric.list()
    }

}

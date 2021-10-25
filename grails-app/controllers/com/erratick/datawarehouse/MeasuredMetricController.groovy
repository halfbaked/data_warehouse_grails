package com.erratick.datawarehouse

import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import grails.gorm.transactions.Transactional
import org.springframework.http.HttpStatus

class MeasuredMetricController {

    def index(){
        respond MeasuredMetric.list()
    }

    @Transactional
    def save(MeasuredMetric measuredMetric){
        measuredMetric.save(flush:true)
        respond measuredMetric, status: HttpStatus.CREATED
    }

}

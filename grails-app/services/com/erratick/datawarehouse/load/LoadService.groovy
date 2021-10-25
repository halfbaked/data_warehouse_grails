package com.erratick.datawarehouse.load

import com.erratick.datawarehouse.load.transformer.TransformerNotFoundException
import com.erratick.datawarehouse.load.transformer.Transformer
import com.erratick.datawarehouse.load.transformer.TransformerFactory
import com.erratick.datawarehouse.measurement.Dimension
import com.erratick.datawarehouse.measurement.DimensionValue
import com.erratick.datawarehouse.measurement.Field
import com.erratick.datawarehouse.measurement.Measurement
import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import com.erratick.datawarehouse.measurement.metrics.Metric
import grails.gorm.transactions.Transactional

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class LoadService {

    def sessionFactory

    /**
     * Loads the data into the system.
     * If the metrics specified do not already exist they are created.
     * The same applies for the dimensions.
     * @param data The data to load
     * @param metricNames List of the names of the metrics in this data
     * @param dimensions List of dimensions in this data
     * @param dateName The name to use to find the date for a measurement in the data provided
     */
    @Transactional
    def load(String contentType, String data, LoadConfig config){

        try {
            Transformer loader = TransformerFactory.buildLoader(contentType)
            List<Metric> metrics = config.metrics.collect { metricName ->
                MeasuredMetric.findOrCreateByName(metricName).tap { save(flush: true) }
            }
            List<Dimension> dimensions = config.dimensions.collect { dimensionName ->
                Dimension.findOrCreateByName(dimensionName).tap { save(flush: true) }
            }
            def dataMaps = loader.load(data)
            dataMaps.eachWithIndex { dataMap, index ->
                new Measurement(
                    fields: metrics.collect { metric ->
                        new Field(metric: metric, value: dataMap[metric.name]).tap { save() }
                    },
                    dimensionValues: dimensions.collect { dimension ->
                        new DimensionValue(dimension: dimension, value: dataMap[dimension.name]).tap { save() }
                    },
                    date: LocalDate.parse(dataMap[config.dateName], DateTimeFormatter.ofPattern(config.dateFormat))
                ).save()
                if(index % 100 == 0) {
                    sessionFactory.currentSession.with {
                        flush()
                        clear()
                    }
                }
            }
        } catch(DateTimeParseException e){
            throw new LoadContentInvalidException("Invalid date format")
        }
    }

}

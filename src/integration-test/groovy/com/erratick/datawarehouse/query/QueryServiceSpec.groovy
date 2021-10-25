package com.erratick.datawarehouse.query

import com.erratick.datawarehouse.measurement.Dimension
import com.erratick.datawarehouse.measurement.DimensionValue
import com.erratick.datawarehouse.measurement.Field
import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import com.erratick.datawarehouse.measurement.Measurement
import com.erratick.datawarehouse.measurement.metrics.VirtualMetric
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.*
import grails.gorm.transactions.*
import spock.lang.Specification

import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Integration
@Rollback
class QueryServiceSpec extends Specification {

    @Autowired
    QueryService dataPointService

    void "Filter by dimension"() {

        when: "there is appropriate test data"
        MeasuredMetric metric = MeasuredMetric.findOrCreateByName("clicks").tap {save(flush:true, failOnError:true)}
        Dimension dimension1 = Dimension.findOrCreateByName("Campaign").tap { save(flush:true, failOnError:true)}
        Dimension dimension2 = Dimension.findOrCreateByName("Datasource").tap { save(flush:true, failOnError:true)}
        [
            ["Campaign1", "Datasource1", 4],
            ["Campaign2", "Datasource1", 6],
            ["Campaign3", "Datasource2", 12]
        ].eachWithIndex { data, idx ->
            new Measurement(
                fields: [new Field(metric: metric, value: data[2])],
                date: LocalDate.now().minus(idx, ChronoUnit.DAYS),
                dimensionValues: [
                    DimensionValue.findOrCreateByDimensionAndValue(dimension1, data[0]),
                    DimensionValue.findOrCreateByDimensionAndValue(dimension2, data[1])
                ]
            ).save(flush:true, failOnError:true)
        }

        then: "no filter should give 3 results"
        3 == dataPointService.runQuery(
            new Query(metrics: [metric])
        ).data.size()

        then: "filtering by the common datasource should give 2 results"
        2 == dataPointService.runQuery(
            new Query(metrics: [metric], filterBy: ["Datasource": "Datasource1"])
        ).data.size()

        and: "filtering by the common datasource, but also the campaign should give 1 result"
        1 == dataPointService.runQuery(
            new Query(metrics: [metric], filterBy: ["Datasource": "Datasource1", "Campaign": "Campaign1"])
        ).data.size()
    }

    void "Test groupBy campaign"() {

        when: "there is appropriate test data"
        MeasuredMetric metric = MeasuredMetric.findOrCreateByName("clicks").tap {save(flush:true)}
        Dimension dimension1 = Dimension.findOrCreateByName("Campaign").tap { save(flush:true)}
        Dimension dimension2 = Dimension.findOrCreateByName("Datasource").tap { save(flush:true)}
        [
            ["Campaign1", "Datasource1", 5],
            ["Campaign2", "Datasource1", 10],
            ["Campaign2", "Datasource1", 20],
            ["Campaign2", "Datasource3", 25]
        ].each { data ->
            new Measurement(
                fields: [new Field(metric: metric, value: data[2])],
                date: LocalDate.now(),
                dimensionValues: [
                    DimensionValue.findOrCreateByDimensionAndValue(dimension1, data[0]),
                    DimensionValue.findOrCreateByDimensionAndValue(dimension2, data[1])
                ]
            ).save(flush:true, failOnError:true)
        }

        then: "Grouping by datasource yields expected group sizes and values"
        dataPointService.runQuery(
            new Query(metrics: [metric], groupBy: [dimension2.name])
        ).tap {
            assert data.size() == 2
        }.data.every { result ->
            [35, 25].any { expected -> expected == result[0] }
        }

        and: "Grouping by 2 dimensions (both datasource and campaign) yields expected group sizes and values"
        dataPointService.runQuery(
            new Query(metrics: [metric], groupBy: [dimension1.name, dimension2.name])
        ).tap {
            assert data.size() == 3
        }.data.every { result ->
            [5, 30, 25].any{ expected -> expected == result[0] }
        }

    }

    void "Restricting to date range reduces the result set"() {

        when: "there is appropriate test data going back 14 days"
        MeasuredMetric metric = new MeasuredMetric(name: "clicks").tap {save(flush:true, failOnError:true)}
        (1..14).each {
            new Measurement(
                fields: [new Field(metric: metric, value: it)],
                    date: LocalDate.now().minusDays(it),
            ).save(flush:true, failOnError:true)
        }

        then: "when we query the last 7 days we get 7 results"
        dataPointService.runQuery(
            new Query(
                metrics: [metric],
                rangeStart: LocalDate.now().minusDays(7),
                rangeEnd: LocalDate.now()
            )
        ).data.size() == 7

    }

    void "Virtual metric produces expected values"() {

        when: "there is appropriate test data"
        MeasuredMetric clicksMetric = new MeasuredMetric(name: "clicks").tap {save(flush:true, failOnError:true)}
        MeasuredMetric impressionsMetric = new MeasuredMetric(name: "impressions").tap {save(flush:true, failOnError:true)}
        VirtualMetric ctrMetric = new VirtualMetric(
            name: "ctr",
            formula: "clicks.value / impressions.value",
            requiredMetrics: [clicksMetric, impressionsMetric],
        ).tap {save(flush:true, failOnError:true)}

        (1..2).each {
            new Measurement(
                fields: [
                    new Field(metric: clicksMetric, value: it),
                    new Field(metric: impressionsMetric, value: it*20)
                ],
                date: LocalDate.now().minusDays(it),
            ).save(flush:true, failOnError:true)
        }

        then: "when we query for the ctr"
        dataPointService.runQuery(new Query(metrics: [ctrMetric])).with {
            assert data.size() == 2
            List expectedData = [[0.05], [0.05]]
            expectedData.eachWithIndex { row, rowIdx ->
                row.eachWithIndex { expectedVal, colIdx ->
                    assert data[rowIdx][colIdx] == expectedVal
                }
            }
        }

    }
}



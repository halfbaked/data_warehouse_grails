package com.erratick.datawarehouse.query

import com.erratick.datawarehouse.auth.SecurityRoleService
import com.erratick.datawarehouse.auth.SecurityRoles
import com.erratick.datawarehouse.auth.User
import com.erratick.datawarehouse.auth.UserSecurityRoleService
import com.erratick.datawarehouse.auth.UserService
import com.erratick.datawarehouse.measurement.metrics.VirtualMetric
import com.erratick.datawarehouse.measurement.Dimension
import com.erratick.datawarehouse.measurement.DimensionValue
import com.erratick.datawarehouse.measurement.Field
import com.erratick.datawarehouse.measurement.Measurement
import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import com.erratick.datawarehouse.utils.client.ClientHelper
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientException
import org.apache.commons.lang.RandomStringUtils
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@SuppressWarnings(['MethodName', 'DuplicateNumberLiteral', 'Instanceof'])
@Integration
class QueryControllerSpec extends Specification {

    static String QUERY_URI = "/query"

    @Shared
    @AutoCleanup
    HttpClient client
    @Shared
    User user
    @Shared
    List<Dimension> dimensions
    @Shared
    List<MeasuredMetric> measuredMetrics
    @Shared
    VirtualMetric virtualMetric
    @Shared
    List<Measurement> measurements

    UserService userService
    UserSecurityRoleService userSecurityRoleService
    SecurityRoleService securityRoleService

    @OnceBefore
    void init() {
        client  = HttpClient.create(new URL("http://localhost:$serverPort"))
        MeasuredMetric.withTransaction {
            user = new User(
                    username: RandomStringUtils.randomAlphabetic(20),
                    password: RandomStringUtils.randomAlphanumeric(20)).tap {
                userSecurityRoleService.save(
                    userService.save(it.username, it.password),
                    securityRoleService.findByAuthority(SecurityRoles.ADMIN)
                )
            }
            measuredMetrics = [0, 1].collect {
                new MeasuredMetric(name: RandomStringUtils.randomAlphabetic(20)).tap {
                    save(flush: true)
                }
            }
            virtualMetric = new VirtualMetric(
                name: RandomStringUtils.randomAlphabetic(20),
                formula: "SUM(${measuredMetrics[0].name}.value) / SUM(${measuredMetrics[1].name}.value)",
                requiredMetrics: measuredMetrics,
            ).tap {save(flush:true, failOnError:true) }
            dimensions = [0, 1].collect {
                new Dimension(name: RandomStringUtils.randomAlphabetic(10)).tap { save(flush: true) }
            }
            measurements = [
                ["Campaign1", "Datasource1", 7, 22425],
                ["Campaign2", "Datasource1", 1, 10],
                ["Campaign3", "Datasource2", 8, 20]
            ].withIndex().collect { sampleData, idx ->
                new Measurement(
                    fields: [
                        new Field(metric: measuredMetrics[0], value: sampleData[2]),
                        new Field(metric: measuredMetrics[1], value: sampleData[3])
                    ],
                    dimensionValues: [
                        DimensionValue.findOrCreateByDimensionAndValue(dimensions[0], sampleData[0]),
                        DimensionValue.findOrCreateByDimensionAndValue(dimensions[1], sampleData[1])
                    ],
                    date: LocalDate.now().minusDays(idx)
                ).save(flush:true, failOnError:true)
            }
        }
    }

    def "URI is secured"() {
        when:
        HttpRequest request = HttpRequest.POST(QUERY_URI, [:])
        client.toBlocking().exchange(request, String)

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNAUTHORIZED
    }

    def "simple query for a single metric"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${measuredMetrics[0].name}")
                .header("Authorization", "Bearer ${accessToken}"),
                QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert data.size() == measurements.size()
            assert columns.size() == 2
            assert columns[0] == measuredMetrics[0].name
            assert columns[1] == "date"
        }
    }

    def "simple query with date range"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        String rangeStart = LocalDate.now().minusDays(1).format(dateFormat)
        String rangeEnd = LocalDate.now().format(dateFormat)
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${measuredMetrics[0].name}&rangeStart=$rangeStart&rangeEnd=$rangeEnd")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and: "the range limitation reduced the number of results returned"
        rsp.body().with {
            assert data.size() == 2
        }
    }

    def "simple query fails for unknown metric"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=unknown")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    def "simple query for a virtual metric"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${virtualMetric.name}")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert columns.size() == 2
            assert columns[0] == virtualMetric.name
            assert columns[1] == "date"
            assert data.size() == measurements.size()
            columns.eachWithIndex{ def column, int i ->
                if(column == virtualMetric.name) {
                    assert data[0][i] == 0.4
                    assert data[1][i] == 0.1
                    assert data[2][i] == (7/BigDecimal.valueOf(22425)).setScale(6, RoundingMode.HALF_UP)
                }
            }
        }
    }

    def "group query for a virtual metric"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        println("Dimension name: ${dimensions[1].name.toString()}")
        println("Dimensions $dimensions")
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${virtualMetric.name}&groupBy=${dimensions[1].name}")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert columns.size() == 2
            assert columns[0] == virtualMetric.name
            assert columns[1] == dimensions[1].name
            assert data.size() == 2
            columns.eachWithIndex{ def column, int i ->
                if(column == virtualMetric.name) {
                    assert data[1][i] == 0.4
                    assert data[0][i] == (8/BigDecimal.valueOf(22435.0)).setScale(6, RoundingMode.HALF_UP)
                }
            }
        }
    }

    def "simple query for multiple metrics"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for 3 metrics'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${virtualMetric.name},${measuredMetrics[0].name},${measuredMetrics[1].name}")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert columns.size() == 4
            assert columns[0] == virtualMetric.name
            assert columns[1] == measuredMetrics[0].name
            assert columns[2] == measuredMetrics[1].name
            assert data.size() == measurements.size()
            List expectedData = [
                [0.4, 8, 20],
                [0.1, 1, 10],
                [(7/BigDecimal.valueOf(22425)).setScale(6, RoundingMode.HALF_UP), 7, 22425]
            ]
            expectedData.eachWithIndex { row, rowIdx ->
                row.eachWithIndex { expectedVal, colIdx ->
                    assert data[rowIdx][colIdx] == expectedVal
                }
            }
        }
    }

    def "query groupBy unknown dimension fails"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${measuredMetrics[1].name}&groupBy=unknown")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    def "query groupBy"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${measuredMetrics[1].name}&groupBy=${dimensions[1].name}")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert columns.size() == 2
            assert columns[0] == measuredMetrics[1].name
            assert columns[1] == dimensions[1].name
            assert data.size() == 2
            List expectedData = [[22435], [20]]
            expectedData.eachWithIndex { row, rowIdx ->
                row.eachWithIndex { expectedVal, colIdx ->
                    assert data[rowIdx][colIdx] == expectedVal
                }
            }
        }
    }

    def "query filterBy unknown dimension fails"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${QUERY_URI}?metrics=${measuredMetrics[1].name}&filterBy=unknown:unknown")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    def "query filterBy returns expected data"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric and valid filter'
        HttpResponse<QueryResult> rsp = client.toBlocking().exchange(
            HttpRequest
                .GET("${ QUERY_URI }?metrics=${ measuredMetrics[1].name}&" +
                    "filterBy=${dimensions[0].name}:" +
                    "${measurements[0].dimensionValues.find { it.dimension == dimensions[0] }.value}")
                .header("Authorization", "Bearer ${accessToken}"),
            QueryResult
        )

        then:
        rsp.status == HttpStatus.OK

        and:
        rsp.body().with {
            assert columns.size() == 2
            assert columns[0] == measuredMetrics[1].name
            assert columns[1] == "date"
            assert data.size() == 1
            List expectedData = [[22425]]
            expectedData.eachWithIndex { row, rowIdx ->
                row.eachWithIndex { expectedVal, colIdx ->
                    assert data[rowIdx][colIdx] == expectedVal
                }
            }
        }
    }

}
package com.erratick.datawarehouse.measurement

import com.erratick.datawarehouse.auth.*
import com.erratick.datawarehouse.measurement.metrics.MeasuredMetric
import com.erratick.datawarehouse.measurement.metrics.VirtualMetric
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

@SuppressWarnings(['MethodName', 'DuplicateNumberLiteral', 'Instanceof'])
@Integration
class VirtualMetricControllerSpec extends Specification {

    static String VIRTUAL_METRIC_URI = "/virtualMetrics"

    @Shared
    @AutoCleanup
    HttpClient client

    @Shared
    User user

    @Shared
    MeasuredMetric measuredMetric

    UserService userService
    UserSecurityRoleService userSecurityRoleService
    SecurityRoleService securityRoleService

    @OnceBefore
    void init() {
        client  = HttpClient.create(new URL("http://localhost:$serverPort"))
        User.withTransaction {
            user = new User(
                username: RandomStringUtils.randomAlphabetic(20),
                password: RandomStringUtils.randomAlphanumeric(20)
            ).tap {
                userSecurityRoleService.save(
                    userService.save(it.username, it.password),
                    securityRoleService.findByAuthority(SecurityRoles.ADMIN)
                )
            }

            measuredMetric = new MeasuredMetric(
                name: RandomStringUtils.randomAlphabetic(20)
            ).tap { save() }
        }
    }

    def 'uri is secured'() {
        when:
        HttpRequest request = HttpRequest.POST(VIRTUAL_METRIC_URI, [:])
        client.toBlocking().exchange(request, String)

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNAUTHORIZED
    }

    def "create virtual metric succeeds"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<VirtualMetric> rsp = client.toBlocking().exchange(
            HttpRequest.POST(
                "${VIRTUAL_METRIC_URI}",
                [
                    name: RandomStringUtils.randomAlphabetic(10),
                    formula: "xxx / yyyy",
                    requiredMetrics: [measuredMetric.id]
                ]
            ).header("Authorization", "Bearer ${accessToken}"),
            VirtualMetric
        )

        then:
        rsp.status.code == 201
    }

    def "create virtual metric fails when invalid required metric provided"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<VirtualMetric> rsp = client.toBlocking().exchange(
            HttpRequest.POST(
                "${VIRTUAL_METRIC_URI}",
                [
                    name: RandomStringUtils.randomAlphabetic(10),
                    formula: "xxx / yyy",
                    requiredMetrics: [500000L]
                ]
            ).header("Authorization", "Bearer ${accessToken}"),
            VirtualMetric
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

}
package com.erratick.datawarehouse.load

import com.erratick.datawarehouse.auth.*
import com.erratick.datawarehouse.measurement.Measurement
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

import java.time.LocalDate

@SuppressWarnings(['MethodName', 'DuplicateNumberLiteral', 'Instanceof'])
@Integration
class LoadControllerSpec extends Specification {

    static String LOAD_URI = "/load"

    @Shared
    @AutoCleanup
    HttpClient client
    @Shared
    User user

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
        }
    }

    def "URI is secured"() {
        when:
        HttpRequest request = HttpRequest.POST(LOAD_URI, [:])
        client.toBlocking().exchange(request, String)

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNAUTHORIZED
    }

    def "load fails with invalid content type"() {

        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send data with'
        HttpResponse<String> rsp = client.toBlocking().exchange(
            HttpRequest.POST(
                "${LOAD_URI}?metrics=Clicks,Impressions&dimensions=Campaigns,Datasource&dateName=Date&dateFormat=yyyy-MM-dd",
                """
                Plain Text
                """.trim().stripIndent()
            )
            .header("Authorization", "Bearer ${accessToken}")
            .header("Content-Type", "text/plain"),
            String
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    def "load fails with invalid date format"() {

        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send data with'
        HttpResponse<String> rsp = client.toBlocking().exchange(
            HttpRequest.POST(
                "${LOAD_URI}?metrics=Clicks,Impressions&dimensions=Campaigns,Datasource&dateName=Date&dateFormat=yyyy-MM-dd",
                """
                Date,Clicks,Impressions,Campaigns,Datasource
                23/10/2011,20,40,Christmas,Google
                """.trim().stripIndent()
            )
            .header("Authorization", "Bearer ${accessToken}")
            .header("Content-Type", "text/csv"),
            String
        )

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.BAD_REQUEST
    }

    def "load measurement data succeeds"() {
        when: 'login as user'
        String accessToken = ClientHelper.login(client, user).accessToken

        and: 'send query for one metric'
        HttpResponse<String> rsp = client.toBlocking().exchange(
            HttpRequest.POST(
                "${LOAD_URI}?metrics=Clicks,Impressions&dimensions=Campaigns,Datasource&dateName=Date&dateFormat=yyyy-MM-dd",
                """
                Date,Clicks,Impressions,Campaigns,Datasource
                2011-10-23,20,40,Christmas,Google
                2011-10-23,1,1564,Easter,Amazon
                2011-10-22,1,1564,Easter,Amazon
                """.trim().stripIndent()
            )
            .header("Authorization", "Bearer ${accessToken}")
            .header("Content-Type", "text/csv"),
            String
        )

        then:
        rsp.status == HttpStatus.OK

        and: "The measurements have been added to the database"
        Measurement.withTransaction {
            Measurement.findByDate(LocalDate.parse("2011-10-23")) != null
        }

    }

}
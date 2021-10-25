package com.erratick.datawarehouse.auth

import com.erratick.datawarehouse.utils.client.BearerToken
import com.erratick.datawarehouse.auth.model.UserCredentials

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import org.apache.commons.lang.RandomStringUtils
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@SuppressWarnings(['MethodName', 'DuplicateNumberLiteral', 'Instanceof'])
@Integration
class LoginControllerSpec extends Specification {

    static String LOGIN_URI = "/login"

    @Shared
    @AutoCleanup
    HttpClient client

    UserService userService
    UserSecurityRoleService userSecurityRoleService
    SecurityRoleService securityRoleService

    @OnceBefore
    void init() {
        client  = HttpClient.create(new URL("http://localhost:$serverPort"))
    }

    def 'test /login to create access token for an admin'() {
        when: 'there is a user'

        def user = new User(
            username: RandomStringUtils.randomAlphabetic(20),
            password: RandomStringUtils.randomAlphanumeric(20)
        ).tap {
            userSecurityRoleService.save(
                userService.save(it.username, it.password),
                securityRoleService.findByAuthority(SecurityRoles.ADMIN)
            )
        }

        and: 'POST to /login'
        HttpRequest request = HttpRequest.POST(
            LOGIN_URI,
            new UserCredentials(username: user.username, password: user.password)
        )
        HttpResponse<BearerToken> resp = client.toBlocking().exchange(request, BearerToken)

        then:
        resp.status.code == 200
        resp.body().roles.find { it == SecurityRoles.ADMIN }

        when:
        String accessToken = resp.body().accessToken

        then:
        accessToken
    }

}
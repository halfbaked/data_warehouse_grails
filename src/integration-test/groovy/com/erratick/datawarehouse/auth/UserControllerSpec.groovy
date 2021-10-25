package com.erratick.datawarehouse.auth

import com.erratick.datawarehouse.utils.client.BearerToken
import com.erratick.datawarehouse.auth.model.UserCreate
import com.erratick.datawarehouse.auth.model.UserCredentials

import grails.testing.mixin.integration.Integration
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientException
import org.apache.commons.lang.RandomStringUtils
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import grails.testing.spock.OnceBefore


@SuppressWarnings(['MethodName', 'DuplicateNumberLiteral', 'Instanceof'])
@Integration
class UserControllerSpec extends Specification {

    static String USER_URI = "/users"
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

    def 'test /users/ url is secured'() {
        when:
        HttpRequest request = HttpRequest.POST(USER_URI, [:])
        client.toBlocking().exchange(request, String)

        then:
        HttpClientException e = thrown(HttpClientException)
        e.response.status == HttpStatus.UNAUTHORIZED
    }

    def "test a user with the role ROLE_ADMIN is able to create a user"() {
        when: 'create admin user'
        User admin = createSampleUserWithRole(SecurityRoles.ADMIN)

        and: 'login as admin user'
        String accessToken = loginAsUser(admin).accessToken

        and: 'create new user request'
        HttpRequest rqst = HttpRequest.POST(
            USER_URI,
            new UserCreate(
                username: RandomStringUtils.randomAlphabetic(20),
                password: RandomStringUtils.randomAlphanumeric(20),
                role: SecurityRoles.ANALYST
            )
        ).header("Authorization", "Bearer ${accessToken}")
        HttpResponse<Map> rsp = client.toBlocking().exchange(rqst, Map)

        then:
        rsp.status.code == 201
    }

    def "test a user with the role ANALYST is NOT able to create a user"() {
        when: 'create analyst user'
        User analyst = createSampleUserWithRole(SecurityRoles.COLLECTOR)

        and: 'login as analyst user'
        String accessToken = loginAsUser(analyst).accessToken

        and: 'create new user request'
        client.toBlocking().exchange(
            HttpRequest.POST(
                USER_URI,
                new UserCreate(
                    username: RandomStringUtils.randomAlphabetic(20),
                    password: RandomStringUtils.randomAlphanumeric(20),
                    role: SecurityRoles.ANALYST
                )
            ).header("Authorization", "Bearer ${accessToken}"),
            Map
        )

        then:
        def e = thrown(HttpClientException)
        e.response.status == HttpStatus.FORBIDDEN
    }

    BearerToken loginAsUser(User user){
        UserCredentials credentials = new UserCredentials(username: user.username, password: user.password)
        HttpRequest request = HttpRequest.POST(LOGIN_URI, credentials)
        HttpResponse<BearerToken> resp = client.toBlocking().exchange(request, BearerToken)
        return resp.body()
    }

    User createSampleUserWithRole(String role){
        return new User(
                username: RandomStringUtils.randomAlphabetic(20),
                password: RandomStringUtils.randomAlphanumeric(20)).tap {
            userSecurityRoleService.save(
                userService.save(it.username, it.password),
                securityRoleService.findByAuthority(role)
            )
        }
    }

}
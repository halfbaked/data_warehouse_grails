package com.erratick.datawarehouse.utils.client

import com.erratick.datawarehouse.auth.User
import com.erratick.datawarehouse.auth.model.UserCredentials
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient

/**
 * Helper methods related to the Micronaut HttpClient
 */
class ClientHelper {

    static String LOGIN_URI = "/login"

    /**
     * Logs in as the given user by requesting a token that can included in other requests.
     * @param client An instance of the Micronaut Http Client
     * @param user The user to generate the token on behalf of
     * @return
     */
    static BearerToken login(HttpClient client, User user){
        UserCredentials credentials = new UserCredentials(username: user.username, password: user.password)
        HttpRequest request = HttpRequest.POST(LOGIN_URI, credentials)
        HttpResponse<BearerToken> resp = client.toBlocking().exchange(request, BearerToken)
        return resp.body()
    }
}

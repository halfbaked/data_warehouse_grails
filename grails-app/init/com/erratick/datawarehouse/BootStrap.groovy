package com.erratick.datawarehouse

import com.erratick.datawarehouse.auth.SecurityRoleService
import com.erratick.datawarehouse.auth.SecurityRoles
import com.erratick.datawarehouse.auth.User
import com.erratick.datawarehouse.auth.UserSecurityRoleService
import com.erratick.datawarehouse.auth.UserService
import grails.core.GrailsApplication

class BootStrap {

    GrailsApplication grailsApplication
    UserService userService
    SecurityRoleService securityRoleService
    UserSecurityRoleService userSecurityRoleService

    def init = { servletContext ->

        SecurityRoles.ALL.each { String authority ->
            if ( !securityRoleService.findByAuthority(authority) ) {
                securityRoleService.save(authority)
            }
        }

        def username = grailsApplication.config.getProperty('app_username')
        if(!username)
            throw new InitException("Username of first user expected. Set appropriate value for environment variable DATA_WAREHOUSE_USER")
        if (username && !userService.findByUsername(username) ) {
            String password = grailsApplication.config.getProperty('app_password')
            if(!password)
                throw new InitException("Password of first user expected. Set appropriate value for environment variable DATA_WAREHOUSE_PASSWORD")
            if(password) {
                User u = userService.save(username, password)
                userSecurityRoleService.save(u, securityRoleService.findByAuthority(SecurityRoles.ADMIN))
            }
        }
    }

    def destroy = {
    }
}

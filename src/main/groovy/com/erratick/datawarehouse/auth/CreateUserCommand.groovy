package com.erratick.datawarehouse.auth

import grails.validation.Validateable

class CreateUserCommand implements Validateable {

    SecurityRoleService securityRoleService

    String username
    String password
    String role

    static constraints = {
        importFrom User
        role inList: SecurityRoles.ALL
    }
}
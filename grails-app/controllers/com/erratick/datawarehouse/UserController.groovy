package com.erratick.datawarehouse

import com.erratick.datawarehouse.auth.CreateUserCommand
import com.erratick.datawarehouse.auth.SecurityRole
import com.erratick.datawarehouse.auth.SecurityRoleService
import com.erratick.datawarehouse.auth.User
import com.erratick.datawarehouse.auth.UserSecurityRoleService
import com.erratick.datawarehouse.auth.UserService
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.springframework.http.HttpStatus

@CompileStatic
class UserController {

    static responseFormats = ['json']

    UserService userService
    UserSecurityRoleService userSecurityRoleService
    SecurityRoleService securityRoleService

    @Transactional
    def save(CreateUserCommand cmd) {
        if(!cmd.hasErrors() && cmd.validate()) {
            User user = userService.save(cmd.username, cmd.password)
            SecurityRole securityRole = securityRoleService.findByAuthority(cmd.role)
            if(securityRole == null) throw new Exception("No security role found for role ${cmd.role}")
            userSecurityRoleService.save(user, securityRole)
            respond user, status: HttpStatus.CREATED
        } else {
            respond cmd.errors
        }
    }
}


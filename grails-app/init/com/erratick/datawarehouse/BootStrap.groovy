package com.erratick.datawarehouse

import com.erratick.datawarehouse.auth.SecurityRoleService
import com.erratick.datawarehouse.auth.SecurityRoles
import com.erratick.datawarehouse.auth.User
import com.erratick.datawarehouse.auth.UserSecurityRoleService
import com.erratick.datawarehouse.auth.UserService

class BootStrap {

    UserService userService
    SecurityRoleService securityRoleService
    UserSecurityRoleService userSecurityRoleService

    def init = { servletContext ->

        SecurityRoles.ALL.each { String authority ->
            if ( !securityRoleService.findByAuthority(authority) ) {
                securityRoleService.save(authority)
            }
        }

        if ( !userService.findByUsername('admin74') ) {
            User u = userService.save('admin74', 'jlkj32klj32lkj$#$#$#$jlk')
            userSecurityRoleService.save(u, securityRoleService.findByAuthority(SecurityRoles.ADMIN))
        }
    }

    def destroy = {
    }
}

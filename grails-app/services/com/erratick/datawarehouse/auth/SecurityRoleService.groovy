package com.erratick.datawarehouse.auth


import grails.gorm.services.Service

@Service(SecurityRole)
interface SecurityRoleService {

    SecurityRole save(String authority)

    SecurityRole findByAuthority(String authority)
}

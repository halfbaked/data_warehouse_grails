package com.erratick.datawarehouse.auth


import grails.gorm.services.Service

@Service(User)
interface UserService {
    User save(String username, String password)

    void delete(Serializable id)

    User findByUsername(String username)

    int count()
}
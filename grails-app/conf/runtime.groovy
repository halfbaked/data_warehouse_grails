import com.erratick.datawarehouse.auth.SecurityRoles

grails {
    plugin {
        springsecurity {
            rest {
                token {
                    storage {
                        jwt {
                            secret = "SECRET_KEY_OF_32_CHARACTERS_MUST_BE_AT_LEAST_256_BITS"
                        }
                    }
                }
                login {
                    endpointUrl = "/login"
                }
            }
            securityConfigType = "InterceptUrlMap"
            filterChain {
                chainMap = [
                        [pattern: '/**',filters: 'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter']
                ]
            }
            userLookup {
                userDomainClassName = 'com.erratick.datawarehouse.auth.User'
                authorityJoinClassName = 'com.erratick.datawarehouse.auth.UserSecurityRole'
            }
            authority {
                className = 'com.erratick.datawarehouse.auth.SecurityRole'
            }
            interceptUrlMap = [
                    [pattern: '/',                      access: ['permitAll']],
                    [pattern: '/error',                 access: ['permitAll']],
                    [pattern: '/index',                 access: ['permitAll']],
                    [pattern: '/index.gsp',             access: ['permitAll']],
                    [pattern: '/shutdown',              access: ['permitAll']],
                    [pattern: '/assets/**',             access: ['permitAll']],
                    [pattern: '/**/js/**',              access: ['permitAll']],
                    [pattern: '/**/css/**',             access: ['permitAll']],
                    [pattern: '/**/images/**',          access: ['permitAll']],
                    [pattern: '/**/favicon.ico',        access: ['permitAll']],
                    [pattern: '/login/**',              access: ['permitAll']],
                    [pattern: '/logout',                access: ['permitAll']],
                    [pattern: '/logout/**',             access: ['permitAll']],
                    [pattern: '/users/**',           	access: [SecurityRoles.ADMIN]],
                    [pattern: '/load/**',           	access: [SecurityRoles.ADMIN, SecurityRoles.COLLECTOR]],
                    [pattern: '/virtualMetrics/**',     access: [SecurityRoles.ADMIN, SecurityRoles.COLLECTOR]],
                    [pattern: '/measuredMetrics/**',    access: [SecurityRoles.ADMIN, SecurityRoles.COLLECTOR]],
                    [pattern: '/query/**',           	access: [SecurityRoles.ADMIN, SecurityRoles.COLLECTOR, SecurityRoles.ANALYST]],
            ]
        }
    }
}



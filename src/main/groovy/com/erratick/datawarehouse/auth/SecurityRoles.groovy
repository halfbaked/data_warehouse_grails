package com.erratick.datawarehouse.auth

class SecurityRoles {
    /**
     * Admin of the system. Expected to have full permissions, including the adding and removing of other users.
     */
    static String ADMIN = "ROLE_ADMIN"
    /**
     * Can only read/analyse the data
     */
    static String ANALYST = "ROLE_ANALYST"
    /**
     * Can read/analyse the data, and load additional data into the system.
     */
    static String COLLECTOR = "ROLE_COLLECTOR"
    /**
     * List of all roles
     */
    static List<String> ALL = [ADMIN, ANALYST, COLLECTOR]
}

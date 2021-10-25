package com.erratick.datawarehouse

import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
class WelcomeController {
    def welcomeMessage() {
        render  "Running. Api Only."
    }
}

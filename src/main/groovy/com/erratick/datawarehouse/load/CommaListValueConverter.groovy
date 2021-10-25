package com.erratick.datawarehouse.load

import grails.databinding.converters.FormattedValueConverter

/**
 * A custom converter which will convert a single string of comma separated elements to a list of strings
 */
class CommaListValueConverter implements FormattedValueConverter {
    def convert(value, String format) {
        if('COMMA_LIST' == format) {
            return value.toString().split(",")
        } else {
            return value
        }
    }

    Class getTargetType() {
        List
    }
}

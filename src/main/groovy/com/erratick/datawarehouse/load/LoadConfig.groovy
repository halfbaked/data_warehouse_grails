package com.erratick.datawarehouse.load


import grails.databinding.BindingFormat
import grails.validation.Validateable

/**
 * Command object to capture the load parameters so we know how to process the data
 */
class LoadConfig implements Validateable {

    @BindingFormat('COMMA_LIST')
    List<String> metrics

    @BindingFormat('COMMA_LIST')
    List<String> dimensions

    String dateName
    String dateFormat
}
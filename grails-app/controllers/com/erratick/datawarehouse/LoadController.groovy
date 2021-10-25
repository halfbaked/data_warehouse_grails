package com.erratick.datawarehouse

import com.erratick.datawarehouse.load.LoadConfig
import com.erratick.datawarehouse.load.LoadService
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.http.HttpStatus

class LoadController {

    LoadService loadService

    @Transactional
    def save() {
        log.info("Loading measurement data $params")
        LoadConfig config = new LoadConfig()
        bindData(config, params)
        if(config.hasErrors() || !config.validate()) throw new ValidationException("Invalid load config", config.errors)
        loadService.load(request.contentType, request.reader.text, config)
        respond status: HttpStatus.OK
    }
}

package com.erratick.datawarehouse

import com.erratick.datawarehouse.load.LoadContentInvalidException
import com.erratick.datawarehouse.load.transformer.TransformerNotFoundException
import grails.validation.ValidationException
import org.grails.web.errors.GrailsWrappedRuntimeException
import org.springframework.http.HttpStatus

class ErrorController {

    def index() {
        def exception = request.exception

        // Unwrap exception
        if(exception instanceof GrailsWrappedRuntimeException)
            exception = exception.cause

        // Unwrap the exception further if wrapped in InvocationTargetException
        if(exception instanceof java.lang.reflect.InvocationTargetException)
            exception = exception.cause

        switch(exception) {
            case ValidationException:
                respond(
                    exception.errors.allErrors.collect{ error ->
                        [code: error.code, field: error.field]
                    },
                    status: HttpStatus.UNPROCESSABLE_ENTITY
                )
                break
            case TransformerNotFoundException:
                respond(exception.message, [status: HttpStatus.UNSUPPORTED_MEDIA_TYPE])
                break
            case LoadContentInvalidException:
                respond(exception.message, [status: HttpStatus.BAD_REQUEST])
                break
            case Exception:
                log.error(exception.message, exception)
                respond("Internal Error", [status: HttpStatus.INTERNAL_SERVER_ERROR])
            default:
                log.error("Unknown error", exception)
                respond("Internal Error", [status: HttpStatus.INTERNAL_SERVER_ERROR])
                break
        }

    }

}

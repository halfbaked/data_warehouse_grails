package com.erratick.datawarehouse.load

class LoadContentInvalidException extends Exception {
    LoadContentInvalidException(String message) {
        super(message)
    }
}

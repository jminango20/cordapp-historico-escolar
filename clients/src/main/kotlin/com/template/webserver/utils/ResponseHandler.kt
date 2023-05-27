package com.template.webserver.utils

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ResponseHandler {

    fun generateResponse(message: String, status: HttpStatus, data: Any): ResponseEntity<Map<String, Any>> {
        val map = hashMapOf<String, Any>()

        map["message"] = message
        map["status"] = status.value()
        map["data"] = data

        return ResponseEntity(map, status)
    }

    fun generateErrorResponse(message: String?): ResponseEntity<Map<String, Any>> {
        val map = hashMapOf<String, Any>()

        if(message != null) map["message"] =  message
        map["status"] = HttpStatus.BAD_REQUEST.value()

        return ResponseEntity(map, HttpStatus.BAD_REQUEST)
    }
}
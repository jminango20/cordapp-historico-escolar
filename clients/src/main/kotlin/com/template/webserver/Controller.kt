package com.template.webserver

import com.template.webserver.dto.HistoricoDTO
import com.template.webserver.service.HistoricoEscolarService
import com.template.webserver.utils.ResponseHandler
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.Exception

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/historico") // The paths for HTTP requests are relative to this base path.
class Controller(
        rpc: NodeRPCConnection,
        private val historicoEscolarService: HistoricoEscolarService,
        private val responseHandler: ResponseHandler) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    @PostMapping
    private fun createHistorico(
            @RequestBody historicoDto : HistoricoDTO
    ) : ResponseEntity<Map<String, Any>> {

        return try {

            val result = historicoEscolarService.createHistorico(historicoDto)

            responseHandler.generateResponse(
                    "Historico criado satisfatoriamente",
                    HttpStatus.CREATED,
                    result.tx.outputs.single()
            )

        } catch (e: Exception){
            responseHandler.generateErrorResponse(e.message)
        }

    }
}
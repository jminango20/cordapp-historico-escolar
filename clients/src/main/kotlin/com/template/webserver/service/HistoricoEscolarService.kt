package com.template.webserver.service

import com.template.webserver.dto.HistoricoDTO
import net.corda.core.transactions.SignedTransaction

interface HistoricoEscolarService {

    fun createHistorico(historicoDto: HistoricoDTO) : SignedTransaction

}
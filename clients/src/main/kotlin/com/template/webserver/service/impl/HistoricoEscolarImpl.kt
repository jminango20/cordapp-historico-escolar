package com.template.webserver.service.impl

import br.puc.historico.flows.ArmazenarHistoricoEscolarFlow
import br.puc.historico.model.HistoricoEscolar
import com.template.webserver.NodeRPCConnection
import com.template.webserver.dto.HistoricoDTO
import com.template.webserver.service.HistoricoEscolarService
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@Service
class HistoricoEscolarImpl(rpc: NodeRPCConnection): HistoricoEscolarService {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    override fun createHistorico(historicoDto: HistoricoDTO): SignedTransaction {

        val faculdadeParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(historicoDto.faculdade))
                ?: throw IllegalArgumentException("Unknown seller node")

        val historicoModel = HistoricoEscolar(
                idAluno = historicoDto.idAluno,
                nomeCurso = historicoDto.nomeCurso,
                dataInicio = historicoDto.dataInicio,
                nota = historicoDto.nota,
                cargaHoraria = historicoDto.cargaHoraria,
                faculdade = faculdadeParty
        )

        return proxy.startTrackedFlowDynamic(
                ArmazenarHistoricoEscolarFlow.ReqFlow::class.java,
                historicoModel).returnValue.get()
    }
}
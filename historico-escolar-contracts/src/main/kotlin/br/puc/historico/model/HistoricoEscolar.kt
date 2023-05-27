package br.puc.historico.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class HistoricoEscolar(
        val idAluno: Int,
        val nomeCurso: String,
        val dataInicio: Instant, //Representação do tempo.
        val nota: Int,
        val cargaHoraria: Int,
        val faculdade: Party //Representação de um nó dentro da rede.
)

package br.puc.historico.states

import br.puc.historico.contracts.HistoricoEscolarContract
import br.puc.historico.model.HistoricoEscolar
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(HistoricoEscolarContract::class)
data class HistoricoEscolarState(
        val historicoEscolar: HistoricoEscolar,
        val faculdadesReceptoras: List<Party> = listOf(),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> = faculdadesReceptoras + historicoEscolar.faculdade
}

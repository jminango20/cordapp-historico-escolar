package br.puc.historico.flows

import br.puc.historico.contracts.HistoricoEscolarContract
import br.puc.historico.model.HistoricoEscolar
import br.puc.historico.states.HistoricoEscolarState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object ArmazenarHistoricoEscolarFlow {

    @InitiatingFlow
    class ReqFlow(val historicoEscolar: HistoricoEscolar) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {

            requireThat {
                "Eu tenho que ser a faculdade emissora" using (historicoEscolar.faculdade == ourIdentity)
            }

            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            val disciplinaState = HistoricoEscolarState(historicoEscolar = historicoEscolar)

            val comando = Command(
                    HistoricoEscolarContract.Commands.CriarHistoricoEscolar(),
                    disciplinaState.participants.map { it.owningKey }
            )

            val txBuilder = TransactionBuilder(notary = notary)
                    .addOutputState(disciplinaState)
                    .addCommand(comando)

            txBuilder.verify(serviceHub) //Chama ao Contrato para validar as regras

            val transacaoAssinada = serviceHub.signInitialTransaction(txBuilder)

            return subFlow(FinalityFlow(transacaoAssinada, emptyList()))
        }
    }


}
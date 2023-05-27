package br.puc.historico.flows

import br.puc.historico.contracts.HistoricoEscolarContract
import br.puc.historico.states.HistoricoEscolarState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

object EnviarHistoricoEscolarFlow {

    @InitiatingFlow
    class ReqFlow(val historicoId: UUID, val para: Party) : FlowLogic<SignedTransaction>(){

        @Suspendable
        override fun call(): SignedTransaction {

            val criteria = QueryCriteria.LinearStateQueryCriteria(
                    linearId = listOf(UniqueIdentifier(id = historicoId))
            )

            val historicoStateAndRef = serviceHub.vaultService.queryBy(
                    HistoricoEscolarState::class.java, criteria = criteria
            ).states.single()

            val historicoEscolarState = historicoStateAndRef.state.data

            requireThat {
                "Eu tenho que ser a faculdade emissora do historico para enviar para outra faculdade" using
                        (historicoEscolarState.historicoEscolar.faculdade == ourIdentity)
            }

            //Utilizar o mesmo notary que foi utilizado anteriormente para nao ter conflitos
            val notary = historicoStateAndRef.state.notary

            //Vamos criar o novo estado do HistoricoEscolar State
            val novoHistoricoEscolarState = historicoEscolarState.copy(
                    faculdadesReceptoras = historicoEscolarState.faculdadesReceptoras + para
            )

            //Criamos o comando
            val comando = Command(
                    HistoricoEscolarContract.Commands.EnviarHistoricoEscolar(),
                    novoHistoricoEscolarState.participants.map { it.owningKey }
                    )

            //Criamos a transacao
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(historicoStateAndRef)
                    .addOutputState(novoHistoricoEscolarState)
                    .addCommand(comando)

            //Verificamos o contrato
            txBuilder.verify(serviceHub)

            //Assinar a transacao que agora esta validada.
            val transacaoParcialmenteAssinada = serviceHub.signInitialTransaction(txBuilder)

            //Criando session com os outros nodes envolvidos na transacao
            val listaSessao = novoHistoricoEscolarState.faculdadesReceptoras.map {
                initiateFlow(it)
            }

            val transacaoTotalmenteAssinada = subFlow(
                    CollectSignaturesFlow(
                            transacaoParcialmenteAssinada,
                            listaSessao
                    )
            )

            return subFlow(FinalityFlow(transacaoTotalmenteAssinada, listaSessao))
        }
    }


    @InitiatedBy(EnviarHistoricoEscolarFlow.ReqFlow::class)
    class RespFlow(val session: FlowSession) : FlowLogic<SignedTransaction>(){

        @Suspendable
        override fun call(): SignedTransaction {

            val signTransactionFlow = object : SignTransactionFlow(session){
                override fun checkTransaction(stx: SignedTransaction) {

                    requireThat {
                        val outputs = stx.coreTransaction.outputsOfType<HistoricoEscolarState>()

                        "Tinha que ter recebido um Historico Escolar" using (outputs.isNotEmpty())
                        "O historico nao pode ser emitido no meu nome" using (outputs.all { it.historicoEscolar.faculdade != ourIdentity })
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id
            return subFlow(ReceiveFinalityFlow(session, expectedTxId = txId))

        }
    }

}
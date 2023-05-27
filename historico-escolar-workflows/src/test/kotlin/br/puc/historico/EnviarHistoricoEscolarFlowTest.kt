package br.puc.historico

import br.puc.historico.flows.ArmazenarHistoricoEscolarFlow
import br.puc.historico.flows.EnviarHistoricoEscolarFlow
import br.puc.historico.model.HistoricoEscolar
import br.puc.historico.states.HistoricoEscolarState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnviarHistoricoEscolarFlowTest {

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var c: StartedMockNode

    private lateinit var historicoEscolarState: HistoricoEscolarState


    @Before
    fun setup(){

        network = MockNetwork(
                MockNetworkParameters(
                        cordappsForAllNodes = listOf(
                                TestCordapp.findCordapp("br.puc.historico.contracts"),
                                TestCordapp.findCordapp("br.puc.historico.flows")
                        ),
                        notarySpecs = listOf(
                                MockNetworkNotarySpec(
                                        CordaX500Name("Notary", "Sao Paulo", "BR")
                                )
                        )
                )
        )

        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()

        //Somente para testes unitarios
        listOf(a,b,c).forEach {
            it.registerInitiatedFlow(EnviarHistoricoEscolarFlow.RespFlow::class.java)
        }

        network.runNetwork()

        historicoEscolarState = criarDisciplina()
    }

    private fun criarDisciplina(): HistoricoEscolarState {

        val disciplina = HistoricoEscolar(
                idAluno = 1,
                nomeCurso = "Corda",
                dataInicio = Instant.now(),
                nota = 10,
                cargaHoraria = 2,
                faculdade = a.info.legalIdentities.first()
        )

        val flow = ArmazenarHistoricoEscolarFlow.ReqFlow(historicoEscolar = disciplina)

        val future = a.startFlow(flow)

        network.runNetwork()

        val signedTransaction = future.get()

        return signedTransaction.coreTransaction.outputsOfType<HistoricoEscolarState>().single()

    }

    @After
    fun tearDown(){
        network.stopNodes()
    }

    @Test
    fun `Deve enviar um Historico Escolar`(){

        val flow = EnviarHistoricoEscolarFlow.ReqFlow(
                historicoId = historicoEscolarState.linearId.id,
                para = b.info.legalIdentities.first()
        )

        val future = a.startFlow(flow)

        network.runNetwork()

        val signedTransaction = future.get()

        val output = signedTransaction.coreTransaction.outputsOfType<HistoricoEscolarState>().single()
        assertEquals(output.historicoEscolar, historicoEscolarState.historicoEscolar)
        assertTrue(output.faculdadesReceptoras.containsAll(historicoEscolarState.faculdadesReceptoras))
        assertEquals(historicoEscolarState.faculdadesReceptoras.size + 1, output.faculdadesReceptoras.size)

        //Verificar no vault do node A
        val historicoEscolarStateNodeA = a.services.vaultService.queryBy(HistoricoEscolarState::class.java)
                .states.single().state.data

        assertEquals(historicoEscolarStateNodeA, output)


        //Verificar no vault do node B
        val historicoEscolarStateNodeB = b.services.vaultService.queryBy(HistoricoEscolarState::class.java)
                .states.single().state.data

        assertEquals(historicoEscolarStateNodeB, output)

        //Comparar que tanto os vaults do node A como node B tenham o mesmo historico escolar state
        assertEquals(historicoEscolarStateNodeA, historicoEscolarStateNodeB)

    }

}
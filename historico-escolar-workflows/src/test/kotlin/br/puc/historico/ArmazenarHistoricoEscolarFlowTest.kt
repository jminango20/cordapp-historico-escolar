package br.puc.historico

import br.puc.historico.flows.ArmazenarHistoricoEscolarFlow
import br.puc.historico.model.HistoricoEscolar
import br.puc.historico.states.HistoricoEscolarState
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArmazenarHistoricoEscolarFlowTest {

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

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
        network.runNetwork()
    }

    @After
    fun tearDown(){
        network.stopNodes()
    }

    @Test
    fun `Deve criar um Historico Escolar`(){

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

        val listOutputs = signedTransaction.coreTransaction.outputsOfType<HistoricoEscolarState>()
        assertEquals(listOutputs.size, 1)

        val output = listOutputs.single()
        assertEquals(output.historicoEscolar, disciplina)

        //Observando a informacao nos vaults (base de dados) dos nodes a e b

        //Node A
        val historicoStateAndRefNodeA = a.services.vaultService.queryBy(HistoricoEscolarState::class.java).states
        assertEquals(historicoStateAndRefNodeA.size, 1)

        val historicoEscolarState = historicoStateAndRefNodeA.single().state.data
        assertEquals(output, historicoEscolarState)

        //Node B
        val historicoStateAndRefNodeB = b.services.vaultService.queryBy(HistoricoEscolarState::class.java).states
        assertEquals(historicoStateAndRefNodeB.size, 0)

    }


    @Test
    fun `Deve falhar se passa uma nota negativa`(){

        val disciplina = HistoricoEscolar(
                idAluno = 1,
                nomeCurso = "Corda",
                dataInicio = Instant.now(),
                nota = -10,
                cargaHoraria = 2,
                faculdade = a.info.legalIdentities.first()
        )

        val flow = ArmazenarHistoricoEscolarFlow.ReqFlow(historicoEscolar = disciplina)

        val future = a.startFlow(flow)
        network.runNetwork()

        assertFailsWith<FlowException> { future.getOrThrow() }

    }

    @Test
    fun `Deve falhar se o node b dar start o flow`(){

        val disciplina = HistoricoEscolar(
                idAluno = 1,
                nomeCurso = "Corda",
                dataInicio = Instant.now(),
                nota = 10,
                cargaHoraria = 2,
                faculdade = a.info.legalIdentities.first()
        )

        val flow = ArmazenarHistoricoEscolarFlow.ReqFlow(historicoEscolar = disciplina)

        val future = b.startFlow(flow)
        network.runNetwork()

        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }

    }



}
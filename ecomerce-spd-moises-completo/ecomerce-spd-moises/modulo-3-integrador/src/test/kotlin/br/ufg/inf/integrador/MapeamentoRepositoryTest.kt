package br.ufg.inf.integrador

import br.ufg.inf.integrador.model.ClienteCanonico
import br.ufg.inf.integrador.model.MapeamentoId
import br.ufg.inf.integrador.repository.IntegradorDatabase
import br.ufg.inf.integrador.repository.MapeamentoRepository
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MapeamentoRepositoryTest {

    private lateinit var repo: MapeamentoRepository

    private val canonico = ClienteCanonico("uuid-1", "Ana", "Souza", "12345678900", "Rua 10", "APROVADO", true)
    private val mapeamento = MapeamentoId("uuid-1", 1L, "64b7f0c2a1b2c3d4e5f60718")

    @BeforeTest
    fun setup() {
        val arquivo = File.createTempFile("integrador-test", ".db")
        arquivo.deleteOnExit()
        IntegradorDatabase.connect("jdbc:sqlite:${arquivo.absolutePath}")
        repo = MapeamentoRepository()
    }

    @Test
    fun `salva e busca o mapeamento pelos dois lados`() {
        repo.salvarNovo(canonico, mapeamento)

        assertEquals(mapeamento, repo.buscarPorIdOrm(1L))
        assertEquals(mapeamento, repo.buscarPorIdOdm("64b7f0c2a1b2c3d4e5f60718"))
        assertEquals(canonico, repo.buscarCanonico("uuid-1"))
    }

    @Test
    fun `salvarCanonico atualiza quando existe`() {
        repo.salvarNovo(canonico, mapeamento)

        repo.salvarCanonico(canonico.copy(endereco = "Av. Goiás, 500", statusFinanceiro = "BLOQUEADO", elegivelEntrega = false))

        val lido = repo.buscarCanonico("uuid-1")
        assertNotNull(lido)
        assertEquals("Av. Goiás, 500", lido.endereco)
        assertEquals("BLOQUEADO", lido.statusFinanceiro)
    }

    @Test
    fun `remover apaga mapeamento e canonico`() {
        repo.salvarNovo(canonico, mapeamento)

        repo.removerPorIdOrm(1L)

        assertNull(repo.buscarPorIdOrm(1L))
        assertNull(repo.buscarCanonico("uuid-1"))
    }

    @Test
    fun `busca de id inexistente devolve null`() {
        assertNull(repo.buscarPorIdOrm(42L))
        assertNull(repo.buscarPorIdOdm("nao-existe"))
    }
}

package br.com.utfpr.gerenciamento.server.service.report;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.dto.relatorios.*;
import br.com.utfpr.gerenciamento.server.fixture.EmprestimoFixture;
import br.com.utfpr.gerenciamento.server.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testes de integração para ReportDataService.
 *
 * <p>Testa a lógica de negócio crítica: - Cálculo de situação (atrasado, pendente, finalizado) -
 * Mapeamentos DTO - Filtros de dados para relatórios
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ReportDataService.class)
class ReportDataServiceTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ReportDataService reportDataService;

  private final EmprestimoFixture fixture = new EmprestimoFixture();

  private Usuario usuarioEmprestimo;
  private Usuario usuarioResponsavel;
  private Item item;
  private Grupo grupo;
  private Permissao permissao;

  @BeforeEach
  void setUp() {
    // Criar permissão
    permissao = fixture.criarPermissao("ROLE_ALUNO");
    entityManager.persist(permissao);

    // Criar grupo para itens
    grupo = new Grupo();
    grupo.setDescricao("Eletrônicos");
    entityManager.persist(grupo);

    // Criar usuários
    usuarioEmprestimo = fixture.criarUsuario("aluno@teste.com", "Aluno Teste", permissao);
    usuarioEmprestimo.setDocumento("12345678901");
    entityManager.persist(usuarioEmprestimo);

    usuarioResponsavel = fixture.criarUsuario("professor@teste.com", "Professor Teste", permissao);
    usuarioResponsavel.setDocumento("98765432109");
    entityManager.persist(usuarioResponsavel);

    // Criar item
    item = fixture.criarItem("Arduino Uno", "Placa Arduino para testes");
    item.setGrupo(grupo);
    item.setSaldo(BigDecimal.TEN);
    item.setQtdeMinima(BigDecimal.valueOf(5));
    entityManager.persist(item);

    entityManager.flush();
  }

  // ========== TESTES DE HISTÓRICO DE EMPRÉSTIMO ==========

  @Nested
  @DisplayName("getHistoricoEmprestimo")
  class GetHistoricoEmprestimoTests {

    @Test
    @DisplayName("Deve retornar histórico vazio quando usuário não tem empréstimos")
    void deveRetornarHistoricoVazio_QuandoUsuarioNaoTemEmprestimos() {
      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo("99999999999");

      // Then
      assertNotNull(resultado);
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar histórico com empréstimos do usuário")
    void deveRetornarHistorico_QuandoUsuarioTemEmprestimos() {
      // Given
      Emprestimo emprestimo =
          fixture.criarEmprestimoPendente(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimo);
      entityManager.flush();

      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo(usuarioEmprestimo.getDocumento());

      // Then
      assertNotNull(resultado);
      assertEquals(1, resultado.size());
      assertEquals(emprestimo.getId(), resultado.getFirst().getCod());
      assertEquals("Aluno Teste", resultado.getFirst().getNomeUsuario());
    }

    @Test
    @DisplayName("Deve calcular situação 'Em atraso' corretamente")
    void deveCalcularSituacaoEmAtraso() {
      // Given
      Emprestimo emprestimoAtrasado =
          fixture.criarEmprestimoAtrasado(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimoAtrasado);
      entityManager.flush();

      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo(usuarioEmprestimo.getDocumento());

      // Then
      assertEquals(1, resultado.size());
      assertEquals("Em atraso", resultado.getFirst().getSituacao());
    }

    @Test
    @DisplayName("Deve calcular situação 'Em andamento' corretamente")
    void deveCalcularSituacaoEmAndamento() {
      // Given
      Emprestimo emprestimoPendente =
          fixture.criarEmprestimoPendente(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimoPendente);
      entityManager.flush();

      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo(usuarioEmprestimo.getDocumento());

      // Then
      assertEquals(1, resultado.size());
      assertEquals("Em andamento", resultado.getFirst().getSituacao());
    }

    @Test
    @DisplayName("Deve calcular situação 'Finalizado' corretamente")
    void deveCalcularSituacaoFinalizado() {
      // Given
      Emprestimo emprestimoFinalizado =
          fixture.criarEmprestimoFinalizado(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimoFinalizado);
      entityManager.flush();

      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo(usuarioEmprestimo.getDocumento());

      // Then
      assertEquals(1, resultado.size());
      assertEquals("Finalizado", resultado.getFirst().getSituacao());
    }

    @Test
    @DisplayName("Deve mapear todos os campos do DTO corretamente")
    void deveMappearTodosCamposDto() {
      // Given
      Emprestimo emprestimo =
          fixture.criarEmprestimoFinalizado(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimo);
      entityManager.flush();

      // When
      List<HistoricoEmprestimoDto> resultado =
          reportDataService.getHistoricoEmprestimo(usuarioEmprestimo.getDocumento());

      // Then
      HistoricoEmprestimoDto dto = resultado.getFirst();
      assertNotNull(dto.getCod());
      assertEquals("Aluno Teste", dto.getNomeUsuario());
      assertNotNull(dto.getDataEmprestimo());
      assertNotNull(dto.getPrazoDevolucao());
      assertNotNull(dto.getDataDevolucao());
      assertNotNull(dto.getSituacao());
    }
  }

  // ========== TESTES DE ITENS SEM ESTOQUE ==========

  @Nested
  @DisplayName("getItensSemEstoque")
  class GetItensSemEstoqueTests {

    @Test
    @DisplayName("Deve retornar lista vazia quando não há itens com saldo zero")
    void deveRetornarListaVazia_QuandoNaoHaItensSemEstoque() {
      // Given - item já existe com saldo 10

      // When
      List<ItemSemEstoqueDto> resultado = reportDataService.getItensSemEstoque();

      // Then
      assertNotNull(resultado);
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar itens com saldo zero")
    void deveRetornarItensSemEstoque() {
      // Given
      Item itemSemEstoque = fixture.criarItem("Item Sem Estoque", "Descrição");
      itemSemEstoque.setGrupo(grupo);
      itemSemEstoque.setSaldo(BigDecimal.ZERO);
      itemSemEstoque.setQtdeMinima(BigDecimal.ONE);
      entityManager.persist(itemSemEstoque);
      entityManager.flush();

      // When
      List<ItemSemEstoqueDto> resultado = reportDataService.getItensSemEstoque();

      // Then
      assertEquals(1, resultado.size());
      assertEquals(itemSemEstoque.getId(), resultado.getFirst().getCod());
      assertEquals("Item Sem Estoque", resultado.getFirst().getNome());
      assertEquals("Eletrônicos", resultado.getFirst().getGrupo());
    }

    @Test
    @DisplayName("Deve mapear grupo como vazio quando item não tem grupo")
    void deveMappearGrupoVazio_QuandoItemNaoTemGrupo() {
      // Given
      Item itemSemGrupo = fixture.criarItem("Item Sem Grupo", "Descrição");
      itemSemGrupo.setGrupo(null);
      itemSemGrupo.setSaldo(BigDecimal.ZERO);
      entityManager.persist(itemSemGrupo);
      entityManager.flush();

      // When
      List<ItemSemEstoqueDto> resultado = reportDataService.getItensSemEstoque();

      // Then
      assertEquals(1, resultado.size());
      assertEquals("", resultado.getFirst().getGrupo());
    }
  }

  // ========== TESTES DE EMPRÉSTIMOS REALIZADOS ==========

  @Nested
  @DisplayName("getEmprestimosRealizados")
  class GetEmprestimosRealizadosTests {

    @Test
    @DisplayName("Deve retornar empréstimos no período especificado")
    void deveRetornarEmprestimosNoPeriodo() {
      // Given
      Emprestimo emprestimo =
          fixture.criarEmprestimoPendente(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimo);
      entityManager.flush();

      LocalDate inicio = LocalDate.now().minusDays(10);
      LocalDate fim = LocalDate.now();

      // When
      List<EmprestimoRealizadoDto> resultado =
          reportDataService.getEmprestimosRealizados(inicio, fim);

      // Then
      assertNotNull(resultado);
      assertEquals(1, resultado.size());
      assertEquals(emprestimo.getId(), resultado.getFirst().getCod());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há empréstimos no período")
    void deveRetornarListaVazia_QuandoNaoHaEmprestimosNoPeriodo() {
      // Given
      LocalDate inicio = LocalDate.now().plusDays(1);
      LocalDate fim = LocalDate.now().plusDays(30);

      // When
      List<EmprestimoRealizadoDto> resultado =
          reportDataService.getEmprestimosRealizados(inicio, fim);

      // Then
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve mapear usuário empréstimo e responsável corretamente")
    void deveMappearUsuariosCorretamente() {
      // Given
      Emprestimo emprestimo =
          fixture.criarEmprestimoPendente(usuarioEmprestimo, usuarioResponsavel, item);
      entityManager.persist(emprestimo);
      entityManager.flush();

      LocalDate inicio = LocalDate.now().minusDays(10);
      LocalDate fim = LocalDate.now();

      // When
      List<EmprestimoRealizadoDto> resultado =
          reportDataService.getEmprestimosRealizados(inicio, fim);

      // Then
      EmprestimoRealizadoDto dto = resultado.getFirst();
      assertEquals("Aluno Teste", dto.getUsuarioEmprestimo());
      assertEquals("Professor Teste", dto.getUsuarioResponsavel());
    }
  }

  // ========== TESTES DE RESERVAS DO ITEM ==========

  @Nested
  @DisplayName("getReservasDoItem")
  class GetReservasDoItemTests {

    @Test
    @DisplayName("Deve retornar lista vazia quando item não tem reservas")
    void deveRetornarListaVazia_QuandoItemNaoTemReservas() {
      // When
      List<ReservaItemDto> resultado = reportDataService.getReservasDoItem(item.getId());

      // Then
      assertNotNull(resultado);
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar reservas do item")
    void deveRetornarReservasDoItem() {
      // Given
      Reserva reserva = criarReserva(usuarioEmprestimo, item, 5);
      entityManager.persist(reserva);
      entityManager.flush();

      // When
      List<ReservaItemDto> resultado = reportDataService.getReservasDoItem(item.getId());

      // Then
      assertEquals(1, resultado.size());
      assertEquals(reserva.getId(), resultado.getFirst().getCod());
      assertEquals("Aluno Teste", resultado.getFirst().getUsuarioReserva());
      assertEquals("Arduino Uno", resultado.getFirst().getNomeItem());
      assertEquals(BigDecimal.valueOf(5), resultado.getFirst().getQtde());
    }

    @Test
    @DisplayName("Deve ordenar reservas por data de retirada")
    void deveOrdenarPorDataRetirada() {
      // Given
      Reserva reserva1 = criarReserva(usuarioEmprestimo, item, 1);
      reserva1.setDataRetirada(LocalDate.now().plusDays(5));
      entityManager.persist(reserva1);

      Reserva reserva2 = criarReserva(usuarioEmprestimo, item, 2);
      reserva2.setDataRetirada(LocalDate.now().plusDays(1));
      entityManager.persist(reserva2);

      entityManager.flush();

      // When
      List<ReservaItemDto> resultado = reportDataService.getReservasDoItem(item.getId());

      // Then
      assertEquals(2, resultado.size());
      // reserva2 deve vir primeiro (data de retirada mais próxima)
      assertEquals(reserva2.getId(), resultado.get(0).getCod());
      assertEquals(reserva1.getId(), resultado.get(1).getCod());
    }
  }

  // ========== TESTES DE SOLICITAÇÕES DO ITEM ==========

  @Nested
  @DisplayName("getSolicitacoesDoItem")
  class GetSolicitacoesDoItemTests {

    @Test
    @DisplayName("Deve retornar lista vazia quando item não tem solicitações")
    void deveRetornarListaVazia_QuandoItemNaoTemSolicitacoes() {
      // When
      List<SolicitacaoItemDto> resultado = reportDataService.getSolicitacoesDoItem(item.getId());

      // Then
      assertNotNull(resultado);
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar solicitações do item")
    void deveRetornarSolicitacoesDoItem() {
      // Given
      Solicitacao solicitacao = criarSolicitacao(usuarioEmprestimo, item, 10, "Preciso para aula");
      entityManager.persist(solicitacao);
      entityManager.flush();

      // When
      List<SolicitacaoItemDto> resultado = reportDataService.getSolicitacoesDoItem(item.getId());

      // Then
      assertEquals(1, resultado.size());
      assertEquals(solicitacao.getId(), resultado.getFirst().getCod());
      assertEquals("Aluno Teste", resultado.getFirst().getUsuarioSolicitacao());
      assertEquals("Preciso para aula", resultado.getFirst().getDescricao());
      assertEquals(BigDecimal.valueOf(10), resultado.getFirst().getQtde());
    }
  }

  // ========== TESTES DE ITENS QUANTIDADE MÍNIMA ==========

  @Nested
  @DisplayName("getItensQtdeMinima")
  class GetItensQtdeMinimaTests {

    @Test
    @DisplayName("Deve excluir itens com saldo acima da quantidade mínima")
    void deveExcluirItens_QuandoSaldoAcimaQtdeMinima() {
      // Given - item já existe com saldo 10 e qtde mínima 5 (saldo > qtdeMinima)

      // When
      List<ItemQtdeMinimaDto> resultado = reportDataService.getItensQtdeMinima();

      // Then
      assertNotNull(resultado);
      // O item do setUp não deve estar no resultado (saldo 10 > qtdeMinima 5)
      assertFalse(
          resultado.stream().anyMatch(dto -> dto.getCod().equals(item.getId())),
          "Item com saldo acima de qtdeMinima não deve ser retornado");
    }

    @Test
    @DisplayName("Deve retornar itens com saldo igual à quantidade mínima")
    void deveRetornarItens_QuandoSaldoIgualQtdeMinima() {
      // Given
      Item itemNoLimite = fixture.criarItem("Item No Limite Test", "Descrição");
      itemNoLimite.setGrupo(grupo);
      itemNoLimite.setSaldo(BigDecimal.valueOf(5));
      itemNoLimite.setQtdeMinima(BigDecimal.valueOf(5));
      entityManager.persist(itemNoLimite);
      entityManager.flush();

      // When
      List<ItemQtdeMinimaDto> resultado = reportDataService.getItensQtdeMinima();

      // Then
      assertTrue(
          resultado.stream().anyMatch(dto -> dto.getCod().equals(itemNoLimite.getId())),
          "Item com saldo igual a qtdeMinima deve ser retornado");

      ItemQtdeMinimaDto itemEncontrado =
          resultado.stream()
              .filter(dto -> dto.getCod().equals(itemNoLimite.getId()))
              .findFirst()
              .orElseThrow();

      assertEquals(BigDecimal.valueOf(5), itemEncontrado.getSaldo());
      assertEquals(BigDecimal.valueOf(5), itemEncontrado.getQtdeMinima());
    }

    @Test
    @DisplayName("Deve retornar itens com saldo abaixo da quantidade mínima")
    void deveRetornarItens_QuandoSaldoAbaixoQtdeMinima() {
      // Given
      Item itemAbaixo = fixture.criarItem("Item Abaixo Test", "Descrição");
      itemAbaixo.setGrupo(grupo);
      itemAbaixo.setSaldo(BigDecimal.valueOf(2));
      itemAbaixo.setQtdeMinima(BigDecimal.valueOf(5));
      entityManager.persist(itemAbaixo);
      entityManager.flush();

      // When
      List<ItemQtdeMinimaDto> resultado = reportDataService.getItensQtdeMinima();

      // Then
      assertTrue(
          resultado.stream().anyMatch(dto -> dto.getCod().equals(itemAbaixo.getId())),
          "Item com saldo abaixo de qtdeMinima deve ser retornado");

      ItemQtdeMinimaDto itemEncontrado =
          resultado.stream()
              .filter(dto -> dto.getCod().equals(itemAbaixo.getId()))
              .findFirst()
              .orElseThrow();

      assertEquals(BigDecimal.valueOf(2), itemEncontrado.getSaldo());
      assertEquals(BigDecimal.valueOf(5), itemEncontrado.getQtdeMinima());
    }
  }

  // ========== MÉTODOS AUXILIARES ==========

  private Reserva criarReserva(Usuario usuario, Item item, int quantidade) {
    Reserva reserva = new Reserva();
    reserva.setUsuario(usuario);
    reserva.setDataReserva(LocalDate.now());
    reserva.setDataRetirada(LocalDate.now().plusDays(3));

    ReservaItem reservaItem = new ReservaItem();
    reservaItem.setReserva(reserva);
    reservaItem.setItem(item);
    reservaItem.setQtde(BigDecimal.valueOf(quantidade));

    reserva.setReservaItem(new ArrayList<>());
    reserva.getReservaItem().add(reservaItem);

    return reserva;
  }

  private Solicitacao criarSolicitacao(
      Usuario usuario, Item item, int quantidade, String descricao) {
    Solicitacao solicitacao = new Solicitacao();
    solicitacao.setUsuario(usuario);
    solicitacao.setDataSolicitacao(LocalDate.now());
    solicitacao.setDescricao(descricao);

    SolicitacaoItem solicitacaoItem = new SolicitacaoItem();
    solicitacaoItem.setSolicitacao(solicitacao);
    solicitacaoItem.setItem(item);
    solicitacaoItem.setQtde(BigDecimal.valueOf(quantidade));

    solicitacao.setSolicitacaoItem(new ArrayList<>());
    solicitacao.getSolicitacaoItem().add(solicitacaoItem);

    return solicitacao;
  }
}

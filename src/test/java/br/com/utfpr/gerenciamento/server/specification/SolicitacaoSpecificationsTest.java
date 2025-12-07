package br.com.utfpr.gerenciamento.server.specification;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.model.*;
import br.com.utfpr.gerenciamento.server.repository.SolicitacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testes de integração para SolicitacaoSpecifications.
 *
 * <p>Valida as Specifications usadas nos relatórios de solicitações de compra.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SolicitacaoSpecificationsTest {

  /** Threshold de performance configurável: 5s em CI, 1s local. */
  private static final long PERFORMANCE_THRESHOLD_MS = System.getenv("CI") != null ? 5000 : 1000;

  private static final int SOLICITACOES_INICIAIS_RESISTOR = 2;
  private static final int SOLICITACOES_ADICIONADAS = 50;

  @Autowired private TestEntityManager entityManager;

  @Autowired private SolicitacaoRepository repository;

  private Usuario usuario;
  private Item itemResistor;
  private Item itemCapacitor;
  private Solicitacao solicitacaoResistores;
  private Solicitacao solicitacaoCapacitores;
  private Solicitacao solicitacaoMista;

  @BeforeEach
  void setUp() {
    // Criar permissão e usuário
    Permissao permissao = criarPermissao("ROLE_LABORATORISTA");
    entityManager.persist(permissao);

    usuario = criarUsuario("lab@teste.com", "Laboratorista Teste", permissao);
    entityManager.persist(usuario);

    // Criar itens
    itemResistor = criarItem("Resistor 10k Ohm");
    entityManager.persist(itemResistor);

    itemCapacitor = criarItem("Capacitor 100uF");
    entityManager.persist(itemCapacitor);

    // Criar solicitações
    solicitacaoResistores =
        criarSolicitacao(usuario, LocalDate.now(), "Compra urgente de resistores");
    adicionarItemNaSolicitacao(solicitacaoResistores, itemResistor, BigDecimal.valueOf(100));
    entityManager.persist(solicitacaoResistores);

    solicitacaoCapacitores =
        criarSolicitacao(
            usuario, LocalDate.now().minusDays(5), "Reposição de capacitores eletrolíticos");
    adicionarItemNaSolicitacao(solicitacaoCapacitores, itemCapacitor, BigDecimal.valueOf(50));
    entityManager.persist(solicitacaoCapacitores);

    solicitacaoMista =
        criarSolicitacao(usuario, LocalDate.now().minusDays(10), "Compra de componentes diversos");
    adicionarItemNaSolicitacao(solicitacaoMista, itemResistor, BigDecimal.valueOf(200));
    adicionarItemNaSolicitacao(solicitacaoMista, itemCapacitor, BigDecimal.valueOf(75));
    entityManager.persist(solicitacaoMista);

    entityManager.flush();
  }

  private Permissao criarPermissao(String nome) {
    Permissao permissao = new Permissao();
    permissao.setNome(nome);
    return permissao;
  }

  private Usuario criarUsuario(String email, String nome, Permissao permissao) {
    Usuario u = new Usuario();
    u.setUsername(email);
    u.setNome(nome);
    u.setEmail(email);
    u.setEmailVerificado(true);
    u.setPassword("senha123");
    u.setTelefone("41999999999");
    u.setPermissoes(new HashSet<>());
    u.getPermissoes().add(permissao);
    return u;
  }

  private Item criarItem(String nome) {
    Item item = new Item();
    item.setNome(nome);
    item.setQtdeMinima(BigDecimal.TEN);
    item.setSaldo(BigDecimal.valueOf(5));
    return item;
  }

  private Solicitacao criarSolicitacao(Usuario u, LocalDate data, String descricao) {
    Solicitacao solicitacao = new Solicitacao();
    solicitacao.setUsuario(u);
    solicitacao.setDataSolicitacao(data);
    solicitacao.setDescricao(descricao);
    solicitacao.setSolicitacaoItem(new ArrayList<>());
    return solicitacao;
  }

  private void adicionarItemNaSolicitacao(Solicitacao solicitacao, Item item, BigDecimal qtde) {
    SolicitacaoItem solicitacaoItem = new SolicitacaoItem();
    solicitacaoItem.setSolicitacao(solicitacao);
    solicitacaoItem.setItem(item);
    solicitacaoItem.setQtde(qtde);
    solicitacao.getSolicitacaoItem().add(solicitacaoItem);
  }

  // ========== byItemId() ==========

  @Nested
  @DisplayName("byItemId()")
  class ByItemIdTests {

    @Test
    @DisplayName("Deve retornar solicitações que contêm o item especificado")
    void deveRetornarSolicitacoesComItem() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.byItemId(itemResistor.getId());

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      List<Long> ids = resultado.stream().map(Solicitacao::getId).toList();
      assertTrue(ids.contains(solicitacaoResistores.getId()));
      assertTrue(ids.contains(solicitacaoMista.getId()));
    }

    @Test
    @DisplayName("Deve retornar apenas solicitações do item específico")
    void deveRetornarApenasSolicitacoesDoItemEspecifico() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.byItemId(itemCapacitor.getId());

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      List<Long> ids = resultado.stream().map(Solicitacao::getId).toList();
      assertTrue(ids.contains(solicitacaoCapacitores.getId()));
      assertTrue(ids.contains(solicitacaoMista.getId()));
      assertFalse(ids.contains(solicitacaoResistores.getId()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando item não tem solicitações")
    void deveRetornarListaVaziaQuandoItemNaoTemSolicitacoes() {
      // Given - Item sem solicitações
      Item itemSemSolicitacoes = criarItem("Item Nunca Solicitado");
      entityManager.persist(itemSemSolicitacoes);
      entityManager.flush();

      Specification<Solicitacao> spec =
          SolicitacaoSpecifications.byItemId(itemSemSolicitacoes.getId());

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar todas as solicitações quando itemId é null")
    void deveRetornarTodasSolicitacoesQuandoItemIdNull() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.byItemId(null);

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertEquals(3, resultado.size());
    }

    @Test
    @DisplayName("Deve prevenir duplicação de resultados")
    void devePrevenirDuplicacao() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.byItemId(itemResistor.getId());

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then - Sem duplicatas
      long solicitacoesUnicas = resultado.stream().map(Solicitacao::getId).distinct().count();
      assertEquals(resultado.size(), solicitacoesUnicas);
    }
  }

  // ========== withFetchJoins() ==========

  @Nested
  @DisplayName("withFetchJoins()")
  class WithFetchJoinsTests {

    @Test
    @DisplayName("Deve carregar usuário e itens sem LazyInitializationException")
    void deveCarregarUsuarioEItensComFetch() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.withFetchJoins();

      // When
      entityManager.clear();
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertFalse(resultado.isEmpty());

      // Acessar dados relacionados após clear (entidades detached)
      resultado.forEach(
          solicitacao -> {
            assertNotNull(solicitacao.getUsuario());
            assertNotNull(solicitacao.getUsuario().getNome());

            assertNotNull(solicitacao.getSolicitacaoItem());
            solicitacao
                .getSolicitacaoItem()
                .forEach(
                    si -> {
                      assertNotNull(si.getItem());
                      assertNotNull(si.getItem().getNome());
                    });
          });
    }

    @Test
    @DisplayName("Deve executar count query sem JOIN FETCH")
    void deveExecutarCountSemFetch() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.withFetchJoins();

      // When
      long count = repository.count(spec);

      // Then
      assertEquals(3L, count);
    }

    @Test
    @DisplayName("Deve prevenir duplicação com distinct")
    void devePrevenirDuplicacaoComDistinct() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.withFetchJoins();

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      long solicitacoesUnicas = resultado.stream().map(Solicitacao::getId).distinct().count();
      assertEquals(resultado.size(), solicitacoesUnicas);
    }
  }

  // ========== forRelatorioSolicitacaoItem() ==========

  @Nested
  @DisplayName("forRelatorioSolicitacaoItem()")
  class ForRelatorioSolicitacaoItemTests {

    @Test
    @DisplayName("Deve combinar filtro por itemId com fetch joins")
    void deveCombinarFiltroComFetch() {
      // Given
      Specification<Solicitacao> spec =
          SolicitacaoSpecifications.forRelatorioSolicitacaoItem(itemResistor.getId());

      // When
      entityManager.clear();
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      // Dados relacionados devem estar carregados
      resultado.forEach(
          solicitacao -> {
            assertNotNull(solicitacao.getUsuario().getNome());
            assertFalse(solicitacao.getSolicitacaoItem().isEmpty());
          });
    }

    @Test
    @DisplayName("Deve carregar quantidade correta de itens por solicitação")
    void deveCarregarQuantidadeCorretaDeItens() {
      // Given
      Specification<Solicitacao> spec =
          SolicitacaoSpecifications.forRelatorioSolicitacaoItem(itemResistor.getId());

      // When
      entityManager.clear();
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      // solicitacaoResistores tem 1 item, solicitacaoMista tem 2 itens
      Solicitacao solUmItem =
          resultado.stream()
              .filter(s -> s.getId().equals(solicitacaoResistores.getId()))
              .findFirst()
              .orElseThrow();
      assertEquals(1, solUmItem.getSolicitacaoItem().size());

      Solicitacao solDoisItens =
          resultado.stream()
              .filter(s -> s.getId().equals(solicitacaoMista.getId()))
              .findFirst()
              .orElseThrow();
      assertEquals(2, solDoisItens.getSolicitacaoItem().size());
    }

    @Test
    @DisplayName("Deve preservar dados de quantidade dos itens")
    void devePreservarDadosDeQuantidade() {
      // Given
      Specification<Solicitacao> spec =
          SolicitacaoSpecifications.forRelatorioSolicitacaoItem(itemResistor.getId());

      // When
      entityManager.clear();
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      Solicitacao solResistores =
          resultado.stream()
              .filter(s -> s.getId().equals(solicitacaoResistores.getId()))
              .findFirst()
              .orElseThrow();

      assertFalse(
          solResistores.getSolicitacaoItem().isEmpty(),
          "Solicitação de resistores deveria ter ao menos um item");
      SolicitacaoItem itemSol = solResistores.getSolicitacaoItem().getFirst();
      assertEquals(0, BigDecimal.valueOf(100).compareTo(itemSol.getQtde()));
    }
  }

  // ========== Edge Cases ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Deve tratar solicitação sem itens")
    void deveTratarSolicitacaoSemItens() {
      // Given - Solicitação vazia
      Solicitacao solicitacaoVazia =
          criarSolicitacao(usuario, LocalDate.now(), "Solicitação sem itens");
      entityManager.persist(solicitacaoVazia);
      entityManager.flush();

      Specification<Solicitacao> spec = SolicitacaoSpecifications.withFetchJoins();

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then - Deve incluir a solicitação vazia
      assertEquals(4, resultado.size());
      assertTrue(resultado.stream().anyMatch(s -> s.getId().equals(solicitacaoVazia.getId())));
    }

    @Test
    @DisplayName("Deve tratar item inexistente")
    void deveTratarItemInexistente() {
      // Given
      Long idInexistente = 999999L;
      Specification<Solicitacao> spec = SolicitacaoSpecifications.byItemId(idInexistente);

      // When
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve manter performance com grandes volumes")
    void deveManterPerformanceComGrandesVolumes() {
      // Given - Criar solicitações adicionais
      for (int i = 0; i < SOLICITACOES_ADICIONADAS; i++) {
        Solicitacao solicitacao =
            criarSolicitacao(usuario, LocalDate.now().minusDays(i), "Solicitação " + i);
        adicionarItemNaSolicitacao(solicitacao, itemResistor, BigDecimal.valueOf(10 + i));
        entityManager.persist(solicitacao);
      }
      entityManager.flush();

      Specification<Solicitacao> spec =
          SolicitacaoSpecifications.forRelatorioSolicitacaoItem(itemResistor.getId());

      // When
      entityManager.clear();
      long inicio = System.currentTimeMillis();
      List<Solicitacao> resultado = repository.findAll(spec);
      long duracao = System.currentTimeMillis() - inicio;

      // Then
      int totalEsperado = SOLICITACOES_ADICIONADAS + SOLICITACOES_INICIAIS_RESISTOR;
      assertEquals(
          totalEsperado,
          resultado.size(),
          () ->
              "Esperadas "
                  + SOLICITACOES_ADICIONADAS
                  + " solicitações criadas + "
                  + SOLICITACOES_INICIAIS_RESISTOR
                  + " do setUp");

      // Deve carregar dentro do threshold configurado
      assertTrue(
          duracao < PERFORMANCE_THRESHOLD_MS,
          () ->
              "Query deve completar em <"
                  + PERFORMANCE_THRESHOLD_MS
                  + "ms, mas levou: "
                  + duracao
                  + "ms");

      // Dados devem estar carregados (sem N+1)
      resultado.forEach(
          s -> {
            assertNotNull(s.getUsuario().getNome());
            assertNotNull(s.getSolicitacaoItem());
          });
    }

    @Test
    @DisplayName("Deve manter integridade de dados entre solicitações")
    void deveManterIntegridadeDeDados() {
      // Given
      Specification<Solicitacao> spec = SolicitacaoSpecifications.withFetchJoins();

      // When
      entityManager.clear();
      List<Solicitacao> resultado = repository.findAll(spec);

      // Then - Cada solicitação deve manter seus próprios dados
      Solicitacao solRes =
          resultado.stream()
              .filter(s -> s.getId().equals(solicitacaoResistores.getId()))
              .findFirst()
              .orElseThrow();

      Solicitacao solCap =
          resultado.stream()
              .filter(s -> s.getId().equals(solicitacaoCapacitores.getId()))
              .findFirst()
              .orElseThrow();

      // Descrições diferentes
      assertNotEquals(solRes.getDescricao(), solCap.getDescricao());

      // Itens diferentes
      assertFalse(
          solRes.getSolicitacaoItem().isEmpty(),
          "Solicitação de resistores deveria ter ao menos um item");
      assertFalse(
          solCap.getSolicitacaoItem().isEmpty(),
          "Solicitação de capacitores deveria ter ao menos um item");
      Long itemIdRes = solRes.getSolicitacaoItem().getFirst().getItem().getId();
      Long itemIdCap = solCap.getSolicitacaoItem().getFirst().getItem().getId();
      assertNotEquals(itemIdRes, itemIdCap);
    }
  }
}

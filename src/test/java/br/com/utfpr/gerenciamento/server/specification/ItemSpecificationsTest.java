package br.com.utfpr.gerenciamento.server.specification;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.model.Grupo;
import br.com.utfpr.gerenciamento.server.model.Item;
import br.com.utfpr.gerenciamento.server.repository.ItemRepository;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
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
 * Testes de integração para ItemSpecifications.
 *
 * <p>Valida as Specifications usadas nos relatórios de estoque. Os testes usam asserções relativas
 * (verificam que os itens de teste estão presentes) para serem independentes de dados de migração.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ItemSpecificationsTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ItemRepository repository;

  private Grupo grupoEletronicos;
  private Grupo grupoFerramentas;

  private Item itemSemEstoque;
  private Item itemComEstoqueNormal;
  private Item itemNoLimiteMinimo;
  private Item itemAbaixoMinimo;

  // Contador de itens criados no setUp para validações
  private long itensNoSetUp;

  @BeforeEach
  void setUp() {
    // Contar itens existentes antes do setUp
    long itensExistentes = repository.count();

    // Criar grupos
    grupoEletronicos = criarGrupo("Eletrônicos_Test");
    entityManager.persist(grupoEletronicos);

    grupoFerramentas = criarGrupo("Ferramentas_Test");
    entityManager.persist(grupoFerramentas);

    // Item sem estoque (saldo = 0)
    itemSemEstoque = criarItem("Multímetro Digital Test", grupoEletronicos);
    itemSemEstoque.setSaldo(BigDecimal.ZERO);
    itemSemEstoque.setQtdeMinima(BigDecimal.valueOf(5));
    entityManager.persist(itemSemEstoque);

    // Item com estoque normal (saldo > qtdeMinima)
    itemComEstoqueNormal = criarItem("Chave Phillips Test", grupoFerramentas);
    itemComEstoqueNormal.setSaldo(BigDecimal.valueOf(50));
    itemComEstoqueNormal.setQtdeMinima(BigDecimal.valueOf(10));
    entityManager.persist(itemComEstoqueNormal);

    // Item no limite mínimo (saldo == qtdeMinima)
    itemNoLimiteMinimo = criarItem("Osciloscópio Test", grupoEletronicos);
    itemNoLimiteMinimo.setSaldo(BigDecimal.valueOf(5));
    itemNoLimiteMinimo.setQtdeMinima(BigDecimal.valueOf(5));
    entityManager.persist(itemNoLimiteMinimo);

    // Item abaixo do mínimo (saldo < qtdeMinima)
    itemAbaixoMinimo = criarItem("Resistor 10k Test", grupoEletronicos);
    itemAbaixoMinimo.setSaldo(BigDecimal.valueOf(3));
    itemAbaixoMinimo.setQtdeMinima(BigDecimal.valueOf(10));
    entityManager.persist(itemAbaixoMinimo);

    entityManager.flush();

    itensNoSetUp = repository.count() - itensExistentes;
  }

  private Grupo criarGrupo(String descricao) {
    Grupo grupo = new Grupo();
    grupo.setDescricao(descricao);
    return grupo;
  }

  private Item criarItem(String nome, Grupo grupo) {
    Item item = new Item();
    item.setNome(nome);
    item.setGrupo(grupo);
    item.setQtdeMinima(BigDecimal.ONE);
    return item;
  }

  // ========== bySaldoZero() ==========

  @Nested
  @DisplayName("bySaldoZero()")
  class BySaldoZeroTests {

    @Test
    @DisplayName("Deve retornar itens com saldo = 0")
    void deveRetornarItensSaldoZero() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoZero();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then
      assertFalse(resultado.isEmpty());
      // Verifica que nosso item de teste está presente
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemEstoque.getId())));

      // Todos os resultados devem ter saldo = 0
      resultado.forEach(item -> assertEquals(0, BigDecimal.ZERO.compareTo(item.getSaldo())));
    }

    @Test
    @DisplayName("Deve excluir itens com saldo > 0")
    void deveExcluirItensComSaldoMaiorQueZero() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoZero();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then
      // Item com estoque normal NÃO deve estar presente
      assertFalse(resultado.stream().anyMatch(i -> i.getId().equals(itemComEstoqueNormal.getId())));
    }

    @Test
    @DisplayName("Deve executar count query corretamente")
    void deveExecutarCountQueryCorretamente() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoZero();

      // When
      long count = repository.count(spec);

      // Then
      assertTrue(count >= 1); // Pelo menos nosso item de teste
    }
  }

  // ========== bySaldoMenorOuIgualQtdeMinima() ==========

  @Nested
  @DisplayName("bySaldoMenorOuIgualQtdeMinima()")
  class BySaldoMenorOuIgualQtdeMinimaTests {

    @Test
    @DisplayName("Deve retornar itens com saldo <= qtdeMinima")
    void deveRetornarItensSaldoMenorOuIgualQtdeMinima() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoMenorOuIgualQtdeMinima();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then
      // Esperados: itemSemEstoque (0 <= 5), itemNoLimiteMinimo (5 <= 5), itemAbaixoMinimo (3 <= 10)
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemEstoque.getId())));
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemNoLimiteMinimo.getId())));
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemAbaixoMinimo.getId())));

      // Não deve conter item com estoque normal
      assertFalse(resultado.stream().anyMatch(i -> i.getId().equals(itemComEstoqueNormal.getId())));
    }

    @Test
    @DisplayName("Deve incluir item com saldo exatamente igual à qtdeMinima")
    void deveIncluirItemComSaldoIgualQtdeMinima() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoMenorOuIgualQtdeMinima();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemNoLimiteMinimo.getId())));

      Item itemEncontrado =
          resultado.stream()
              .filter(i -> i.getId().equals(itemNoLimiteMinimo.getId()))
              .findFirst()
              .orElseThrow();

      assertEquals(0, itemEncontrado.getSaldo().compareTo(itemEncontrado.getQtdeMinima()));
    }

    @Test
    @DisplayName("Deve excluir itens com saldo > qtdeMinima")
    void deveExcluirItensAcimaDoMinimo() {
      // Given
      Specification<Item> spec = ItemSpecifications.bySaldoMenorOuIgualQtdeMinima();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then
      // itemComEstoqueNormal tem saldo=50, qtdeMinima=10, então não deve aparecer
      assertFalse(resultado.stream().anyMatch(i -> i.getId().equals(itemComEstoqueNormal.getId())));
    }
  }

  // ========== withGrupoFetch() ==========

  @Nested
  @DisplayName("withGrupoFetch()")
  class WithGrupoFetchTests {

    @Test
    @DisplayName("Deve carregar grupo com JOIN FETCH sem LazyInitializationException")
    void deveCarregarGrupoComJoinFetch() {
      // Given
      Specification<Item> spec = ItemSpecifications.withGrupoFetch();

      // When
      entityManager.clear(); // Limpa cache para forçar nova query
      List<Item> resultado = repository.findAll(spec);

      // Then
      assertFalse(resultado.isEmpty());

      // Acessar grupo após clear (detached entity)
      resultado.forEach(
          item -> {
            if (item.getGrupo() != null) {
              assertNotNull(item.getGrupo().getDescricao());
            }
          });
    }

    @Test
    @DisplayName("Deve executar count query sem JOIN FETCH")
    void deveExecutarCountSemJoinFetch() {
      // Given
      Specification<Item> spec = ItemSpecifications.withGrupoFetch();

      // When
      long count = repository.count(spec);

      // Then
      assertTrue(count >= itensNoSetUp); // Pelo menos os itens criados no setUp
    }
  }

  // ========== forRelatorioItensSemEstoque() ==========

  @Nested
  @DisplayName("forRelatorioItensSemEstoque()")
  class ForRelatorioItensSemEstoqueTests {

    @Test
    @DisplayName("Deve combinar filtro saldo zero com fetch de grupo")
    void deveCombinarFiltroComFetch() {
      // Given
      Specification<Item> spec = ItemSpecifications.forRelatorioItensSemEstoque();

      // When
      entityManager.clear();
      List<Item> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemEstoque.getId())));

      // Grupo deve estar carregado para o item de teste
      Item itemTeste =
          resultado.stream()
              .filter(i -> i.getId().equals(itemSemEstoque.getId()))
              .findFirst()
              .orElseThrow();

      assertNotNull(itemTeste.getGrupo());
      assertEquals(grupoEletronicos.getDescricao(), itemTeste.getGrupo().getDescricao());
    }
  }

  // ========== forRelatorioItensQtdeMinima() ==========

  @Nested
  @DisplayName("forRelatorioItensQtdeMinima()")
  class ForRelatorioItensQtdeMinimaTests {

    @Test
    @DisplayName("Deve combinar filtro qtdeMinima com fetch de grupo")
    void deveCombinarFiltroComFetch() {
      // Given
      Specification<Item> spec = ItemSpecifications.forRelatorioItensQtdeMinima();

      // When
      entityManager.clear();
      List<Item> resultado = repository.findAll(spec);

      // Then
      // Verifica que nossos itens de teste estão presentes
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemEstoque.getId())));
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemNoLimiteMinimo.getId())));
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemAbaixoMinimo.getId())));

      // Todos os grupos devem estar carregados
      resultado.stream()
          .filter(i -> i.getGrupo() != null)
          .forEach(item -> assertNotNull(item.getGrupo().getDescricao()));
    }
  }

  // ========== Testes de Edge Cases ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Deve tratar item sem grupo (grupo null)")
    void deveTratarItemSemGrupo() {
      // Given - Item sem grupo
      Item itemSemGrupo = new Item();
      itemSemGrupo.setNome("Item Avulso Test");
      itemSemGrupo.setSaldo(BigDecimal.ZERO);
      itemSemGrupo.setQtdeMinima(BigDecimal.valueOf(5));
      itemSemGrupo.setGrupo(null);
      entityManager.persist(itemSemGrupo);
      entityManager.flush();

      Specification<Item> spec = ItemSpecifications.forRelatorioItensSemEstoque();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then - Deve retornar o item sem grupo
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemGrupo.getId())));
    }

    @Test
    @DisplayName("Não deve retornar itens com saldo null na busca por saldo zero")
    void naoDeveRetornarItemSaldoNullNaBuscaSaldoZero() {
      // Given - Item com saldo null
      Item itemSaldoNull = new Item();
      itemSaldoNull.setNome("Item Saldo Null Test");
      itemSaldoNull.setSaldo(null);
      itemSaldoNull.setQtdeMinima(BigDecimal.valueOf(5));
      itemSaldoNull.setGrupo(grupoEletronicos);
      entityManager.persist(itemSaldoNull);
      entityManager.flush();

      Specification<Item> spec = ItemSpecifications.bySaldoZero();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then - NULL != 0 em SQL, então não deve retornar o item com saldo null
      assertFalse(resultado.stream().anyMatch(i -> i.getId().equals(itemSaldoNull.getId())));
    }

    @Test
    @DisplayName("Deve garantir que qtdeMinima não pode ser null (constraint do banco)")
    void deveGarantirQtdeMinimaNotNull() {
      // Given - Tentativa de criar item com qtdeMinima null
      Item itemQtdeMinimaNull = new Item();
      itemQtdeMinimaNull.setNome("Item QtdeMinima Null Test");
      itemQtdeMinimaNull.setSaldo(BigDecimal.ZERO);
      itemQtdeMinimaNull.setQtdeMinima(null);
      itemQtdeMinimaNull.setGrupo(grupoEletronicos);

      // When/Then - Deve lançar exceção devido à constraint NOT NULL no banco
      assertThrows(
          PersistenceException.class,
          () -> entityManager.persistAndFlush(itemQtdeMinimaNull),
          "Deve rejeitar item com qtdeMinima NULL devido à constraint do banco");
    }

    @Test
    @DisplayName(
        "Deve retornar apenas itens com qtdeMinima definida na busca de saldo <= qtdeMinima")
    void deveRetornarApenasItensComQtdeMinimaDefinida() {
      // Given - Todos os itens do setUp têm qtdeMinima definida

      Specification<Item> spec = ItemSpecifications.bySaldoMenorOuIgualQtdeMinima();

      // When
      List<Item> resultado = repository.findAll(spec);

      // Then - Todos os itens retornados devem ter qtdeMinima definida
      resultado.forEach(
          item ->
              assertNotNull(
                  item.getQtdeMinima(),
                  () -> "Item " + item.getNome() + " não deveria ter qtdeMinima null"));
    }

    @Test
    @DisplayName("Deve combinar múltiplas specifications com AND")
    void deveCombinarMultiplasSpecificationsComAnd() {
      // Given
      Specification<Item> saldoZero = ItemSpecifications.bySaldoZero();
      Specification<Item> comGrupo = ItemSpecifications.withGrupoFetch();

      // When
      List<Item> resultado = repository.findAll(saldoZero.and(comGrupo));

      // Then
      assertTrue(resultado.stream().anyMatch(i -> i.getId().equals(itemSemEstoque.getId())));
    }
  }
}

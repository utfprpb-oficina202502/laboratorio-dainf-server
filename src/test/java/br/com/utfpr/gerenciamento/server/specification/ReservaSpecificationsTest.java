package br.com.utfpr.gerenciamento.server.specification;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.model.*;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
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
 * Testes de integração para ReservaSpecifications.
 *
 * <p>Valida as Specifications usadas nos relatórios de reservas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReservaSpecificationsTest {

  /** Threshold de performance configurável: 5s em CI, 1s local. */
  private static final long PERFORMANCE_THRESHOLD_MS = System.getenv("CI") != null ? 5000 : 1000;

  private static final int RESERVAS_INICIAIS_MULTIMETRO = 2;
  private static final int RESERVAS_ADICIONADAS = 50;

  @Autowired private TestEntityManager entityManager;

  @Autowired private ReservaRepository repository;

  private Usuario usuario;
  private Item itemMultimetro;
  private Item itemOsciloscopio;
  private Reserva reservaComMultimetro;
  private Reserva reservaComOsciloscopio;
  private Reserva reservaComAmbosItens;

  @BeforeEach
  void setUp() {
    // Criar permissão e usuário
    Permissao permissao = criarPermissao("ROLE_ALUNO");
    entityManager.persist(permissao);

    usuario = criarUsuario("aluno@teste.com", "Aluno Teste", permissao);
    entityManager.persist(usuario);

    // Criar itens
    itemMultimetro = criarItem("Multímetro Digital");
    entityManager.persist(itemMultimetro);

    itemOsciloscopio = criarItem("Osciloscópio");
    entityManager.persist(itemOsciloscopio);

    // Criar reservas
    reservaComMultimetro = criarReserva(usuario, LocalDate.now(), LocalDate.now().plusDays(7));
    adicionarItemNaReserva(reservaComMultimetro, itemMultimetro, BigDecimal.ONE);
    entityManager.persist(reservaComMultimetro);

    reservaComOsciloscopio =
        criarReserva(usuario, LocalDate.now().minusDays(5), LocalDate.now().plusDays(2));
    adicionarItemNaReserva(reservaComOsciloscopio, itemOsciloscopio, BigDecimal.valueOf(2));
    entityManager.persist(reservaComOsciloscopio);

    reservaComAmbosItens =
        criarReserva(usuario, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
    adicionarItemNaReserva(reservaComAmbosItens, itemMultimetro, BigDecimal.ONE);
    adicionarItemNaReserva(reservaComAmbosItens, itemOsciloscopio, BigDecimal.ONE);
    entityManager.persist(reservaComAmbosItens);

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
    item.setQtdeMinima(BigDecimal.ONE);
    item.setSaldo(BigDecimal.TEN);
    return item;
  }

  private Reserva criarReserva(Usuario u, LocalDate dataReserva, LocalDate dataRetirada) {
    Reserva reserva = new Reserva();
    reserva.setUsuario(u);
    reserva.setDataReserva(dataReserva);
    reserva.setDataRetirada(dataRetirada);
    reserva.setDescricao("Reserva para teste");
    reserva.setReservaItem(new ArrayList<>());
    return reserva;
  }

  private void adicionarItemNaReserva(Reserva reserva, Item item, BigDecimal qtde) {
    ReservaItem reservaItem = new ReservaItem();
    reservaItem.setReserva(reserva);
    reservaItem.setItem(item);
    reservaItem.setQtde(qtde);
    reserva.getReservaItem().add(reservaItem);
  }

  // ========== byItemId() ==========

  @Nested
  @DisplayName("byItemId()")
  class ByItemIdTests {

    @Test
    @DisplayName("Deve retornar reservas que contêm o item especificado")
    void deveRetornarReservasComItem() {
      // Given
      Specification<Reserva> spec = ReservaSpecifications.byItemId(itemMultimetro.getId());

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      List<Long> ids = resultado.stream().map(Reserva::getId).toList();
      assertTrue(ids.contains(reservaComMultimetro.getId()));
      assertTrue(ids.contains(reservaComAmbosItens.getId()));
    }

    @Test
    @DisplayName("Deve retornar apenas reservas do item específico")
    void deveRetornarApenasReservasDoItemEspecifico() {
      // Given
      Specification<Reserva> spec = ReservaSpecifications.byItemId(itemOsciloscopio.getId());

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      List<Long> ids = resultado.stream().map(Reserva::getId).toList();
      assertTrue(ids.contains(reservaComOsciloscopio.getId()));
      assertTrue(ids.contains(reservaComAmbosItens.getId()));
      assertFalse(ids.contains(reservaComMultimetro.getId()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando item não tem reservas")
    void deveRetornarListaVaziaQuandoItemNaoTemReservas() {
      // Given - Item sem reservas
      Item itemSemReservas = criarItem("Item Sem Reservas");
      entityManager.persist(itemSemReservas);
      entityManager.flush();

      Specification<Reserva> spec = ReservaSpecifications.byItemId(itemSemReservas.getId());

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar todas as reservas quando itemId é null")
    void deveRetornarTodasReservasQuandoItemIdNull() {
      // Given
      Specification<Reserva> spec = ReservaSpecifications.byItemId(null);

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertEquals(3, resultado.size());
    }

    @Test
    @DisplayName("Deve prevenir duplicação de resultados")
    void devePrevenirDuplicacao() {
      // Given - Reserva com múltiplos itens do mesmo tipo
      // reservaComAmbosItens tem 2 itens diferentes, não deve duplicar

      Specification<Reserva> spec = ReservaSpecifications.byItemId(itemMultimetro.getId());

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then - Sem duplicatas
      long reservasUnicas = resultado.stream().map(Reserva::getId).distinct().count();
      assertEquals(resultado.size(), reservasUnicas);
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
      Specification<Reserva> spec = ReservaSpecifications.withFetchJoins();

      // When
      entityManager.clear();
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertFalse(resultado.isEmpty());

      // Acessar dados relacionados após clear (entidades detached)
      resultado.forEach(
          reserva -> {
            assertNotNull(reserva.getUsuario());
            assertNotNull(reserva.getUsuario().getNome());

            assertNotNull(reserva.getReservaItem());
            reserva
                .getReservaItem()
                .forEach(
                    ri -> {
                      assertNotNull(ri.getItem());
                      assertNotNull(ri.getItem().getNome());
                    });
          });
    }

    @Test
    @DisplayName("Deve executar count query sem JOIN FETCH")
    void deveExecutarCountSemFetch() {
      // Given
      Specification<Reserva> spec = ReservaSpecifications.withFetchJoins();

      // When
      long count = repository.count(spec);

      // Then
      assertEquals(3L, count);
    }

    @Test
    @DisplayName("Deve prevenir duplicação com distinct")
    void devePrevenirDuplicacaoComDistinct() {
      // Given
      Specification<Reserva> spec = ReservaSpecifications.withFetchJoins();

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      long reservasUnicas = resultado.stream().map(Reserva::getId).distinct().count();
      assertEquals(resultado.size(), reservasUnicas);
    }
  }

  // ========== forRelatorioReservaDoItem() ==========

  @Nested
  @DisplayName("forRelatorioReservaDoItem()")
  class ForRelatorioReservaDoItemTests {

    @Test
    @DisplayName("Deve combinar filtro por itemId com fetch joins")
    void deveCombinarFiltroComFetch() {
      // Given
      Specification<Reserva> spec =
          ReservaSpecifications.forRelatorioReservaDoItem(itemMultimetro.getId());

      // When
      entityManager.clear();
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertEquals(2, resultado.size());

      // Dados relacionados devem estar carregados
      resultado.forEach(
          reserva -> {
            assertNotNull(reserva.getUsuario().getNome());
            assertFalse(reserva.getReservaItem().isEmpty());
          });
    }

    @Test
    @DisplayName("Deve carregar quantidade correta de itens por reserva")
    void deveCarregarQuantidadeCorretaDeItens() {
      // Given
      Specification<Reserva> spec =
          ReservaSpecifications.forRelatorioReservaDoItem(itemMultimetro.getId());

      // When
      entityManager.clear();
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      // reservaComMultimetro tem 1 item, reservaComAmbosItens tem 2 itens
      Reserva reservaUmItem =
          resultado.stream()
              .filter(r -> r.getId().equals(reservaComMultimetro.getId()))
              .findFirst()
              .orElseThrow();
      assertEquals(1, reservaUmItem.getReservaItem().size());

      Reserva reservaDoisItens =
          resultado.stream()
              .filter(r -> r.getId().equals(reservaComAmbosItens.getId()))
              .findFirst()
              .orElseThrow();
      assertEquals(2, reservaDoisItens.getReservaItem().size());
    }
  }

  // ========== Edge Cases ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Deve tratar reserva sem itens")
    void deveTratarReservaSemItens() {
      // Given - Reserva vazia (sem itens)
      Reserva reservaVazia = criarReserva(usuario, LocalDate.now(), LocalDate.now().plusDays(1));
      entityManager.persist(reservaVazia);
      entityManager.flush();

      Specification<Reserva> spec = ReservaSpecifications.withFetchJoins();

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then - Deve incluir a reserva vazia
      assertEquals(4, resultado.size());
      assertTrue(resultado.stream().anyMatch(r -> r.getId().equals(reservaVazia.getId())));
    }

    @Test
    @DisplayName("Deve tratar item inexistente")
    void deveTratarItemInexistente() {
      // Given
      Long idInexistente = 999999L;
      Specification<Reserva> spec = ReservaSpecifications.byItemId(idInexistente);

      // When
      List<Reserva> resultado = repository.findAll(spec);

      // Then
      assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve manter performance com grandes volumes")
    void deveManterPerformanceComGrandesVolumes() {
      // Given - Criar reservas adicionais
      for (int i = 0; i < RESERVAS_ADICIONADAS; i++) {
        Reserva reserva =
            criarReserva(usuario, LocalDate.now().minusDays(i), LocalDate.now().plusDays(i));
        adicionarItemNaReserva(reserva, itemMultimetro, BigDecimal.ONE);
        entityManager.persist(reserva);
      }
      entityManager.flush();

      Specification<Reserva> spec =
          ReservaSpecifications.forRelatorioReservaDoItem(itemMultimetro.getId());

      // When
      entityManager.clear();
      long inicio = System.currentTimeMillis();
      List<Reserva> resultado = repository.findAll(spec);
      long duracao = System.currentTimeMillis() - inicio;

      // Then
      int totalEsperado = RESERVAS_ADICIONADAS + RESERVAS_INICIAIS_MULTIMETRO;
      assertEquals(
          totalEsperado,
          resultado.size(),
          () ->
              "Esperadas "
                  + RESERVAS_ADICIONADAS
                  + " reservas criadas + "
                  + RESERVAS_INICIAIS_MULTIMETRO
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
          r -> {
            assertNotNull(r.getUsuario().getNome());
            assertNotNull(r.getReservaItem());
          });
    }
  }
}

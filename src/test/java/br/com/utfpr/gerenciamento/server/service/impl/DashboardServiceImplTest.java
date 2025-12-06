package br.com.utfpr.gerenciamento.server.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.dto.dashboards.*;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.EmprestimoItem;
import br.com.utfpr.gerenciamento.server.model.Item;
import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.ReservaItem;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardEmprestimoCountRange;
import br.com.utfpr.gerenciamento.server.repository.EmprestimoRepository;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceImplTest {

  private static final String TEST_USERNAME = "aluno.teste@utfpr.edu.br";

  @Mock private EmprestimoRepository emprestimoRepository;
  @Mock private ReservaRepository reservaRepository;

  @InjectMocks private DashboardServiceImpl dashboardService;

  private LocalDate dtIni;
  private LocalDate dtFim;

  @BeforeEach
  void setUp() {
    dtIni = LocalDate.of(2025, 6, 1);
    dtFim = LocalDate.of(2025, 10, 31);
  }

  @Test
  void testFindDadosEmprestimoCountRange_deveRetornarDadosCorretamente() {
    // Arrange
    DashboardEmprestimoCountRange mockResult =
        new DashboardEmprestimoCountRange(100L, 75L, 10L, 15L);

    when(emprestimoRepository.countEmprestimosByStatusInRange(dtIni, dtFim)).thenReturn(mockResult);

    // Act
    DashboardEmprestimoCountRangeResponseDto result =
        dashboardService.findDadosEmprestimoCountRange(dtIni, dtFim);

    // Assert
    assertNotNull(result);
    assertEquals(100L, result.total());
    assertEquals(75L, result.emAndamento());
    assertEquals(10L, result.emAtraso());
    assertEquals(15L, result.finalizado());

    verify(emprestimoRepository, times(1)).countEmprestimosByStatusInRange(dtIni, dtFim);
  }

  @Test
  void testFindDadosEmprestimoCountRange_QuandoResultRetornaNulo_DeveEntregarValorPadrao() {
    when(emprestimoRepository.countEmprestimosByStatusInRange(dtIni, dtFim)).thenReturn(null);

    DashboardEmprestimoCountRangeResponseDto result =
        dashboardService.findDadosEmprestimoCountRange(dtIni, dtFim);

    assertNotNull(result);
    assertEquals(0L, result.total());
    assertEquals(0L, result.emAndamento());
    assertEquals(0L, result.emAtraso());
    assertEquals(0L, result.finalizado());
  }

  @Test
  void testFindDadosEmprestimoCountRange_comZeroEmprestimos() {
    // Arrange
    DashboardEmprestimoCountRange mockResult = new DashboardEmprestimoCountRange(0L, 0L, 0L, 0L);

    when(emprestimoRepository.countEmprestimosByStatusInRange(dtIni, dtFim)).thenReturn(mockResult);

    // Act
    DashboardEmprestimoCountRangeResponseDto result =
        dashboardService.findDadosEmprestimoCountRange(dtIni, dtFim);

    // Assert
    assertNotNull(result);
    assertEquals(0L, result.total());
    assertEquals(0L, result.emAndamento());
    assertEquals(0L, result.emAtraso());
    assertEquals(0L, result.finalizado());
  }

  // ========== TESTES DO DASHBOARD PESSOAL DO USUARIO ==========

  @Nested
  @DisplayName("findEstatisticasUsuarioLogado")
  class FindEstatisticasUsuarioLogadoTest {

    @Test
    @DisplayName("Deve retornar estatisticas com dados corretos")
    void deveRetornarEstatisticasComDadosCorretos() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        EstatisticasEmprestimoProjection stats =
            new EstatisticasEmprestimoProjection(5L, 2L, 10L); // emAberto, emAtraso, total
        LocalDate proximaDevolucao = LocalDate.now().plusDays(3);

        when(emprestimoRepository.countEstatisticasByUsername(TEST_USERNAME)).thenReturn(stats);
        when(emprestimoRepository.findProximaDevolucaoByUsername(TEST_USERNAME))
            .thenReturn(proximaDevolucao);

        // Act
        EstatisticasUsuarioDto result = dashboardService.findEstatisticasUsuarioLogado();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.emprestimosEmAberto());
        assertEquals(2, result.emprestimosEmAtraso());
        assertEquals(10, result.emprestimosTotal());
        assertEquals(proximaDevolucao, result.proximaDevolucao());
        assertEquals(3, result.diasParaProximaDevolucao());

        verify(emprestimoRepository).countEstatisticasByUsername(TEST_USERNAME);
        verify(emprestimoRepository).findProximaDevolucaoByUsername(TEST_USERNAME);
      }
    }

    @Test
    @DisplayName("Deve retornar zeros quando usuario nao tem emprestimos")
    void deveRetornarZerosQuandoSemEmprestimos() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        EstatisticasEmprestimoProjection stats = new EstatisticasEmprestimoProjection(0L, 0L, 0L);

        when(emprestimoRepository.countEstatisticasByUsername(TEST_USERNAME)).thenReturn(stats);
        when(emprestimoRepository.findProximaDevolucaoByUsername(TEST_USERNAME)).thenReturn(null);

        // Act
        EstatisticasUsuarioDto result = dashboardService.findEstatisticasUsuarioLogado();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.emprestimosEmAberto());
        assertEquals(0, result.emprestimosEmAtraso());
        assertEquals(0, result.emprestimosTotal());
        assertNull(result.proximaDevolucao());
        assertNull(result.diasParaProximaDevolucao());
      }
    }

    @Test
    @DisplayName("Deve retornar dias negativos quando emprestimo esta atrasado")
    void deveRetornarDiasNegativosQuandoAtrasado() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        EstatisticasEmprestimoProjection stats = new EstatisticasEmprestimoProjection(0L, 1L, 1L);
        LocalDate proximaDevolucao = LocalDate.now().minusDays(5); // 5 dias atrasado

        when(emprestimoRepository.countEstatisticasByUsername(TEST_USERNAME)).thenReturn(stats);
        when(emprestimoRepository.findProximaDevolucaoByUsername(TEST_USERNAME))
            .thenReturn(proximaDevolucao);

        // Act
        EstatisticasUsuarioDto result = dashboardService.findEstatisticasUsuarioLogado();

        // Assert
        assertEquals(-5, result.diasParaProximaDevolucao());
      }
    }
  }

  @Nested
  @DisplayName("findItensFrequentesUsuarioLogado")
  class FindItensFrequentesUsuarioLogadoTest {

    @Test
    @DisplayName("Deve retornar lista de itens frequentes")
    void deveRetornarListaDeItensFrequentes() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        List<ItemFrequenteUsuarioDto> itens =
            List.of(
                new ItemFrequenteUsuarioDto(1L, "Multimetro", 5L, BigDecimal.TEN),
                new ItemFrequenteUsuarioDto(2L, "Osciloscopio", 3L, BigDecimal.valueOf(5)));

        when(emprestimoRepository.findItensMaisEmprestadosByUsername(
                TEST_USERNAME, PageRequest.of(0, 5)))
            .thenReturn(itens);

        // Act
        List<ItemFrequenteUsuarioDto> result = dashboardService.findItensFrequentesUsuarioLogado(5);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Multimetro", result.getFirst().itemNome());
        assertEquals(5L, result.getFirst().qtde());
      }
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando usuario nunca emprestou")
    void deveRetornarListaVaziaQuandoSemEmprestimos() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        when(emprestimoRepository.findItensMaisEmprestadosByUsername(
                TEST_USERNAME, PageRequest.of(0, 5)))
            .thenReturn(List.of());

        // Act
        List<ItemFrequenteUsuarioDto> result = dashboardService.findItensFrequentesUsuarioLogado(5);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("findHistoricoUsoUsuarioLogado")
  class FindHistoricoUsoUsuarioLogadoTest {

    @Test
    @DisplayName("Deve retornar historico de uso com labels em pt-BR")
    void deveRetornarHistoricoComLabelsEmPortugues() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        List<Object[]> resultados = List.of(new Object[] {2025, 1, 3L}, new Object[] {2025, 2, 5L});

        when(emprestimoRepository.countEmprestimosPorMesByUsername(
                eq(TEST_USERNAME), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(resultados);

        // Act
        List<HistoricoUsoMensalDto> result = dashboardService.findHistoricoUsoUsuarioLogado(6);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2025-01", result.get(0).mes());
        assertEquals(3, result.get(0).quantidade());
        assertEquals("2025-02", result.get(1).mes());
        assertEquals(5, result.get(1).quantidade());
      }
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nao ha historico")
    void deveRetornarListaVaziaQuandoSemHistorico() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        when(emprestimoRepository.countEmprestimosPorMesByUsername(
                eq(TEST_USERNAME), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());

        // Act
        List<HistoricoUsoMensalDto> result = dashboardService.findHistoricoUsoUsuarioLogado(6);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("findAtividadesUsuarioLogado")
  class FindAtividadesUsuarioLogadoTest {

    @Test
    @DisplayName("Deve combinar emprestimos e reservas ordenados por data")
    void deveCombinarEmprestimosEReservasOrdenados() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        Emprestimo emprestimo = criarEmprestimoComItens(1L, LocalDate.now().minusDays(2), null);
        Reserva reserva = criarReservaComItens(1L, LocalDate.now().minusDays(1));

        when(emprestimoRepository.findEmprestimosParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of(emprestimo));
        when(reservaRepository.findReservasParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of(reserva));

        // Act
        List<AtividadeUsuarioDto> result = dashboardService.findAtividadesUsuarioLogado(20);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Reserva mais recente deve vir primeiro
        assertEquals("RESERVA_CRIADA", result.get(0).tipo());
        assertEquals("EMPRESTIMO_RETIRADA", result.get(1).tipo());
      }
    }

    @Test
    @DisplayName("Deve incluir atividade de devolucao quando emprestimo foi devolvido")
    void deveIncluirAtividadeDeDevolucao() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        Emprestimo emprestimo =
            criarEmprestimoComItens(1L, LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        when(emprestimoRepository.findEmprestimosParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of(emprestimo));
        when(reservaRepository.findReservasParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of());

        // Act
        List<AtividadeUsuarioDto> result = dashboardService.findAtividadesUsuarioLogado(20);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> "EMPRESTIMO_DEVOLUCAO".equals(a.tipo())));
        assertTrue(result.stream().anyMatch(a -> "EMPRESTIMO_RETIRADA".equals(a.tipo())));
      }
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando usuario nao tem atividades")
    void deveRetornarListaVaziaQuandoSemAtividades() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        when(emprestimoRepository.findEmprestimosParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of());
        when(reservaRepository.findReservasParaAtividadesByUsername(
                eq(TEST_USERNAME), any(PageRequest.class)))
            .thenReturn(List.of());

        // Act
        List<AtividadeUsuarioDto> result = dashboardService.findAtividadesUsuarioLogado(20);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("findEventosCalendarioUsuarioLogado")
  class FindEventosCalendarioUsuarioLogadoTest {

    @Test
    @DisplayName("Deve retornar evento de retirada")
    void deveRetornarEventoDeRetirada() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        LocalDate dataEmprestimo = LocalDate.of(2025, 6, 15);
        Emprestimo emprestimo = criarEmprestimoComItens(1L, dataEmprestimo, null);
        emprestimo.setPrazoDevolucao(LocalDate.of(2025, 6, 30));

        when(emprestimoRepository.findEmprestimosParaCalendarioByUsername(
                TEST_USERNAME, dtIni, dtFim))
            .thenReturn(List.of(emprestimo));

        // Act
        List<EventoCalendarioDto> result =
            dashboardService.findEventosCalendarioUsuarioLogado(dtIni, dtFim);

        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(e -> "RETIRADA".equals(e.tipo())));
      }
    }

    @Test
    @DisplayName("Deve retornar evento de devolucao realizada")
    void deveRetornarEventoDeDevolucaoRealizada() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        LocalDate dataEmprestimo = LocalDate.of(2025, 6, 10);
        LocalDate dataDevolucao = LocalDate.of(2025, 6, 20);
        Emprestimo emprestimo = criarEmprestimoComItens(1L, dataEmprestimo, dataDevolucao);
        emprestimo.setPrazoDevolucao(LocalDate.of(2025, 6, 25));

        when(emprestimoRepository.findEmprestimosParaCalendarioByUsername(
                TEST_USERNAME, dtIni, dtFim))
            .thenReturn(List.of(emprestimo));

        // Act
        List<EventoCalendarioDto> result =
            dashboardService.findEventosCalendarioUsuarioLogado(dtIni, dtFim);

        // Assert
        assertTrue(result.stream().anyMatch(e -> "DEVOLUCAO_REALIZADA".equals(e.tipo())));
      }
    }

    @Test
    @DisplayName("Deve retornar evento atrasado quando prazo venceu")
    void deveRetornarEventoAtrasadoQuandoPrazoVenceu() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        // Prazo vencido que esta dentro do range de busca
        LocalDate prazoDevolucao = LocalDate.now().minusDays(2);
        LocalDate dataEmprestimo = prazoDevolucao.minusDays(7);
        LocalDate inicioRange = prazoDevolucao.minusDays(5);
        LocalDate fimRange = LocalDate.now().plusDays(5);

        Emprestimo emprestimo = criarEmprestimoComItens(1L, dataEmprestimo, null);
        emprestimo.setPrazoDevolucao(prazoDevolucao);

        when(emprestimoRepository.findEmprestimosParaCalendarioByUsername(
                TEST_USERNAME, inicioRange, fimRange))
            .thenReturn(List.of(emprestimo));

        // Act
        List<EventoCalendarioDto> result =
            dashboardService.findEventosCalendarioUsuarioLogado(inicioRange, fimRange);

        // Assert
        assertTrue(result.stream().anyMatch(e -> "ATRASADO".equals(e.tipo())));
      }
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nao ha eventos no periodo")
    void deveRetornarListaVaziaQuandoSemEventos() {
      try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
        // Arrange
        securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn(TEST_USERNAME);

        when(emprestimoRepository.findEmprestimosParaCalendarioByUsername(
                TEST_USERNAME, dtIni, dtFim))
            .thenReturn(List.of());

        // Act
        List<EventoCalendarioDto> result =
            dashboardService.findEventosCalendarioUsuarioLogado(dtIni, dtFim);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
      }
    }
  }

  // ========== METODOS AUXILIARES ==========

  private Emprestimo criarEmprestimoComItens(
      Long id, LocalDate dataEmprestimo, LocalDate dataDevolucao) {
    Item item = new Item();
    item.setId(1L);
    item.setNome("Multimetro Digital");

    EmprestimoItem emprestimoItem = new EmprestimoItem();
    emprestimoItem.setId(1L);
    emprestimoItem.setItem(item);
    emprestimoItem.setQtde(BigDecimal.ONE);

    Set<EmprestimoItem> itens = new HashSet<>();
    itens.add(emprestimoItem);

    Emprestimo emprestimo = new Emprestimo();
    emprestimo.setId(id);
    emprestimo.setDataEmprestimo(dataEmprestimo);
    emprestimo.setDataDevolucao(dataDevolucao);
    emprestimo.setPrazoDevolucao(dataEmprestimo.plusDays(7));
    emprestimo.setEmprestimoItem(itens);

    emprestimoItem.setEmprestimo(emprestimo);

    return emprestimo;
  }

  private Reserva criarReservaComItens(Long id, LocalDate dataReserva) {
    Item item = new Item();
    item.setId(2L);
    item.setNome("Osciloscopio");

    ReservaItem reservaItem = new ReservaItem();
    reservaItem.setId(1L);
    reservaItem.setItem(item);
    reservaItem.setQtde(BigDecimal.ONE);

    List<ReservaItem> itens = new ArrayList<>();
    itens.add(reservaItem);

    Reserva reserva = new Reserva();
    reserva.setId(id);
    reserva.setDataReserva(dataReserva);
    reserva.setReservaItem(itens);

    reservaItem.setReserva(reserva);

    return reserva;
  }
}

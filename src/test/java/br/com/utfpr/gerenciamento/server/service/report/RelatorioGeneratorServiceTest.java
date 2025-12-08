package br.com.utfpr.gerenciamento.server.service.report;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.dto.relatorios.*;
import br.com.utfpr.gerenciamento.server.enumeration.FormatoRelatorio;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testes unitários para RelatorioGeneratorService.
 *
 * <p>Foco: testar a orquestração entre ReportDataService e os geradores (PDF/Excel).
 */
class RelatorioGeneratorServiceTest {

  @Mock private ReportDataService reportDataService;
  @Mock private PdfGeneratorService pdfGeneratorService;
  @Mock private ExcelGeneratorService excelGeneratorService;

  @InjectMocks private RelatorioGeneratorService relatorioGeneratorService;

  private static final byte[] PDF_BYTES = "PDF_CONTENT".getBytes();
  private static final byte[] EXCEL_BYTES = "EXCEL_CONTENT".getBytes();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  // ========== HISTÓRICO DE EMPRÉSTIMO ==========

  @Nested
  @DisplayName("gerarHistoricoEmprestimo()")
  class GerarHistoricoEmprestimoTests {

    private final String documento = "12345678901";
    private List<HistoricoEmprestimoDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              HistoricoEmprestimoDto.builder()
                  .cod(1L)
                  .nomeUsuario("João Silva")
                  .dataEmprestimo(LocalDate.of(2025, 1, 15))
                  .prazoDevolucao(LocalDate.of(2025, 1, 22))
                  .dataDevolucao(LocalDate.of(2025, 1, 20))
                  .situacao("Finalizado")
                  .build());
      when(reportDataService.getHistoricoEmprestimo(documento)).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve gerar PDF com dados corretos")
    void deveGerarPdfComDadosCorretos() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("historico-emprestimo"), any()))
          .thenReturn(PDF_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarHistoricoEmprestimo(documento, FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("historico-emprestimo"), captor.capture());

      Map<String, Object> model = captor.getValue();
      assertEquals(documento, model.get("documento"));
      assertEquals(dados, model.get("emprestimos"));
    }

    @Test
    @DisplayName("Deve gerar Excel chamando ExcelGeneratorService")
    void deveGerarExcel() {
      // Given
      when(excelGeneratorService.generateExcel(
              anyString(),
              any(String[].class),
              anyList(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarHistoricoEmprestimo(documento, FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Histórico de Empréstimo"),
              any(String[].class),
              eq(dados),
              any(),
              any(),
              any(),
              any(),
              any(),
              any());
    }

    @Test
    @DisplayName("Deve buscar dados do ReportDataService")
    void deveBuscarDadosDoReportDataService() {
      // Given
      when(pdfGeneratorService.generatePdf(anyString(), any())).thenReturn(PDF_BYTES);

      // When
      relatorioGeneratorService.gerarHistoricoEmprestimo(documento, FormatoRelatorio.PDF);

      // Then
      verify(reportDataService).getHistoricoEmprestimo(documento);
    }
  }

  // ========== ITENS SEM ESTOQUE ==========

  @Nested
  @DisplayName("gerarItensSemEstoque()")
  class GerarItensSemEstoqueTests {

    private List<ItemSemEstoqueDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              ItemSemEstoqueDto.builder()
                  .cod(1L)
                  .nome("Multímetro Digital")
                  .patrimonio(BigInteger.valueOf(12345))
                  .siorg(BigInteger.valueOf(67890))
                  .qtdeMinima(BigDecimal.valueOf(5))
                  .grupo("Instrumentos de Medição")
                  .build());
      when(reportDataService.getItensSemEstoque()).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve gerar PDF para itens sem estoque")
    void deveGerarPdfParaItensSemEstoque() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("itens-sem-estoque"), any())).thenReturn(PDF_BYTES);

      // When
      byte[] resultado = relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("itens-sem-estoque"), captor.capture());
      assertEquals(dados, captor.getValue().get("itens"));
    }

    @Test
    @DisplayName("Deve gerar Excel chamando ExcelGeneratorService")
    void deveGerarExcel() {
      // Given
      when(excelGeneratorService.generateExcel(
              anyString(),
              any(String[].class),
              anyList(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado = relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Itens Sem Estoque"),
              any(String[].class),
              eq(dados),
              any(),
              any(),
              any(),
              any(),
              any(),
              any());
    }
  }

  // ========== EMPRÉSTIMOS REALIZADOS ==========

  @Nested
  @DisplayName("gerarEmprestimosRealizados()")
  class GerarEmprestimosRealizadosTests {

    private final LocalDate dataInicio = LocalDate.of(2025, 1, 1);
    private final LocalDate dataFim = LocalDate.of(2025, 6, 30);
    private List<EmprestimoRealizadoDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              EmprestimoRealizadoDto.builder()
                  .cod(1L)
                  .usuarioEmprestimo("Maria Santos")
                  .usuarioResponsavel("Admin")
                  .dataEmprestimo(LocalDate.of(2025, 3, 15))
                  .situacao("Em andamento")
                  .build());
      when(reportDataService.getEmprestimosRealizados(dataInicio, dataFim)).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve gerar PDF com período correto")
    void deveGerarPdfComPeriodoCorreto() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("emprestimos-realizados"), any()))
          .thenReturn(PDF_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarEmprestimosRealizados(
              dataInicio, dataFim, FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("emprestimos-realizados"), captor.capture());

      Map<String, Object> model = captor.getValue();
      assertEquals(dataInicio, model.get("dataInicio"));
      assertEquals(dataFim, model.get("dataFim"));
      assertEquals(dados, model.get("emprestimos"));
    }

    @Test
    @DisplayName("Deve gerar Excel chamando ExcelGeneratorService")
    void deveGerarExcel() {
      // Given
      when(excelGeneratorService.generateExcel(
              anyString(), any(String[].class), anyList(), any(), any(), any(), any(), any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarEmprestimosRealizados(
              dataInicio, dataFim, FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Empréstimos Realizados"),
              any(String[].class),
              eq(dados),
              any(),
              any(),
              any(),
              any(),
              any());
    }
  }

  // ========== RESERVAS DO ITEM ==========

  @Nested
  @DisplayName("gerarReservasDoItem()")
  class GerarReservasDoItemTests {

    private final Long itemId = 100L;
    private List<ReservaItemDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              ReservaItemDto.builder()
                  .cod(1L)
                  .dataReserva(LocalDate.of(2025, 2, 1))
                  .dataRetirada(LocalDate.of(2025, 2, 5))
                  .qtde(BigDecimal.valueOf(2))
                  .usuarioReserva("Pedro Alves")
                  .nomeItem("Osciloscópio")
                  .build());
      when(reportDataService.getReservasDoItem(itemId)).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve usar nomeItem fornecido quando disponível")
    void deveUsarNomeItemFornecido() {
      // Given
      String nomeItem = "Osciloscópio Digital";
      when(pdfGeneratorService.generatePdf(eq("reserva-item"), any())).thenReturn(PDF_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarReservasDoItem(itemId, nomeItem, FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("reserva-item"), captor.capture());
      assertEquals(nomeItem, captor.getValue().get("nomeItem"));
    }

    @Test
    @DisplayName("Deve extrair nomeItem do primeiro registro quando não fornecido")
    void deveExtrairNomeItemDoPrimeiroRegistro() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("reserva-item"), any())).thenReturn(PDF_BYTES);

      // When
      relatorioGeneratorService.gerarReservasDoItem(itemId, null, FormatoRelatorio.PDF);

      // Then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("reserva-item"), captor.capture());
      assertEquals("Osciloscópio", captor.getValue().get("nomeItem"));
    }

    @Test
    @DisplayName("Deve usar fallback quando lista está vazia e nomeItem não fornecido")
    void deveUsarFallbackQuandoListaVazia() {
      // Given
      when(reportDataService.getReservasDoItem(itemId)).thenReturn(Collections.emptyList());
      when(pdfGeneratorService.generatePdf(eq("reserva-item"), any())).thenReturn(PDF_BYTES);

      // When
      relatorioGeneratorService.gerarReservasDoItem(itemId, null, FormatoRelatorio.PDF);

      // Then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("reserva-item"), captor.capture());
      assertEquals("Item " + itemId, captor.getValue().get("nomeItem"));
    }

    @Test
    @DisplayName("Deve gerar Excel com nome do item no título da planilha")
    void deveGerarExcelComNomeNoTitulo() {
      // Given
      String nomeItem = "Osciloscópio Digital";
      when(excelGeneratorService.generateExcel(
              anyString(), any(String[].class), anyList(), any(), any(), any(), any(), any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarReservasDoItem(itemId, nomeItem, FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Reservas - " + nomeItem),
              any(String[].class),
              anyList(),
              any(),
              any(),
              any(),
              any(),
              any());
    }
  }

  // ========== SOLICITAÇÕES DO ITEM ==========

  @Nested
  @DisplayName("gerarSolicitacoesDoItem()")
  class GerarSolicitacoesDoItemTests {

    private final Long itemId = 200L;
    private List<SolicitacaoItemDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              SolicitacaoItemDto.builder()
                  .cod(1L)
                  .dataSolicitacao(LocalDate.of(2025, 3, 1))
                  .descricao("Compra urgente")
                  .usuarioSolicitacao("Ana Costa")
                  .qtde(BigDecimal.valueOf(10))
                  .nomeItem("Resistor 10k")
                  .build());
      when(reportDataService.getSolicitacoesDoItem(itemId)).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve gerar PDF com solicitações")
    void deveGerarPdfComSolicitacoes() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("solicitacao-item"), any())).thenReturn(PDF_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarSolicitacoesDoItem(
              itemId, "Resistor 10k", FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("solicitacao-item"), captor.capture());
      assertEquals(dados, captor.getValue().get("solicitacoes"));
    }

    @Test
    @DisplayName("Deve usar nomeItem do DTO quando parâmetro é blank")
    void deveUsarNomeItemDoDtoQuandoParametroBlank() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("solicitacao-item"), any())).thenReturn(PDF_BYTES);

      // When
      relatorioGeneratorService.gerarSolicitacoesDoItem(itemId, "   ", FormatoRelatorio.PDF);

      // Then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("solicitacao-item"), captor.capture());
      assertEquals("Resistor 10k", captor.getValue().get("nomeItem"));
    }

    @Test
    @DisplayName("Deve gerar Excel com nome do item no título")
    void deveGerarExcelComNomeNoTitulo() {
      // Given
      when(excelGeneratorService.generateExcel(
              anyString(), any(String[].class), anyList(), any(), any(), any(), any(), any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado =
          relatorioGeneratorService.gerarSolicitacoesDoItem(
              itemId, "Resistor", FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Solicitações - Resistor"),
              any(String[].class),
              anyList(),
              any(),
              any(),
              any(),
              any(),
              any());
    }
  }

  // ========== ITENS QUANTIDADE MÍNIMA ==========

  @Nested
  @DisplayName("gerarItensQtdeMinima()")
  class GerarItensQtdeMinimaTests {

    private List<ItemQtdeMinimaDto> dados;

    @BeforeEach
    void setUpDados() {
      dados =
          List.of(
              ItemQtdeMinimaDto.builder()
                  .cod(1L)
                  .nome("Capacitor 100uF")
                  .grupo("Componentes Eletrônicos")
                  .qtdeMinima(BigDecimal.valueOf(50))
                  .saldo(BigDecimal.valueOf(30))
                  .build());
      when(reportDataService.getItensQtdeMinima()).thenReturn(dados);
    }

    @Test
    @DisplayName("Deve gerar PDF com itens em quantidade mínima")
    void deveGerarPdfComItensQtdeMinima() {
      // Given
      when(pdfGeneratorService.generatePdf(eq("itens-qtde-minima"), any())).thenReturn(PDF_BYTES);

      // When
      byte[] resultado = relatorioGeneratorService.gerarItensQtdeMinima(FormatoRelatorio.PDF);

      // Then
      assertArrayEquals(PDF_BYTES, resultado);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(pdfGeneratorService).generatePdf(eq("itens-qtde-minima"), captor.capture());
      assertEquals(dados, captor.getValue().get("itens"));
    }

    @Test
    @DisplayName("Deve gerar Excel chamando ExcelGeneratorService")
    void deveGerarExcel() {
      // Given
      when(excelGeneratorService.generateExcel(
              anyString(), any(String[].class), anyList(), any(), any(), any(), any(), any()))
          .thenReturn(EXCEL_BYTES);

      // When
      byte[] resultado = relatorioGeneratorService.gerarItensQtdeMinima(FormatoRelatorio.EXCEL);

      // Then
      assertArrayEquals(EXCEL_BYTES, resultado);
      verify(excelGeneratorService)
          .generateExcel(
              eq("Itens Qtde Mínima"),
              any(String[].class),
              eq(dados),
              any(),
              any(),
              any(),
              any(),
              any());
    }
  }
}

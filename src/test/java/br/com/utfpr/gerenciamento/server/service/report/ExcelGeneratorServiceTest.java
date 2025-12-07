package br.com.utfpr.gerenciamento.server.service.report;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.exception.RelatorioException;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários para ExcelGeneratorService.
 *
 * <p>Foco nos testes de segurança e funcionalidade: - Limite de registros (prevenção de DoS) -
 * Geração correta de Excel - Exception handling
 */
class ExcelGeneratorServiceTest {

  private ExcelGeneratorService excelGeneratorService;

  @BeforeEach
  void setUp() {
    excelGeneratorService = new ExcelGeneratorService();
  }

  @Test
  @DisplayName("Deve gerar Excel com dados válidos")
  void generateExcel_QuandoDadosValidos_DeveGerarExcelCorretamente() throws Exception {
    // Given
    List<TestDto> dados =
        List.of(
            new TestDto(1L, "Item 1", LocalDate.of(2025, 1, 15)),
            new TestDto(2L, "Item 2", LocalDate.of(2025, 2, 20)));

    String[] headers = {"ID", "Nome", "Data"};

    // When
    byte[] result =
        excelGeneratorService.generateExcel(
            "Teste", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData);

    // Then
    assertNotNull(result);
    assertTrue(result.length > 0);

    // Validar estrutura do Excel
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
      assertEquals(1, workbook.getNumberOfSheets());
      assertEquals("Teste", workbook.getSheetName(0));
      assertEquals(3, workbook.getSheetAt(0).getPhysicalNumberOfRows()); // Header + 2 dados
    }
  }

  @Test
  @DisplayName("Deve gerar Excel com lista vazia")
  void generateExcel_QuandoListaVazia_DeveGerarExcelApenasComHeaders() throws Exception {
    // Given
    List<TestDto> dados = List.of();
    String[] headers = {"ID", "Nome", "Data"};

    // When
    byte[] result =
        excelGeneratorService.generateExcel(
            "Vazio", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData);

    // Then
    assertNotNull(result);
    assertTrue(result.length > 0);

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
      assertEquals(1, workbook.getSheetAt(0).getPhysicalNumberOfRows()); // Apenas header
    }
  }

  @Test
  @DisplayName("Deve lançar RelatorioException quando excede limite de registros")
  void generateExcel_QuandoExcedeLimite_DeveLancarRelatorioException() {
    // Given - Lista com mais de 50.000 registros
    List<TestDto> dados = new ArrayList<>();
    for (int i = 0; i < 50_001; i++) {
      dados.add(new TestDto((long) i, "Item " + i, LocalDate.now()));
    }

    String[] headers = {"ID", "Nome", "Data"};

    // When & Then
    RelatorioException exception =
        assertThrows(
            RelatorioException.class,
            () ->
                excelGeneratorService.generateExcel(
                    "Grande", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData));

    assertTrue(exception.getMessage().contains("excede o limite permitido"));
    assertTrue(exception.getMessage().contains("50001"));
    assertTrue(exception.getMessage().contains("50000"));
  }

  @Test
  @DisplayName("Deve aceitar exatamente o limite de registros")
  void generateExcel_QuandoExatamenteLimite_DeveGerarNormalmente() {
    // Given - Lista com exatamente 50.000 registros
    List<TestDto> dados = new ArrayList<>();
    for (int i = 0; i < 50_000; i++) {
      dados.add(new TestDto((long) i, "Item " + i, LocalDate.now()));
    }

    String[] headers = {"ID", "Nome", "Data"};

    // When & Then
    assertDoesNotThrow(
        () ->
            excelGeneratorService.generateExcel(
                "Limite", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData));
  }

  @Test
  @DisplayName("Deve tratar valores null nas células")
  void generateExcel_QuandoValoresNull_DeveGerarCelulasVazias() throws Exception {
    // Given
    List<TestDto> dados = List.of(new TestDto(null, null, null));

    String[] headers = {"ID", "Nome", "Data"};

    // When
    byte[] result =
        excelGeneratorService.generateExcel(
            "NullValues", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData);

    // Then
    assertNotNull(result);
    assertTrue(result.length > 0);

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
      var row = workbook.getSheetAt(0).getRow(1);
      assertEquals("", row.getCell(0).getStringCellValue());
      assertEquals("", row.getCell(1).getStringCellValue());
      assertEquals("", row.getCell(2).getStringCellValue());
    }
  }

  @Test
  @DisplayName("Deve formatar datas no padrão brasileiro")
  void generateExcel_QuandoTemDatas_DeveFormatarCorretamente() throws Exception {
    // Given
    LocalDate data = LocalDate.of(2025, 12, 7);
    List<TestDto> dados = List.of(new TestDto(1L, "Teste", data));

    String[] headers = {"ID", "Nome", "Data"};

    // When
    byte[] result =
        excelGeneratorService.generateExcel(
            "Datas", headers, dados, TestDto::getId, TestDto::getNome, TestDto::getData);

    // Then
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
      var row = workbook.getSheetAt(0).getRow(1);
      assertEquals("07/12/2025", row.getCell(2).getStringCellValue());
    }
  }

  // DTO para testes - usando classe regular para compatibilidade com Function<T, Object>
  private static class TestDto {
    private final Long id;
    private final String nome;
    private final LocalDate data;

    TestDto(Long id, String nome, LocalDate data) {
      this.id = id;
      this.nome = nome;
      this.data = data;
    }

    public Long getId() {
      return id;
    }

    public String getNome() {
      return nome;
    }

    public LocalDate getData() {
      return data;
    }
  }
}

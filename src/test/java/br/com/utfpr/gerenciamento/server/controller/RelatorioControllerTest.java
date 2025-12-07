package br.com.utfpr.gerenciamento.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.enumeration.FormatoRelatorio;
import br.com.utfpr.gerenciamento.server.service.report.RelatorioGeneratorService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Testes unitários para RelatorioController.
 *
 * <p>Foco nos testes de segurança: - Sanitização de nomes de arquivo (prevenção de Path Traversal)
 * - Geração correta de Content-Disposition header
 */
class RelatorioControllerTest {

  @Mock private RelatorioGeneratorService relatorioGeneratorService;

  @InjectMocks private RelatorioController relatorioController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Deve retornar relatório com Content-Disposition correto")
  void gerarHistoricoEmprestimo_QuandoDadosValidos_DeveRetornarRelatorio() {
    // Given
    String documento = "12345678901";
    byte[] pdfBytes = "PDF content".getBytes();
    when(relatorioGeneratorService.gerarHistoricoEmprestimo(documento, FormatoRelatorio.PDF))
        .thenReturn(pdfBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarHistoricoEmprestimo(documento, FormatoRelatorio.PDF);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(
        response
            .getHeaders()
            .getFirst(HttpHeaders.CONTENT_DISPOSITION)
            .contains("historico-emprestimo-12345678901.pdf"));
  }

  @ParameterizedTest
  @CsvSource({
    "documento-normal, documento-normal",
    "../path/traversal, pathtraversal",
    "../../etc/passwd, etcpasswd",
    "arquivo/com/barras, arquivocombarras",
    "arquivo\\com\\backslash, arquivocombackslash",
    "nome com espacos, nome-com-espacos",
    "nome   multiplos   espacos, nome-multiplos-espacos",
    "<script>alert, scriptalert",
    "nome|pipe, nomepipe",
    "nome:dois:pontos, nomedoispontos"
  })
  @DisplayName("Deve sanitizar nomes de arquivo corretamente")
  void sanitizarNomeArquivo_QuandoCaracteresPerigosos_DeveRemover(String entrada, String esperado)
      throws Exception {
    // Given - Usar reflection para testar método privado
    Method sanitizarMethod =
        RelatorioController.class.getDeclaredMethod("sanitizarNomeArquivo", String.class);
    sanitizarMethod.setAccessible(true);

    // When
    String resultado = (String) sanitizarMethod.invoke(relatorioController, entrada);

    // Then
    assertEquals(esperado, resultado);
  }

  @Test
  @DisplayName("Deve retornar 'relatorio' quando nome é null")
  void sanitizarNomeArquivo_QuandoNull_DeveRetornarPadrao() throws Exception {
    // Given
    Method sanitizarMethod =
        RelatorioController.class.getDeclaredMethod("sanitizarNomeArquivo", String.class);
    sanitizarMethod.setAccessible(true);

    // When
    String resultado = (String) sanitizarMethod.invoke(relatorioController, (String) null);

    // Then
    assertEquals("relatorio", resultado);
  }

  @Test
  @DisplayName("Deve remover caracteres de controle do nome de arquivo")
  void sanitizarNomeArquivo_QuandoCaracteresControle_DeveRemover() throws Exception {
    // Given
    Method sanitizarMethod =
        RelatorioController.class.getDeclaredMethod("sanitizarNomeArquivo", String.class);
    sanitizarMethod.setAccessible(true);

    // When & Then - Null byte (0x00)
    String resultadoNullByte =
        (String) sanitizarMethod.invoke(relatorioController, "test\u0000file");
    assertEquals("testfile", resultadoNullByte);

    // Newline (0x0A)
    String resultadoNewline = (String) sanitizarMethod.invoke(relatorioController, "test\nfile");
    assertEquals("testfile", resultadoNewline);

    // Carriage return (0x0D)
    String resultadoCr = (String) sanitizarMethod.invoke(relatorioController, "test\rfile");
    assertEquals("testfile", resultadoCr);

    // Tab (0x09) - removed as control character
    String resultadoTab = (String) sanitizarMethod.invoke(relatorioController, "test\tfile");
    assertEquals("testfile", resultadoTab);

    // Bell (0x07)
    String resultadoBell = (String) sanitizarMethod.invoke(relatorioController, "test\u0007file");
    assertEquals("testfile", resultadoBell);
  }

  @Test
  @DisplayName("Deve retornar 'relatorio' quando nome é vazio")
  void sanitizarNomeArquivo_QuandoVazio_DeveRetornarPadrao() throws Exception {
    // Given
    Method sanitizarMethod =
        RelatorioController.class.getDeclaredMethod("sanitizarNomeArquivo", String.class);
    sanitizarMethod.setAccessible(true);

    // When
    String resultado = (String) sanitizarMethod.invoke(relatorioController, "");

    // Then
    assertEquals("relatorio", resultado);
  }

  @Test
  @DisplayName("Deve gerar relatório Excel com extensão correta")
  void gerarItensSemEstoque_QuandoFormatoExcel_DeveRetornarXlsx() {
    // Given
    byte[] excelBytes = "Excel content".getBytes();
    when(relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.EXCEL))
        .thenReturn(excelBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarItensSemEstoque(FormatoRelatorio.EXCEL);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(
        response
            .getHeaders()
            .getFirst(HttpHeaders.CONTENT_DISPOSITION)
            .contains("itens-sem-estoque.xlsx"));
  }

  @Test
  @DisplayName("Deve retornar Content-Length correto")
  void gerarRelatorio_DeveRetornarContentLength() {
    // Given
    byte[] pdfBytes = new byte[1024];
    when(relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.PDF)).thenReturn(pdfBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarItensSemEstoque(FormatoRelatorio.PDF);

    // Then
    assertEquals(1024L, response.getHeaders().getContentLength());
  }

  @Test
  @DisplayName("Deve retornar MediaType correto para PDF")
  void gerarRelatorio_QuandoPdf_DeveRetornarMediaTypePdf() {
    // Given
    byte[] pdfBytes = "PDF".getBytes();
    when(relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.PDF)).thenReturn(pdfBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarItensSemEstoque(FormatoRelatorio.PDF);

    // Then
    assertEquals("application/pdf", response.getHeaders().getContentType().toString());
  }

  @Test
  @DisplayName("Deve retornar MediaType correto para Excel")
  void gerarRelatorio_QuandoExcel_DeveRetornarMediaTypeExcel() {
    // Given
    byte[] excelBytes = "Excel".getBytes();
    when(relatorioGeneratorService.gerarItensSemEstoque(FormatoRelatorio.EXCEL))
        .thenReturn(excelBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarItensSemEstoque(FormatoRelatorio.EXCEL);

    // Then
    assertTrue(
        response
            .getHeaders()
            .getContentType()
            .toString()
            .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Test
  @DisplayName("Deve validar que data final não é anterior à data inicial")
  void gerarEmprestimosRealizados_QuandoDataFimAnteriorInicio_DeveLancarExcecao() {
    // Given
    java.time.LocalDate dataInicio = java.time.LocalDate.of(2025, 6, 1);
    java.time.LocalDate dataFim = java.time.LocalDate.of(2025, 5, 1);

    // When & Then
    assertThrows(
        br.com.utfpr.gerenciamento.server.exception.RelatorioException.class,
        () ->
            relatorioController.gerarEmprestimosRealizados(
                dataInicio, dataFim, FormatoRelatorio.PDF));
  }

  @Test
  @DisplayName("Deve validar que data final não está no futuro")
  void gerarEmprestimosRealizados_QuandoDataFimNoFuturo_DeveLancarExcecao() {
    // Given
    java.time.LocalDate dataInicio = java.time.LocalDate.now();
    java.time.LocalDate dataFim = java.time.LocalDate.now().plusDays(30);

    // When & Then
    assertThrows(
        br.com.utfpr.gerenciamento.server.exception.RelatorioException.class,
        () ->
            relatorioController.gerarEmprestimosRealizados(
                dataInicio, dataFim, FormatoRelatorio.PDF));
  }

  @Test
  @DisplayName("Deve validar que período não excede 2 anos")
  void gerarEmprestimosRealizados_QuandoPeriodoExcede2Anos_DeveLancarExcecao() {
    // Given
    java.time.LocalDate dataInicio = java.time.LocalDate.of(2020, 1, 1);
    java.time.LocalDate dataFim = java.time.LocalDate.of(2023, 1, 2);

    // When & Then
    assertThrows(
        br.com.utfpr.gerenciamento.server.exception.RelatorioException.class,
        () ->
            relatorioController.gerarEmprestimosRealizados(
                dataInicio, dataFim, FormatoRelatorio.PDF));
  }

  @Test
  @DisplayName("Deve aceitar período válido de exatamente 2 anos")
  void gerarEmprestimosRealizados_QuandoPeriodoExatamente2Anos_DeveAceitar() {
    // Given
    java.time.LocalDate dataFim = java.time.LocalDate.now();
    java.time.LocalDate dataInicio = dataFim.minusYears(2);
    byte[] pdfBytes = "PDF".getBytes();
    when(relatorioGeneratorService.gerarEmprestimosRealizados(
            dataInicio, dataFim, FormatoRelatorio.PDF))
        .thenReturn(pdfBytes);

    // When
    ResponseEntity<byte[]> response =
        relatorioController.gerarEmprestimosRealizados(dataInicio, dataFim, FormatoRelatorio.PDF);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

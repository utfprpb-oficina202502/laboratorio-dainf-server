package br.com.utfpr.gerenciamento.server.service.report;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.exception.RelatorioException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Testes unitários para PdfGeneratorService.
 *
 * <p>Foco nos testes de segurança: - Whitelist de templates (prevenção de Path Traversal) -
 * Exception handling (prevenção de Information Disclosure)
 */
class PdfGeneratorServiceTest {

  @Mock private TemplateEngine templateEngine;

  private PdfGeneratorService pdfGeneratorService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pdfGeneratorService = new PdfGeneratorService(templateEngine);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "historico-emprestimo",
        "itens-sem-estoque",
        "emprestimos-realizados",
        "reserva-item",
        "solicitacao-item",
        "itens-qtde-minima"
      })
  @DisplayName("Deve aceitar templates da whitelist")
  void generatePdf_QuandoTemplatePermitido_DeveProcessarNormalmente(String templateName) {
    // Given
    Map<String, Object> dados = new HashMap<>();
    String htmlMock = "<html><head><style></style></head><body><h1>Test</h1></body></html>";
    when(templateEngine.process(eq("reports/" + templateName), any(Context.class)))
        .thenReturn(htmlMock);

    // When & Then
    assertDoesNotThrow(() -> pdfGeneratorService.generatePdf(templateName, dados));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "template-nao-existente",
        "../../../etc/passwd",
        "../../config/application.properties",
        "path/traversal/attack",
        "HISTORICO-EMPRESTIMO", // Case sensitive
        " historico-emprestimo ", // Com espaços
        "historico-emprestimo.html" // Com extensão
      })
  @DisplayName("Deve rejeitar templates inválidos, fora da whitelist, null ou vazio")
  void generatePdf_QuandoTemplateInvalido_DeveLancarRelatorioException(String templateName) {
    // Given
    Map<String, Object> dados = new HashMap<>();

    // When & Then
    RelatorioException exception =
        assertThrows(
            RelatorioException.class, () -> pdfGeneratorService.generatePdf(templateName, dados));

    assertEquals("Template de relatório não disponível", exception.getBody().getDetail());
    verifyNoInteractions(templateEngine);
  }

  @Test
  @DisplayName("Deve lançar RelatorioException quando template engine falha")
  void generatePdf_QuandoTemplateEngineFalha_DeveLancarRelatorioException() {
    // Given
    String templateName = "historico-emprestimo";
    Map<String, Object> dados = new HashMap<>();
    when(templateEngine.process(eq("reports/" + templateName), any(Context.class)))
        .thenThrow(new org.thymeleaf.exceptions.TemplateInputException("Template não encontrado"));

    // When & Then
    RelatorioException exception =
        assertThrows(
            RelatorioException.class, () -> pdfGeneratorService.generatePdf(templateName, dados));

    assertEquals("Erro ao processar template de relatório", exception.getBody().getDetail());
    assertNotNull(exception.getCause());
  }

  @Test
  @DisplayName("Deve passar dados para o contexto do template")
  void generatePdf_QuandoDadosFornecidos_DevePassarParaContexto() {
    // Given
    String templateName = "historico-emprestimo";
    Map<String, Object> dados = new HashMap<>();
    dados.put("documento", "12345678901");
    dados.put("emprestimos", java.util.List.of());

    String htmlMock = "<html><head><style></style></head><body><h1>Test</h1></body></html>";
    when(templateEngine.process(eq("reports/" + templateName), any(Context.class)))
        .thenReturn(htmlMock);

    // When
    assertDoesNotThrow(() -> pdfGeneratorService.generatePdf(templateName, dados));

    // Then
    verify(templateEngine).process(eq("reports/" + templateName), any(Context.class));
  }
}

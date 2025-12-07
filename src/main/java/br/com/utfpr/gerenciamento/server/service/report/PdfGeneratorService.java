package br.com.utfpr.gerenciamento.server.service.report;

import br.com.utfpr.gerenciamento.server.exception.RelatorioException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Serviço para geração de PDFs a partir de templates HTML usando Flying Saucer.
 *
 * <p>Fluxo: Template Thymeleaf → HTML processado → PDF via Flying Saucer/OpenPDF
 *
 * <p>Vantagens sobre Jasper Reports: - Templates HTML são mais fáceis de manter - CSS para estilo
 * (web devs podem editar) - Sem compilação de .jrxml em runtime - Debugging simples (visualizar
 * HTML no browser)
 *
 * <p>Segurança: - Whitelist de templates permitidos (prevenção de Path Traversal) - Exceções
 * customizadas sem exposição de detalhes internos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

  /** Whitelist de templates permitidos (prevenção de Path Traversal - OWASP A01:2021). */
  private static final Set<String> TEMPLATES_PERMITIDOS =
      Set.of(
          "historico-emprestimo",
          "itens-sem-estoque",
          "emprestimos-realizados",
          "reserva-item",
          "solicitacao-item",
          "itens-qtde-minima");

  /** Limite máximo de tamanho do PDF gerado em bytes (10MB - prevenção de DoS). */
  private static final int LIMITE_TAMANHO_PDF_BYTES = 10 * 1024 * 1024;

  private final TemplateEngine templateEngine;

  /**
   * Gera PDF a partir de um template Thymeleaf.
   *
   * @param templateName Nome do template em resources/templates/reports/ (sem extensão)
   * @param dados Mapa de variáveis para o template
   * @return Array de bytes do PDF gerado
   * @throws RelatorioException Se o template não for permitido ou ocorrer erro na geração
   */
  public byte[] generatePdf(String templateName, Map<String, Object> dados) {
    log.debug("Gerando PDF para template: {}", templateName);

    // Validar template contra whitelist (prevenção de Path Traversal)
    validarTemplate(templateName);

    try {
      // 1. Processar template Thymeleaf → HTML
      String html = processarTemplate(templateName, dados);

      // 2. Converter HTML → PDF usando Flying Saucer
      return converterHtmlParaPdf(html);

    } catch (TemplateInputException e) {
      log.error("Erro ao processar template {}: {}", templateName, e.getMessage(), e);
      throw new RelatorioException("Erro ao processar template de relatório", e);
    } catch (IOException e) {
      log.error("Erro ao gerar PDF para template {}: {}", templateName, e.getMessage(), e);
      throw new RelatorioException("Erro ao gerar arquivo PDF", e);
    } catch (RelatorioException e) {
      throw e; // Re-throw exceções já tratadas
    } catch (Exception e) {
      log.error(
          "Erro inesperado ao gerar PDF para template {}: {}", templateName, e.getMessage(), e);
      throw new RelatorioException("Erro ao gerar relatório", e);
    }
  }

  /**
   * Valida se o template está na whitelist de templates permitidos.
   *
   * @param templateName Nome do template a validar
   * @throws RelatorioException Se o template não for permitido
   */
  private void validarTemplate(String templateName) {
    if (templateName == null || !TEMPLATES_PERMITIDOS.contains(templateName)) {
      log.warn("Tentativa de acesso a template não permitido");
      throw new RelatorioException("Template de relatório não disponível");
    }
  }

  /**
   * Processa template Thymeleaf com os dados fornecidos.
   *
   * @param templateName Nome do template (já validado contra whitelist)
   * @param dados Variáveis para o contexto
   * @return HTML processado como String
   */
  private String processarTemplate(String templateName, Map<String, Object> dados) {
    Context context = new Context();
    context.setVariables(dados);

    // Template deve estar em resources/templates/reports/{templateName}.html
    String templatePath = "reports/" + templateName;
    return templateEngine.process(templatePath, context);
  }

  /**
   * Converte HTML para PDF usando Flying Saucer (ITextRenderer).
   *
   * @param html Conteúdo HTML válido
   * @return Array de bytes do PDF
   * @throws IOException Se ocorrer erro na geração
   */
  private byte[] converterHtmlParaPdf(String html) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();

      // Configurar documento HTML
      renderer.setDocumentFromString(html);
      renderer.layout();

      // Gerar PDF
      renderer.createPDF(outputStream);

      int tamanho = outputStream.size();
      log.debug("PDF gerado com sucesso, tamanho: {} bytes", tamanho);

      // Validar tamanho máximo (prevenção de DoS)
      if (tamanho > LIMITE_TAMANHO_PDF_BYTES) {
        log.warn(
            "PDF gerado excede limite de tamanho: {} bytes (limite: {} bytes)",
            tamanho,
            LIMITE_TAMANHO_PDF_BYTES);
        throw new RelatorioException(
            "Relatório excede o tamanho máximo permitido. Tente refinar os filtros.");
      }

      return outputStream.toByteArray();
    }
  }
}

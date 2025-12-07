package br.com.utfpr.gerenciamento.server.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção customizada para erros na geração de relatórios.
 *
 * <p>Usada para encapsular erros de template, conversão PDF e geração Excel sem expor detalhes
 * internos do sistema (prevenção de Information Disclosure - OWASP A04:2021).
 *
 * <p>Estende BaseApiException para compatibilidade com RFC 9457 (Problem Details for HTTP APIs).
 */
public class RelatorioException extends BaseApiException {

  private static final String TYPE_URI = "https://api.laboratorio.utfpr.edu.br/errors/relatorio";
  private static final String TITLE = "Erro na Geração de Relatório";

  public RelatorioException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, TITLE, message, TYPE_URI);
  }

  public RelatorioException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, TITLE, message, TYPE_URI, cause);
  }
}

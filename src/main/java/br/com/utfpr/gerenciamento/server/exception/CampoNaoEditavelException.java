package br.com.utfpr.gerenciamento.server.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando o usuário tenta alterar campos que não são editáveis.
 *
 * <p>Retorna HTTP 400 Bad Request com detalhes no formato RFC 9457.
 */
public class CampoNaoEditavelException extends BaseApiException {

  private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;
  private static final String TITLE = "Campo não editável";
  private static final String TYPE_URI = "/errors/campo-nao-editavel";

  /**
   * Cria uma nova exceção de campo não editável.
   *
   * @param message mensagem descritiva indicando quais campos não podem ser alterados
   */
  public CampoNaoEditavelException(String message) {
    super(STATUS, TITLE, message, TYPE_URI);
  }
}

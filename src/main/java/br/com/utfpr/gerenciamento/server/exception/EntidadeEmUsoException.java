package br.com.utfpr.gerenciamento.server.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando uma entidade não pode ser excluída por estar vinculada a outros registros.
 *
 * <p>Retorna HTTP 409 Conflict com detalhes no formato RFC 9457.
 */
public class EntidadeEmUsoException extends BaseApiException {

  private static final HttpStatus STATUS = HttpStatus.CONFLICT;
  private static final String TITLE = "Entidade em uso";
  private static final String TYPE_URI = "/errors/entidade-em-uso";

  /**
   * Cria uma nova exceção de entidade em uso.
   *
   * @param message mensagem descritiva do erro
   */
  public EntidadeEmUsoException(String message) {
    super(STATUS, TITLE, message, TYPE_URI);
  }

  /**
   * Cria uma nova exceção de entidade em uso com causa.
   *
   * @param message mensagem descritiva do erro
   * @param cause exceção original que causou o erro
   */
  public EntidadeEmUsoException(String message, Throwable cause) {
    super(STATUS, TITLE, message, TYPE_URI, cause);
  }
}

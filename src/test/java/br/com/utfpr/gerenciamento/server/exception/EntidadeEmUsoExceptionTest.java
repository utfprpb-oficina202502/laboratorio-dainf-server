package br.com.utfpr.gerenciamento.server.exception;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.util.TraceIdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Testes unitarios especificos para EntidadeEmUsoException.
 *
 * <p>Testes de estrutura RFC 9457 estao em BaseApiExceptionSubclassesTest.
 */
class EntidadeEmUsoExceptionTest {

  @AfterEach
  void cleanup() {
    TraceIdUtil.clear();
  }

  @Test
  void deveInstanciarComMensagemECausa() {
    // Given
    String mensagem = "Não é possível excluir este registro pois ele está vinculado a outros.";
    Throwable causa = new RuntimeException("FK constraint violation");

    // When
    EntidadeEmUsoException exception = new EntidadeEmUsoException(mensagem, causa);

    // Then
    assertNotNull(exception);
    assertEquals(mensagem, exception.getBody().getDetail());
    assertEquals(causa, exception.getCause());
    assertEquals("FK constraint violation", exception.getCause().getMessage());
  }
}

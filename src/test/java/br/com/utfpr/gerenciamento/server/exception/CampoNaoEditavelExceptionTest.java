package br.com.utfpr.gerenciamento.server.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class CampoNaoEditavelExceptionTest {

  @Test
  void deveCriarExcecaoComMensagemCorreta() {
    // Given
    String mensagem = "Os seguintes campos não podem ser alterados: email, permissões";

    // When
    CampoNaoEditavelException exception = new CampoNaoEditavelException(mensagem);

    // Then
    ProblemDetail problemDetail = exception.getBody();
    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    assertEquals("Campo não editável", problemDetail.getTitle());
    assertEquals(mensagem, problemDetail.getDetail());
    assertNotNull(problemDetail.getProperties());
    assertTrue(problemDetail.getProperties().containsKey("traceId"));
    assertTrue(problemDetail.getProperties().containsKey("timestamp"));
  }

  @Test
  void deveRetornarStatusBadRequest() {
    // Given
    CampoNaoEditavelException exception = new CampoNaoEditavelException("Mensagem de teste");

    // When/Then
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }
}

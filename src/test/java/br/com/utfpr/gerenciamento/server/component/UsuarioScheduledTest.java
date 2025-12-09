package br.com.utfpr.gerenciamento.server.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioScheduledTest {

  @Mock private UsuarioService usuarioService;

  @InjectMocks private UsuarioScheduled usuarioScheduled;

  @Test
  void deleteUnverifiedUsers_DeveChamarServicoQuandoExecutadoComSucesso() {
    // Given: Serviço não lança exceção
    doNothing().when(usuarioService).deleteUnverifiedUsers();

    // When
    usuarioScheduled.deleteUnverifiedUsers();

    // Then
    verify(usuarioService).deleteUnverifiedUsers();
  }

  @Test
  void deleteUnverifiedUsers_DeveLogarErroQuandoServicoFalha() {
    // Given: Serviço lança exceção
    RuntimeException exception = new RuntimeException("Erro no banco");
    doThrow(exception).when(usuarioService).deleteUnverifiedUsers();

    // When
    usuarioScheduled.deleteUnverifiedUsers();

    // Then: Metodo ainda é chamado, mas erro é logado (não relançado)
    verify(usuarioService).deleteUnverifiedUsers();
    // Nota: O log de erro seria verificado em um teste de integração,
    // mas aqui focamos no comportamento do metodo
  }

  @Test
  void deleteUnverifiedUsers_NaoDevePropararExcecaoParaScheduler() {
    // Given: Serviço lança exceção grave
    doThrow(new RuntimeException("Falha crítica no banco de dados"))
        .when(usuarioService)
        .deleteUnverifiedUsers();

    // When/Then: Exceção NÃO deve ser propagada (importante para jobs scheduled)
    assertDoesNotThrow(
        () -> usuarioScheduled.deleteUnverifiedUsers(),
        "Exceção não deve ser propagada para o scheduler - evita interromper outros jobs");

    verify(usuarioService).deleteUnverifiedUsers();
  }

  @Test
  void deleteUnverifiedUsers_DeveChamarServicoExatamenteUmaVez() {
    // Given
    doNothing().when(usuarioService).deleteUnverifiedUsers();

    // When
    usuarioScheduled.deleteUnverifiedUsers();

    // Then: Verifica que o serviço é chamado exatamente uma vez
    verify(usuarioService, times(1)).deleteUnverifiedUsers();
    verifyNoMoreInteractions(usuarioService);
  }
}

package br.com.utfpr.gerenciamento.server.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HistoricoTransicaoUtilTest {

  private static final String SEPARADOR_INICIO = "--- Histórico de Transição ---";
  private static final String SEPARADOR_FIM = "------------------------------";

  @Test
  void deveFormatarHistoricoComObservacaoOriginal() {
    // Given
    Long emprestimoId = 456L;
    String responsavelNome = "Maria Admin";
    LocalDate dataEmprestimo = LocalDate.of(2025, 1, 15);
    String usuarioEmprestimoNome = "João Silva";
    String observacaoOriginal = "Preciso devolver até sexta pois vou viajar";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId,
            responsavelNome,
            dataEmprestimo,
            usuarioEmprestimoNome,
            observacaoOriginal);

    // Then
    assertThat(resultado)
        .contains(SEPARADOR_INICIO)
        .contains("[EMPRÉSTIMO #456]")
        .contains("Autorizado por Maria Admin")
        .contains("15/01/2025")
        .contains("para João Silva")
        .contains(SEPARADOR_FIM)
        .contains("Preciso devolver até sexta pois vou viajar")
        .startsWith(SEPARADOR_INICIO)
        .endsWith("Preciso devolver até sexta pois vou viajar");
  }

  @Test
  void deveFormatarHistoricoSemObservacao() {
    // Given
    Long emprestimoId = 789L;
    String responsavelNome = "Admin Sistema";
    LocalDate dataEmprestimo = LocalDate.of(2025, 6, 20);
    String usuarioEmprestimoNome = "Pedro Costa";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId, responsavelNome, dataEmprestimo, usuarioEmprestimoNome, null);

    // Then
    assertThat(resultado)
        .contains(SEPARADOR_INICIO)
        .contains("[EMPRÉSTIMO #789]")
        .contains("Autorizado por Admin Sistema")
        .contains("20/06/2025")
        .contains("para Pedro Costa")
        .contains(SEPARADOR_FIM)
        .endsWith(SEPARADOR_FIM);
  }

  @Test
  void deveOmitirObservacaoVazia() {
    // Given
    Long emprestimoId = 123L;
    String responsavelNome = "Responsável";
    LocalDate dataEmprestimo = LocalDate.of(2025, 3, 10);
    String usuarioEmprestimoNome = "Usuário";
    String observacaoVazia = "   ";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId, responsavelNome, dataEmprestimo, usuarioEmprestimoNome, observacaoVazia);

    // Then
    assertThat(resultado).endsWith(SEPARADOR_FIM).doesNotContain("   \n");
  }

  @Test
  void devePreservarHistoricoExistenteDaReserva() {
    // Given
    Long emprestimoId = 456L;
    String responsavelNome = "Maria Admin";
    LocalDate dataEmprestimo = LocalDate.of(2025, 1, 15);
    String usuarioEmprestimoNome = "João Silva";
    String observacaoComHistorico =
        """
        --- Histórico de Transição ---
        [RESERVA #123] Criado por João Silva em 12/01/2025
        ------------------------------
        Observação original do usuário""";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId,
            responsavelNome,
            dataEmprestimo,
            usuarioEmprestimoNome,
            observacaoComHistorico);

    // Then
    assertThat(resultado)
        .contains("[EMPRÉSTIMO #456]")
        .contains("[RESERVA #123]")
        .contains("Observação original do usuário");

    int contadorSeparadorInicio = contarOcorrencias(resultado, SEPARADOR_INICIO);
    assertThat(contadorSeparadorInicio).isEqualTo(2);
  }

  @Test
  void deveFormatarDataCorretamenteNoPadraoBrasileiro() {
    // Given
    Long emprestimoId = 1L;
    String responsavelNome = "Admin";
    LocalDate dataEmprestimo = LocalDate.of(2025, 12, 7);
    String usuarioEmprestimoNome = "Usuário";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId, responsavelNome, dataEmprestimo, usuarioEmprestimoNome, null);

    // Then
    assertThat(resultado)
        .contains("07/12/2025")
        .doesNotContain("2025-12-07")
        .doesNotContain("12/07/2025");
  }

  @Test
  void deveTratarDataNula() {
    // Given
    Long emprestimoId = 999L;
    String responsavelNome = "Admin";
    LocalDate dataEmprestimo = null;
    String usuarioEmprestimoNome = "Usuário";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId, responsavelNome, dataEmprestimo, usuarioEmprestimoNome, null);

    // Then
    assertThat(resultado).contains("[EMPRÉSTIMO #999]").contains("Autorizado por Admin em ");
  }

  @Test
  void deveFormatarHistoricoCompletoCorretamente() {
    // Given
    Long emprestimoId = 456L;
    String responsavelNome = "Maria Admin";
    LocalDate dataEmprestimo = LocalDate.of(2025, 1, 15);
    String usuarioEmprestimoNome = "João Silva";
    String observacao = "Observação teste";

    // When
    String resultado =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimoId, responsavelNome, dataEmprestimo, usuarioEmprestimoNome, observacao);

    // Then
    String esperado =
        """
        --- Histórico de Transição ---
        [EMPRÉSTIMO #456] Autorizado por Maria Admin em 15/01/2025 para João Silva
        ------------------------------
        Observação teste""";

    assertThat(resultado).isEqualTo(esperado);
  }

  @Test
  void construtorPrivadoDeveLancarExcecao() throws NoSuchMethodException {
    // Given
    Constructor<HistoricoTransicaoUtil> constructor =
        HistoricoTransicaoUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    // When/Then
    assertThatThrownBy(constructor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(UnsupportedOperationException.class);
  }

  private int contarOcorrencias(String texto, String substring) {
    int count = 0;
    int idx = 0;
    while ((idx = texto.indexOf(substring, idx)) != -1) {
      count++;
      idx += substring.length();
    }
    return count;
  }
}

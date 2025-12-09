package br.com.utfpr.gerenciamento.server.util;

import java.time.LocalDate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Utilitários para formatação de histórico de transições entre entidades.
 *
 * <p>Preserva rastreabilidade de transições (ex: Empréstimo → Saída) através de texto formatado no
 * campo observação, garantindo histórico mesmo em caso de migração de banco ou exclusão de
 * registros originais.
 *
 * @author Rodrigo Izidoro
 * @since 2025-12-09
 */
public final class HistoricoTransicaoUtil {

  private static final String SEPARADOR_INICIO = "--- Histórico de Transição ---";
  private static final String SEPARADOR_FIM = "------------------------------";

  private HistoricoTransicaoUtil() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Formata histórico de transição Empréstimo → Saída.
   *
   * <p>Gera texto formatado contendo informações do empréstimo de origem, preservando a observação
   * original (incluindo possível histórico de Reserva já existente).
   *
   * <p>Formato de saída:
   *
   * <pre>
   * --- Histórico de Transição ---
   * [EMPRÉSTIMO #ID] Autorizado por RESPONSAVEL em DATA para USUARIO
   * ------------------------------
   * Observação original (se existir)
   * </pre>
   *
   * @param emprestimoId ID do empréstimo de origem
   * @param responsavelNome Nome do usuário responsável pela autorização
   * @param dataEmprestimo Data do empréstimo
   * @param usuarioEmprestimoNome Nome do usuário que recebeu o empréstimo
   * @param observacaoOriginal Observação original do empréstimo (pode ser null ou vazia)
   * @return Texto formatado com histórico de transição
   */
  @NonNull public static String formatarHistoricoEmprestimoParaSaida(
      @NonNull Long emprestimoId,
      @NonNull String responsavelNome,
      @Nullable LocalDate dataEmprestimo,
      @NonNull String usuarioEmprestimoNome,
      @Nullable String observacaoOriginal) {

    StringBuilder sb = new StringBuilder();

    sb.append(SEPARADOR_INICIO).append("\n");
    sb.append(
        String.format(
            "[EMPRÉSTIMO #%d] Autorizado por %s em %s para %s",
            emprestimoId, responsavelNome, formatarData(dataEmprestimo), usuarioEmprestimoNome));
    sb.append("\n");
    sb.append(SEPARADOR_FIM);

    if (observacaoOriginal != null && !observacaoOriginal.trim().isEmpty()) {
      sb.append("\n").append(observacaoOriginal);
    }

    return sb.toString();
  }

  private static String formatarData(LocalDate data) {
    return data != null ? data.format(DateUtil.BR_DATE_FORMATTER) : "";
  }
}

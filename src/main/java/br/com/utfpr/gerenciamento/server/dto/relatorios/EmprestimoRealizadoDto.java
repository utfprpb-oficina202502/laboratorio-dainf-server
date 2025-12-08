package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de empréstimos realizados em um período.
 *
 * <p>Preserva a lógica do relatório Jasper EmprestimosRealizadosEntre.jrxml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoRealizadoDto {

  private Long cod;

  /** Nome do aluno/professor que fez o empréstimo */
  private String usuarioEmprestimo;

  /** Nome do usuário responsável pelo empréstimo */
  private String usuarioResponsavel;

  private LocalDate dataEmprestimo;

  /** Situação calculada: "Em atraso", "Em andamento" ou "Finalizado" */
  private String situacao;
}

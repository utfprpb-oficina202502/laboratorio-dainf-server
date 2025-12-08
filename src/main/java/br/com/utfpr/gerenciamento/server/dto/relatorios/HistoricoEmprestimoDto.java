package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de histórico de empréstimos de um usuário.
 *
 * <p>Preserva a lógica do relatório Jasper HistoricoEmprestimoUsuario.jrxml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoEmprestimoDto {

  private Long cod;

  private String nomeUsuario;

  private LocalDate dataEmprestimo;

  private LocalDate prazoDevolucao;

  private LocalDate dataDevolucao;

  /** Situação calculada: "Em atraso", "Em andamento" ou "Finalizado" */
  private String situacao;
}

package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de itens que atingiram a quantidade mínima.
 *
 * <p>Preserva a lógica do relatório Jasper ItensAtingiramQtdeMinima.jrxml (WHERE saldo <=
 * qtde_minima)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemQtdeMinimaDto {

  private Long cod;

  private String nome;

  private BigDecimal qtdeMinima;

  private BigDecimal saldo;

  private String grupo;
}

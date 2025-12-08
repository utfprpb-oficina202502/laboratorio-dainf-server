package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de itens sem estoque (saldo = 0).
 *
 * <p>Preserva a lógica do relatório Jasper ItensSemEstoque.jrxml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSemEstoqueDto {

  private Long cod;

  private String nome;

  private BigInteger patrimonio;

  private BigInteger siorg;

  private BigDecimal qtdeMinima;

  private String grupo;
}

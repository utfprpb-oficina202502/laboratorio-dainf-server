package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de solicitações de compra de um item.
 *
 * <p>Preserva a lógica do relatório Jasper SolicitacaoItem.jrxml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoItemDto {

  private Long cod;

  private String descricao;

  private LocalDate dataSolicitacao;

  private BigDecimal qtde;

  private String usuarioSolicitacao;

  private String nomeItem;
}

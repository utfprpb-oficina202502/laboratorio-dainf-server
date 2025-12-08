package br.com.utfpr.gerenciamento.server.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class EmprestimoItemResponseDto {
  private Long id;

  private BigDecimal qtde;

  private ItemResponseDto item;

  // Flag enviada pela UI indicando se o item deve ser devolvido
  private Boolean devolver;
}

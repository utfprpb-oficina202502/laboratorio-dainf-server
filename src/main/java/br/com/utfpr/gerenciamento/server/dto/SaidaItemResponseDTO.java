package br.com.utfpr.gerenciamento.server.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaidaItemResponseDTO {
  private Long id;

  private BigDecimal qtde;

  private ItemResponseDto item;

  private Long saidaId;
}

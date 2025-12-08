package br.com.utfpr.gerenciamento.server.dto.relatorios;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para relatório de reservas de um item específico.
 *
 * <p>Preserva a lógica do relatório Jasper ReservaDoItem.jrxml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaItemDto {

  private Long cod;

  private LocalDate dataReserva;

  private LocalDate dataRetirada;

  private BigDecimal qtde;

  private String usuarioReserva;

  private String nomeItem;
}

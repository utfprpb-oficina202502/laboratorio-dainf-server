package br.com.utfpr.gerenciamento.server.dto;

import br.com.utfpr.gerenciamento.server.repository.projection.ItemSimpleProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado para listagem de itens com apenas id e nome.
 *
 * <p>Usado em contextos onde apenas a identificação básica do item é necessária, como a listagem de
 * itens vinculados a um grupo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSimpleDto {

  private Long id;
  private String nome;

  /**
   * Converte uma projeção JPA para DTO.
   *
   * @param projection projeção retornada pelo repositório
   * @return DTO com os dados mapeados
   */
  public static ItemSimpleDto fromProjection(ItemSimpleProjection projection) {
    if (projection == null) {
      return null;
    }
    return ItemSimpleDto.builder().id(projection.getId()).nome(projection.getNome()).build();
  }
}

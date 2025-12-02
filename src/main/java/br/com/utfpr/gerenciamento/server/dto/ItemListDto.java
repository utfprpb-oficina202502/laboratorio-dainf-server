package br.com.utfpr.gerenciamento.server.dto;

import br.com.utfpr.gerenciamento.server.repository.projection.ItemListProjection;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado para listagem paginada de Itens.
 *
 * <p>Contém apenas os campos necessários para exibição em tabelas no frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemListDto implements BaseListDto {

  @SortableField private Long id;
  @SortableField private String nome;
  @SortableField private String localizacao;
  @SortableField private BigDecimal saldo;

  @SortableField(entityPath = "grupo.descricao")
  private GrupoListDto grupo;

  private String imagemUrl;

  /** DTO aninhado para grupo. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class GrupoListDto {
    private Long id;
    private String descricao;
  }

  /**
   * Converte uma projection JPA para DTO.
   *
   * @param projection projeção retornada pelo repositório
   * @return DTO com os dados mapeados
   */
  public static ItemListDto fromProjection(ItemListProjection projection) {
    if (projection == null) {
      return null;
    }

    GrupoListDto grupoDto = null;
    if (projection.getGrupoId() != null) {
      grupoDto =
          GrupoListDto.builder()
              .id(projection.getGrupoId())
              .descricao(projection.getGrupoDescricao())
              .build();
    }

    return ItemListDto.builder()
        .id(projection.getId())
        .nome(projection.getNome())
        .localizacao(projection.getLocalizacao())
        .saldo(projection.getSaldo())
        .grupo(grupoDto)
        .imagemUrl(projection.getImagemUrl())
        .build();
  }
}

package br.com.utfpr.gerenciamento.server.dto;

import lombok.Data;

@Data
public class GrupoResponseDto implements BaseListDto {

  @SortableField private Long id;

  @SortableField private String descricao;
}

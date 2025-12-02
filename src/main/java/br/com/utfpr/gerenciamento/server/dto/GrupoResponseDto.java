package br.com.utfpr.gerenciamento.server.dto;

import lombok.Data;

@Data
public class GrupoResponseDto implements BaseListDto {

  private Long id;

  private String descricao;
}

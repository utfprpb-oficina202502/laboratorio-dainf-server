package br.com.utfpr.gerenciamento.server.dto;

import lombok.Data;

@Data
public class PaisResponseDto implements BaseListDto {

  private Long id;

  private String nome;

  private String sigla;
}

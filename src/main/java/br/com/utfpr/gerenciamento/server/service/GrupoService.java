package br.com.utfpr.gerenciamento.server.service;

import br.com.utfpr.gerenciamento.server.dto.GrupoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Grupo;

public interface GrupoService extends CrudService<Grupo, Long, GrupoResponseDto> {
  // Metodo complete() herdado de CrudService com paginacao
}

package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.GrupoResponseDto;
import br.com.utfpr.gerenciamento.server.dto.ItemSimpleDto;
import br.com.utfpr.gerenciamento.server.model.Grupo;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.GrupoService;
import br.com.utfpr.gerenciamento.server.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("grupo")
public class GrupoController extends CrudController<Grupo, Long, GrupoResponseDto> {

  private final GrupoService grupoService;
  private final ItemService itemService;

  public GrupoController(GrupoService grupoService, ItemService itemService) {
    this.grupoService = grupoService;
    this.itemService = itemService;
  }

  @Override
  protected CrudService<Grupo, Long, GrupoResponseDto> getService() {
    return grupoService;
  }

  // Endpoint /complete herdado de CrudController com paginacao

  /**
   * Busca paginada simplificada de itens vinculados a um grupo.
   *
   * <p>Retorna apenas id e nome dos itens para otimizar a listagem no diálogo de itens vinculados.
   *
   * @param idGrupo ID do grupo
   * @param page Número da página (0-indexed)
   * @param size Tamanho da página
   * @param filter Filtro opcional por id ou nome
   * @return Página de ItemSimpleDto
   */
  @GetMapping("/itens-vinculados/{idGrupo}")
  public Page<ItemSimpleDto> findItensVinculado(
      @PathVariable("idGrupo") Long idGrupo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(required = false) String filter) {
    PageRequest pageRequest = PageRequest.of(page, size);
    return itemService.findByGrupoPagedSimple(idGrupo, filter, pageRequest);
  }
}

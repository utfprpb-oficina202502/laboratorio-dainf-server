package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.GrupoResponseDto;
import br.com.utfpr.gerenciamento.server.dto.ItemResponseDto;
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

  @GetMapping("/itens-vinculados/{idGrupo}")
  public Page<ItemResponseDto> findItensVinculado(
      @PathVariable("idGrupo") Long idGrupo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(required = false) String filter) {
    PageRequest pageRequest = PageRequest.of(page, size);
    return itemService.findByGrupoPaged(idGrupo, filter, pageRequest);
  }
}

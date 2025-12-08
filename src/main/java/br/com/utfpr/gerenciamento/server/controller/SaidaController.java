package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.BaseListDto;
import br.com.utfpr.gerenciamento.server.dto.SaidaListDto;
import br.com.utfpr.gerenciamento.server.dto.SaidaResponseDTO;
import br.com.utfpr.gerenciamento.server.model.Saida;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.ItemService;
import br.com.utfpr.gerenciamento.server.service.SaidaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("saida")
public class SaidaController extends CrudController<Saida, Long, SaidaResponseDTO> {

  private final SaidaService saidaService;
  private final ItemService itemService;

  public SaidaController(SaidaService saidaService, ItemService itemService) {
    this.saidaService = saidaService;
    this.itemService = itemService;
  }

  @Override
  protected CrudService<Saida, Long, SaidaResponseDTO> getService() {
    return saidaService;
  }

  @Override
  protected Class<? extends BaseListDto> getListDtoClass() {
    return SaidaListDto.class;
  }

  /**
   * Lista paginada de saídas com filtro textual usando DTO simplificado.
   *
   * <p><b>Otimização:</b> Retorna apenas campos necessários para listagem via projeção SQL.
   *
   * @param page Número da página (0-indexed)
   * @param size Tamanho da página
   * @param filter Filtro opcional (busca textual em todos os campos)
   * @param sort Ordenacao no formato "campo,direcao" (ex: "dataSaida,desc")
   * @return Página de saídas simplificadas
   */
  @Override
  @GetMapping("page")
  public Page<? extends BaseListDto> findAllPaged(
      @RequestParam("page") int page,
      @RequestParam("size") int size,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) Long grupoId) {
    Sort sortObj = parseSortParameter(sort);
    PageRequest pageRequest = PageRequest.of(page, size, sortObj);
    return saidaService.findAllPagedList(filter, pageRequest);
  }

  @Override
  public void preSave(Saida object) {
    // se está editando, ele retorna o saldo de todos os itens, para depois baixar novamente com os
    // valores atualizados
    if (object.getId() != null) {
      Saida old = saidaService.toEntity(saidaService.findOne(object.getId()));
      old.getSaidaItem().stream()
          .forEach(
              saidaItem ->
                  itemService.aumentaSaldoItem(saidaItem.getItem().getId(), saidaItem.getQtde()));
    }
    object.getSaidaItem().stream()
        .forEach(
            saidaItem -> {
              if (saidaItem.getItem() != null) {
                itemService.saldoItemIsValid(
                    itemService.getSaldoItem(saidaItem.getItem().getId()), saidaItem.getQtde());
              }
            });
  }

  @Override
  public void postSave(Saida object) {
    object.getSaidaItem().stream()
        .forEach(
            saidaItem ->
                itemService.diminuiSaldoItem(
                    saidaItem.getItem().getId(), saidaItem.getQtde(), true));
  }

  @Override
  public void postDelete(Saida object) {
    object.getSaidaItem().stream()
        .forEach(
            saidaItem ->
                itemService.aumentaSaldoItem(saidaItem.getItem().getId(), saidaItem.getQtde()));
  }
}

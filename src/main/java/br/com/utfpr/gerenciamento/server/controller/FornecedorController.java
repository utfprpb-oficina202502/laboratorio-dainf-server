package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.BaseListDto;
import br.com.utfpr.gerenciamento.server.dto.FornecedorListDto;
import br.com.utfpr.gerenciamento.server.dto.FornecedorResponseDto;
import br.com.utfpr.gerenciamento.server.model.Fornecedor;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.FornecedorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("fornecedor")
public class FornecedorController extends CrudController<Fornecedor, Long, FornecedorResponseDto> {

  private final FornecedorService fornecedorService;

  public FornecedorController(FornecedorService fornecedorService) {
    this.fornecedorService = fornecedorService;
  }

  @Override
  protected CrudService<Fornecedor, Long, FornecedorResponseDto> getService() {
    return fornecedorService;
  }

  @Override
  protected Class<? extends BaseListDto> getListDtoClass() {
    return FornecedorListDto.class;
  }

  /**
   * Lista paginada de fornecedores com filtro textual usando DTO simplificado.
   *
   * <p><b>Otimização:</b> Retorna apenas campos necessários para listagem via projeção SQL.
   *
   * @param page Número da página (0-indexed)
   * @param size Tamanho da página
   * @param filter Filtro opcional (busca textual em todos os campos)
   * @param sort Ordenacao no formato "campo,direcao" (ex: "razaoSocial,desc")
   * @return Página de fornecedores simplificados
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
    return fornecedorService.findAllPagedList(filter, pageRequest);
  }
}

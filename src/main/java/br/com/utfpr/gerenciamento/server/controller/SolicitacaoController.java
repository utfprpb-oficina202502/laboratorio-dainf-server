package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.BaseListDto;
import br.com.utfpr.gerenciamento.server.dto.SolicitacaoListDto;
import br.com.utfpr.gerenciamento.server.dto.SolicitacaoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Solicitacao;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.SolicitacaoService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("solicitacao-compra")
public class SolicitacaoController
    extends CrudController<Solicitacao, Long, SolicitacaoResponseDto> {

  private final SolicitacaoService solicitacaoService;

  public SolicitacaoController(SolicitacaoService solicitacaoService) {
    this.solicitacaoService = solicitacaoService;
  }

  @Override
  protected CrudService<Solicitacao, Long, SolicitacaoResponseDto> getService() {
    return solicitacaoService;
  }

  @Override
  protected Class<? extends BaseListDto> getListDtoClass() {
    return SolicitacaoListDto.class;
  }

  /**
   * Lista paginada de solicitações com filtro textual usando DTO simplificado.
   *
   * <p><b>Otimização:</b> Retorna apenas campos necessários para listagem via projeção SQL.
   *
   * @param page Número da página (0-indexed)
   * @param size Tamanho da página
   * @param filter Filtro opcional (busca textual em todos os campos)
   * @param sort Ordenacao no formato "campo,direcao" (ex: "dataSolicitacao,desc")
   * @return Página de solicitações simplificadas
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
    return solicitacaoService.findAllPagedList(filter, pageRequest);
  }

  /**
   * Lista todas as solicitações de um usuário específico.
   *
   * <p>Acesso controlado no service com validação por ID: - Usuário só pode ver suas próprias
   * solicitações - Admin/Laboratorista podem ver todas as solicitações
   *
   * @param username Username do usuário a consultar
   * @return Lista de solicitações do usuário especificado
   */
  @GetMapping("find-all-by-username/{username}")
  public List<SolicitacaoResponseDto> findAllByUsername(@PathVariable("username") String username) {
    return solicitacaoService.findAllByUsername(username);
  }
}

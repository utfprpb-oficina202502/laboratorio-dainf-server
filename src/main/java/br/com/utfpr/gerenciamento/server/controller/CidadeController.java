package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.CidadeResponseDto;
import br.com.utfpr.gerenciamento.server.model.Cidade;
import br.com.utfpr.gerenciamento.server.model.Estado;
import br.com.utfpr.gerenciamento.server.service.CidadeService;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("cidade")
public class CidadeController extends CrudController<Cidade, Long, CidadeResponseDto> {

  private final CidadeService cidadeService;

  public CidadeController(CidadeService cidadeService) {
    this.cidadeService = cidadeService;
  }

  @Override
  protected CrudService<Cidade, Long, CidadeResponseDto> getService() {
    return cidadeService;
  }

  /**
   * Busca cidades filtradas por estado especifico. Mantido separado pois requer filtro por
   * relacionamento.
   */
  @PostMapping("/complete-by-estado")
  public java.util.List<CidadeResponseDto> completeByEstado(
      @RequestParam("query") String query, @RequestBody Estado estado) {
    return cidadeService.completeByEstado(query, estado);
  }
}

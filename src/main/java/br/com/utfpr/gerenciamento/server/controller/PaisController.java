package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.PaisResponseDto;
import br.com.utfpr.gerenciamento.server.model.Pais;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.PaisService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pais")
public class PaisController extends CrudController<Pais, Long, PaisResponseDto> {

  private final PaisService paisService;

  public PaisController(PaisService paisService) {
    this.paisService = paisService;
  }

  @Override
  protected CrudService<Pais, Long, PaisResponseDto> getService() {
    return paisService;
  }
}

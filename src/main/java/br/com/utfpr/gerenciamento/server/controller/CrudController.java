package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.service.CrudService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
public abstract class CrudController<T, ID extends Serializable, DTO> {

  protected abstract CrudService<T, ID, DTO> getService();

  @GetMapping
  public List<DTO> findAll() {
    return getService().findAll(Sort.by("id"));
  }

  @PostMapping
  public DTO save(@RequestBody @Valid T object) {
    preSave(object);
    DTO toReturn = getService().save(object);
    postSave(object);
    return toReturn;
  }

  public void preSave(T object) {}

  public void postSave(T object) {}

  @GetMapping("{id}")
  public DTO findone(@PathVariable("id") ID id) {
    return getService().findOne(id);
  }

  @DeleteMapping("{id}")
  public void delete(@PathVariable("id") ID id) {
    T object = getService().toEntity(getService().findOne(id));
    getService().delete(id);
    postDelete(object);
  }

  public void postDelete(T object) {}

  @GetMapping("exists/{id}")
  public boolean exists(@PathVariable ID id) {
    return getService().exists(id);
  }

  @GetMapping("count")
  public long count() {
    return getService().count();
  }

  @GetMapping("page")
  public Page<DTO> findAllPaged(
      @RequestParam("page") @Min(0) int page,
      @RequestParam("size") @Min(1) @Max(100) int size,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String order,
      @RequestParam(required = false) Boolean asc) {
    PageRequest pageRequest = PageRequest.of(page, size);
    if (order != null && asc != null) {
      pageRequest =
          PageRequest.of(page, size, asc ? Sort.Direction.ASC : Sort.Direction.DESC, order);
    }
    if (filter != null && !filter.isEmpty()) {
      Specification<T> spec = getService().filterByAllFields(filter);
      return getService().findAllSpecification(spec, pageRequest);
    } else return getService().findAll(pageRequest);
  }

  /**
   * Endpoint paginado para autocomplete.
   *
   * <p>Retorna Page de DTOs com busca textual em todos os campos String/Number. Para filtros
   * customizados, sobrescreva este metodo no controller especifico.
   *
   * @param query Texto para filtro (case insensitive, busca parcial)
   * @param page Numero da pagina (0-indexed, minimo: 0)
   * @param size Tamanho da pagina (default: 10, minimo: 1, maximo: 100)
   * @return Pagina de DTOs filtrados
   */
  @GetMapping("complete")
  public Page<DTO> complete(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    return getService().complete(query, pageRequest);
  }
}

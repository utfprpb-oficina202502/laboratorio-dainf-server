package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.service.CrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CrudController<T, ID extends Serializable, DTO> {

  protected abstract CrudService<T, ID,DTO> getService();

  @GetMapping
  public List<DTO> findAll() {
    return getService()
            .findAll(Sort.by("id"));
  }

  @PostMapping
  public DTO save(@RequestBody T entity) {
    return getService().save(entity);
  }

  public void preSave(T object) {}
  public void postSave(T object) {}

  @GetMapping("{id}")
  public DTO findOne(@PathVariable("id") ID id) {
    DTO dto = getService().findOne(id);
    return dto != null ? dto : null;
  }

  @DeleteMapping("{id}")
  public void delete(@PathVariable("id") ID id) {
    getService().delete(id);
  }

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
          @RequestParam("page") int page,
          @RequestParam("size") int size,
          @RequestParam(required = false) String filter,
          @RequestParam(required = false) String order,
          @RequestParam(required = false) Boolean asc) {

    PageRequest pageRequest = PageRequest.of(page, size);
    if (order != null && asc != null) {
      pageRequest = PageRequest.of(page, size, asc ? Sort.Direction.ASC : Sort.Direction.DESC, order);
    }

    Page<DTO> pageResult;
    if (filter != null && !filter.isEmpty()) {
      Specification<T> spec = getService().filterByAllFields(filter);
      pageResult = getService().findAllSpecification(spec, pageRequest);
    } else {
      pageResult = getService().findAll(pageRequest);
    }

    return pageResult;
  }
}

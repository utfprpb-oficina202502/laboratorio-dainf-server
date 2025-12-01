package br.com.utfpr.gerenciamento.server.service;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public interface CrudService<T, ID extends Serializable, DTO> {

  List<DTO> findAll();

  List<DTO> findAll(Sort sort);

  Page<DTO> findAll(Pageable pageable);

  Page<DTO> findAllSpecification(Specification<T> specification, Pageable pageable);

  DTO save(T entity);

  DTO saveAndFlush(T entity);

  Iterable<DTO> save(Iterable<T> iterable);

  void flush();

  DTO findOne(ID id);

  List<DTO> findAllById(Iterable<ID> ids);

  boolean exists(ID id);

  long count();

  void delete(ID id);

  void delete(T entity);

  void delete(Iterable<T> iterable);

  Specification<T> filterByAllFields(String filter);

  /**
   * Busca paginada para autocomplete com filtro textual.
   *
   * <p>Implementacao padrao utiliza filterByAllFields() para busca generica. Services especificos
   * podem sobrescrever para filtros customizados.
   *
   * @param query Texto para filtro (busca em todos os campos String/Number)
   * @param pageable Configuracao de paginacao
   * @return Pagina de DTOs com resultados filtrados
   */
  Page<DTO> complete(String query, Pageable pageable);

  void deleteAll();

  DTO toDto(T entity);

  T toEntity(DTO dto);
}

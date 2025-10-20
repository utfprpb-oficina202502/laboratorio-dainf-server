package br.com.utfpr.gerenciamento.server.service;

import java.io.Serializable;
import java.util.List;

import br.com.utfpr.gerenciamento.server.dto.EmprestimoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public interface CrudService<T, ID extends Serializable,DTO> {

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
  public abstract DTO convertToDTO(T entity);
  public abstract T convertToEntity(DTO entity);
  List<T> findAllByIdEntity(Iterable<ID> ids);

  boolean exists(ID id);

  long count();

  void delete(ID id);

  void delete(T entity);

  void delete(Iterable<T> iterable);

  Specification<T> filterByAllFields(String filter);

  void deleteAll();
}

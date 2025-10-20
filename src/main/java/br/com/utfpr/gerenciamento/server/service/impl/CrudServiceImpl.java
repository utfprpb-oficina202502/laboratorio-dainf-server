package br.com.utfpr.gerenciamento.server.service.impl;

import br.com.utfpr.gerenciamento.server.service.CrudService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CrudServiceImpl<T, ID extends Serializable, DTO> implements CrudService<T, ID, DTO> {

  protected abstract JpaRepository<T, ID> getRepository();

  // Métodos padrão (entidades puras)
  @Override
  @Transactional(readOnly = true)
  public List<DTO> findAll() {
    return getRepository().findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DTO> findAll(Sort sort) {
    return getRepository().findAll(sort).stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DTO> findAll(Pageable pageable) {
    return getRepository().findAll(pageable).map(this::convertToDTO);
  }

  @Override
  @Transactional
  public DTO save(T entity) {
    return convertToDTO(getRepository().save(entity));
  }

  @Override
  @Transactional
  public DTO saveAndFlush(T entity) {
    return convertToDTO(getRepository().saveAndFlush(entity));
  }

  @Override
  public Iterable<DTO> save(Iterable<T> iterable) {
    return null;
  }

  @Override
  public void flush() {

  }

  @Override
  @Transactional(readOnly = true)
  public DTO findOne(ID id) {
    return convertToDTO(getRepository().getReferenceById(id));
  }

  @Override
  public List<DTO> findAllById(Iterable<ID> ids) {
    return List.of();
  }

  @Override
  @Transactional(readOnly = true)
  public List<T> findAllByIdEntity(Iterable<ID> ids) {
    return getRepository().findAllById(ids);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(ID id) {
    return getRepository().existsById(id);
  }

  @Override
  public long count() {
    return 0;
  }

  @Override
  @Transactional
  public void delete(ID id) {
    getRepository().deleteById(id);
  }

  @Override
  public void delete(T entity) {

  }

  @Override
  public void delete(Iterable<T> iterable) {

  }

  @Override
  @Transactional
  public Specification<T> filterByAllFields(String filter) {
    return (root, query, cb) -> {
      if (filter == null || filter.trim().isEmpty()) {
        return cb.conjunction();
      }

      String likeFilter = "%" + filter.toLowerCase() + "%";

      Predicate[] predicates = root.getModel().getDeclaredSingularAttributes().stream()
              .filter(attr -> {
                Class<?> javaType = attr.getJavaType();
                return javaType.equals(String.class) || Number.class.isAssignableFrom(javaType);
              })
              .map(attr -> {
                if (attr.getJavaType().equals(String.class)) {
                  return cb.like(cb.lower(root.get(attr.getName())), likeFilter);
                } else {
                  return cb.like(cb.toString(root.get(attr.getName())), likeFilter);
                }
              })
              .toArray(Predicate[]::new);

      return cb.or(predicates);
    };
  }

  @Override
  public void deleteAll() {

  }

  @Override
  @Transactional
  public Page<DTO> findAllSpecification(Specification<T> specification, Pageable pageable) {
    return ((org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) getRepository())
            .findAll(specification, pageable).map(this::convertToDTO);
  }
}

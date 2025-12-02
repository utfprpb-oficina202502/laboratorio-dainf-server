package br.com.utfpr.gerenciamento.server.service.impl;

import br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public abstract class CrudServiceImpl<T, ID extends Serializable, DTO>
    implements CrudService<T, ID, DTO> {

  /** Profundidade maxima de paths nested para evitar queries muito complexas. */
  private static final int MAX_PATH_DEPTH = 3;

  /** Formato de data brasileiro (dd/MM/yyyy). */
  private static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /** Caractere de escape para wildcards LIKE. */
  private static final char LIKE_ESCAPE_CHAR = '\\';

  protected abstract JpaRepository<T, ID> getRepository();

  /**
   * Retorna a referencia self para uso em subclasses que precisam de chamadas via proxy.
   *
   * <p>Nota: Metodos nesta classe base sao chamados externamente (via controller) e ja passam pelo
   * proxy Spring. Este metodo existe para subclasses que precisam chamar outros metodos do servico
   * internamente.
   */
  @SuppressWarnings("unchecked")
  protected CrudService<T, ID, DTO> self() {
    return this;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DTO> findAll() {
    return getRepository().findAll().stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DTO> findAll(Sort sort) {
    return getRepository().findAll(sort).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DTO> findAll(Pageable pageable) {
    Page<T> page = getRepository().findAll(pageable);
    List<DTO> dtoList = page.getContent().stream().map(this::toDto).toList();
    return new PageImpl<>(dtoList, pageable, page.getTotalElements());
  }

  @Override
  @Transactional
  public DTO save(T entity) {
    return toDto(getRepository().save(entity));
  }

  @Override
  @Transactional
  public DTO saveAndFlush(T entity) {
    return toDto(getRepository().saveAndFlush(entity));
  }

  @Override
  @Transactional
  public Iterable<DTO> save(Iterable<T> iterable) {
    List<T> entities = (List<T>) iterable;
    return getRepository().saveAll(entities).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional
  public void flush() {
    getRepository().flush();
  }

  @Override
  @Transactional(readOnly = true)
  public DTO findOne(ID id) {
    T entity =
        getRepository()
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entidade não encontrada."));
    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DTO> findAllById(Iterable<ID> ids) {
    return getRepository().findAllById(ids).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(ID id) {
    return getRepository().existsById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return getRepository().count();
  }

  @Override
  @Transactional
  public void delete(ID id) {

    getRepository().deleteById(id);
  }

  @Override
  @Transactional
  public void delete(T entity) {
    getRepository().delete(entity);
  }

  @Override
  @Transactional
  public void delete(Iterable<T> iterable) {
    getRepository().deleteAll(iterable);
  }

  @Override
  @Transactional
  public void deleteAll() {
    getRepository().deleteAll();
  }

  /**
   * Retorna mapeamento de campos pesquisaveis do ListDto para paths da entidade.
   *
   * <p>Sobrescreva em subclasses para definir campos especificos. Exemplo:
   *
   * <pre>
   * return Map.of(
   *   "usuarioNome", "usuario.nome",
   *   "id", "id"
   * );
   * </pre>
   *
   * @return Map de campo DTO para path da entidade, ou mapa vazio para usar fallback generico
   */
  protected Map<String, String> getSearchableFieldMappings() {
    return Collections.emptyMap();
  }

  @Override
  @Transactional(readOnly = true)
  public Specification<T> filterByAllFields(String filter) {
    return (root, query, cb) -> {
      if (filter == null || filter.trim().isEmpty()) {
        return cb.conjunction();
      }

      String trimmedFilter = filter.trim();
      String escapedFilter = escapeLikeWildcards(trimmedFilter);
      String likeFilter = "%" + escapedFilter.toLowerCase() + "%";
      List<Predicate> predicates = new ArrayList<>();
      Map<String, String> mappings = getSearchableFieldMappings();

      if (!mappings.isEmpty()) {
        addMappedFieldPredicates(root, cb, mappings, likeFilter, trimmedFilter, predicates);
      } else {
        addFallbackFieldPredicates(root, cb, likeFilter, predicates);
      }

      return predicates.isEmpty() ? cb.conjunction() : cb.or(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Adiciona predicados para campos mapeados especificos do ListDto.
   *
   * @param root Root da query
   * @param cb CriteriaBuilder
   * @param mappings Mapeamentos de campo DTO para path da entidade
   * @param likeFilter Filtro LIKE ja formatado
   * @param trimmedFilter Filtro original sem wildcards
   * @param predicates Lista para acumular predicados
   */
  private void addMappedFieldPredicates(
      jakarta.persistence.criteria.Root<T> root,
      jakarta.persistence.criteria.CriteriaBuilder cb,
      Map<String, String> mappings,
      String likeFilter,
      String trimmedFilter,
      List<Predicate> predicates) {
    for (String entityPath : mappings.values()) {
      Predicate predicate = createPredicateForPath(root, cb, entityPath, likeFilter, trimmedFilter);
      if (predicate != null) {
        predicates.add(predicate);
      }
    }
  }

  /**
   * Adiciona predicados para campos String/Number diretos da entidade (fallback).
   *
   * @param root Root da query
   * @param cb CriteriaBuilder
   * @param likeFilter Filtro LIKE ja formatado
   * @param predicates Lista para acumular predicados
   */
  private void addFallbackFieldPredicates(
      jakarta.persistence.criteria.Root<T> root,
      jakarta.persistence.criteria.CriteriaBuilder cb,
      String likeFilter,
      List<Predicate> predicates) {
    root.getModel().getDeclaredSingularAttributes().stream()
        .filter(attr -> isSearchableType(attr.getJavaType()))
        .forEach(attr -> predicates.add(createDirectFieldPredicate(root, cb, attr, likeFilter)));
  }

  /**
   * Verifica se o tipo e pesquisavel (String ou Number).
   *
   * @param javaType Tipo Java do atributo
   * @return true se o tipo pode ser pesquisado
   */
  private boolean isSearchableType(Class<?> javaType) {
    return javaType.equals(String.class) || Number.class.isAssignableFrom(javaType);
  }

  /**
   * Cria predicado para campo direto (nao nested) da entidade.
   *
   * @param root Root da query
   * @param cb CriteriaBuilder
   * @param attr Atributo da entidade
   * @param likeFilter Filtro LIKE ja formatado
   * @return Predicate para o campo
   */
  private Predicate createDirectFieldPredicate(
      jakarta.persistence.criteria.Root<T> root,
      jakarta.persistence.criteria.CriteriaBuilder cb,
      jakarta.persistence.metamodel.SingularAttribute<?, ?> attr,
      String likeFilter) {
    if (attr.getJavaType().equals(String.class)) {
      return cb.like(cb.lower(root.get(attr.getName())), likeFilter, LIKE_ESCAPE_CHAR);
    }
    return cb.like(cb.toString(root.get(attr.getName())), likeFilter, LIKE_ESCAPE_CHAR);
  }

  /**
   * Escapa caracteres especiais do LIKE (%, _) no filtro de usuario.
   *
   * <p>Previne que o usuario injete wildcards maliciosos que poderiam causar buscas muito amplas ou
   * lentidao no banco de dados.
   *
   * @param input Texto do filtro de usuario
   * @return Texto com wildcards escapados usando backslash
   */
  private String escapeLikeWildcards(String input) {
    if (input == null) {
      return null;
    }
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /**
   * Cria predicado para um path de entidade, suportando paths nested (ex: "usuario.nome").
   *
   * <p>Suporta tipos String, Number e LocalDate. Para LocalDate, tenta fazer parse do filtro no
   * formato brasileiro (dd/MM/yyyy) para busca exata.
   *
   * @param root Root da query
   * @param cb CriteriaBuilder
   * @param entityPath Path da entidade (ex: "usuario.nome" ou "id")
   * @param likeFilter Filtro LIKE ja formatado (ex: "%texto%")
   * @param originalFilter Filtro original sem formatacao (para parse de datas)
   * @return Predicate para o campo, ou null se o tipo nao for suportado
   */
  @SuppressWarnings("unchecked")
  private Predicate createPredicateForPath(
      jakarta.persistence.criteria.Root<T> root,
      jakarta.persistence.criteria.CriteriaBuilder cb,
      String entityPath,
      String likeFilter,
      String originalFilter) {
    try {
      String[] parts = entityPath.split("\\.");

      // Valida profundidade do path para evitar queries muito complexas
      if (parts.length > MAX_PATH_DEPTH) {
        log.warn(
            "Path '{}' excede profundidade maxima ({}), ignorando para evitar impacto em performance",
            entityPath,
            MAX_PATH_DEPTH);
        return null;
      }

      Path<?> currentPath = root;

      // Navega pelo path, criando JOINs para relacionamentos
      for (int i = 0; i < parts.length - 1; i++) {
        currentPath = ((From<?, ?>) currentPath).join(parts[i], JoinType.LEFT);
      }

      // Obtem o atributo final
      Path<?> attrPath = currentPath.get(parts[parts.length - 1]);
      Class<?> type = attrPath.getJavaType();

      if (String.class.equals(type)) {
        return cb.like(cb.lower((Path<String>) attrPath), likeFilter, LIKE_ESCAPE_CHAR);
      } else if (Number.class.isAssignableFrom(type)) {
        return cb.like(attrPath.as(String.class), likeFilter, LIKE_ESCAPE_CHAR);
      } else if (LocalDate.class.equals(type)) {
        // Tenta fazer parse da data no formato brasileiro
        return createLocalDatePredicate(cb, (Path<LocalDate>) attrPath, originalFilter);
      }
      return null;
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.debug("Path invalido '{}': {}", entityPath, e.getMessage());
      return null;
    } catch (ClassCastException e) {
      log.debug("Tipo de path incompativel '{}': {}", entityPath, e.getMessage());
      return null;
    }
  }

  /**
   * Cria predicado para busca em campos LocalDate.
   *
   * <p>Tenta fazer parse do filtro no formato brasileiro (dd/MM/yyyy). Se o parse falhar, retorna
   * null e o campo nao sera incluido na busca.
   *
   * @param cb CriteriaBuilder
   * @param path Path do campo LocalDate
   * @param filter Filtro original (ex: "07/10/2025")
   * @return Predicate para busca exata por data, ou null se o filtro nao for uma data valida
   */
  private Predicate createLocalDatePredicate(
      jakarta.persistence.criteria.CriteriaBuilder cb, Path<LocalDate> path, String filter) {
    try {
      LocalDate date = LocalDate.parse(filter.trim(), BR_DATE_FORMAT);
      return cb.equal(path, date);
    } catch (DateTimeParseException e) {
      // Filtro nao e uma data valida - nao inclui este campo na busca
      log.debug("Filtro '{}' nao e uma data valida no formato dd/MM/yyyy", filter);
      return null;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DTO> findAllSpecification(Specification<T> specification, Pageable pageable) {
    var repo =
        (org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) getRepository();
    Page<T> page = repo.findAll(specification, pageable);
    List<DTO> dtoList = page.getContent().stream().map(this::toDto).toList();
    return new PageImpl<>(dtoList, pageable, page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DTO> complete(String query, Pageable pageable) {
    if (query == null || query.isBlank()) {
      return self().findAll(pageable);
    }
    Specification<T> spec = self().filterByAllFields(query);
    return self().findAllSpecification(spec, pageable);
  }
}

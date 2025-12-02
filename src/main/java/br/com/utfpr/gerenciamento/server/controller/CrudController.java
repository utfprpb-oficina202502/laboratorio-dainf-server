package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.BaseListDto;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.util.SortableFieldExtractor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
public abstract class CrudController<T, ID extends Serializable, DTO extends BaseListDto> {

  /** Propriedade de ordenacao padrao quando nenhuma e especificada. */
  protected static final String DEFAULT_SORT_PROPERTY = "id";

  protected abstract CrudService<T, ID, DTO> getService();

  @GetMapping
  public List<DTO> findAll() {
    return getService().findAll(Sort.by(DEFAULT_SORT_PROPERTY));
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

  /**
   * Endpoint paginado com suporte a ordenacao e filtro.
   *
   * <p>Retorna {@code Page<? extends BaseListDto>} para permitir que subclasses retornem DTOs
   * simplificados (ListDto) sem necessidade de casting inseguro.
   *
   * @param page Numero da pagina (0-indexed)
   * @param size Tamanho da pagina (1-100)
   * @param filter Texto para busca em todos os campos visiveis (case insensitive)
   * @param sort Ordenacao no formato "campo,direcao" (ex: "nome,desc"). Default: "id,asc"
   * @return Pagina de DTOs (ou ListDTOs em subclasses)
   */
  @GetMapping("page")
  public Page<? extends BaseListDto> findAllPaged(
      @RequestParam("page") @Min(0) int page,
      @RequestParam("size") @Min(1) @Max(100) int size,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String sort) {
    Sort sortObj = parseSortParameter(sort);
    PageRequest pageRequest = PageRequest.of(page, size, sortObj);
    if (filter != null && !filter.isEmpty()) {
      Specification<T> spec = getService().filterByAllFields(filter);
      return getService().findAllSpecification(spec, pageRequest);
    }
    return getService().findAll(pageRequest);
  }

  /**
   * Retorna a classe do ListDto usado para listagem paginada.
   *
   * <p>Sobrescreva em subclasses que usam ListDto separado do DTO principal. Se retornar null, usa
   * o metodo getAllowedSortProperties() para definir campos permitidos manualmente.
   *
   * <p>Quando especificado, os campos ordenáveis são extraídos automaticamente dos campos anotados
   * com {@link br.com.utfpr.gerenciamento.server.dto.SortableField}.
   *
   * @return classe do ListDto, ou null para usar whitelist manual
   */
  protected Class<? extends BaseListDto> getListDtoClass() {
    return null;
  }

  /**
   * Retorna o conjunto de propriedades permitidas para ordenacao.
   *
   * <p>Se getListDtoClass() retornar uma classe, extrai automaticamente os campos anotados com
   * {@link br.com.utfpr.gerenciamento.server.dto.SortableField}. Caso contrário, retorna apenas
   * "id" como padrão.
   *
   * <p>Sobrescreva em subclasses para definir propriedades específicas manualmente quando não usar
   * annotations.
   *
   * @return Set de nomes de propriedades válidas para ordenação
   */
  protected Set<String> getAllowedSortProperties() {
    Class<? extends BaseListDto> listDtoClass = getListDtoClass();
    if (listDtoClass != null) {
      Set<String> sortableFields = SortableFieldExtractor.getAllowedSortProperties(listDtoClass);
      if (!sortableFields.isEmpty()) {
        return sortableFields;
      }
    }
    return Set.of(DEFAULT_SORT_PROPERTY);
  }

  /**
   * Retorna o mapeamento de campos DTO para paths da entidade.
   *
   * <p>Usado para traduzir nomes de campos do frontend (DTO) para paths JPA (entidade). Extraído
   * automaticamente dos campos anotados com {@link
   * br.com.utfpr.gerenciamento.server.dto.SortableField}.
   *
   * @return mapa de campo DTO → path da entidade
   */
  protected Map<String, String> getSortFieldMappings() {
    Class<? extends BaseListDto> listDtoClass = getListDtoClass();
    if (listDtoClass != null) {
      return SortableFieldExtractor.extractSortableFields(listDtoClass);
    }
    return Map.of();
  }

  /**
   * Converte parametro de ordenacao para objeto Sort com validacao.
   *
   * <p>Valida a propriedade contra a whitelist retornada por getAllowedSortProperties(). Se a
   * propriedade nao for permitida, usa a ordenacao padrao por seguranca.
   *
   * <p>Traduz automaticamente nomes de campos do DTO para paths da entidade quando há mapeamento
   * definido via {@link br.com.utfpr.gerenciamento.server.dto.SortableField}.
   *
   * @param sort String no formato "campo,direcao" (ex: "nome,desc")
   * @return Sort configurado, default: ordenacao por id ascendente
   */
  protected Sort parseSortParameter(String sort) {
    if (sort == null || sort.isBlank()) {
      return Sort.by(DEFAULT_SORT_PROPERTY);
    }
    String[] parts = sort.split(",");
    String dtoProperty = parts[0].trim();

    // Valida propriedade contra whitelist
    if (!getAllowedSortProperties().contains(dtoProperty)) {
      return Sort.by(DEFAULT_SORT_PROPERTY);
    }

    // Traduz nome do DTO para path da entidade (se houver mapeamento)
    Map<String, String> mappings = getSortFieldMappings();
    String entityPath = mappings.getOrDefault(dtoProperty, dtoProperty);

    Sort.Direction direction =
        (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    return Sort.by(direction, entityPath);
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

package br.com.utfpr.gerenciamento.server.util;

import br.com.utfpr.gerenciamento.server.dto.SortableField;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilitário para extrair campos ordenáveis de classes DTO anotadas com {@link SortableField}.
 *
 * <p>Utiliza cache para evitar reflection repetitiva em cada requisição.
 */
@Slf4j
public final class SortableFieldExtractor {

  /** Cache de campos ordenáveis por classe DTO. */
  private static final Map<Class<?>, Map<String, String>> CACHE = new ConcurrentHashMap<>();

  private SortableFieldExtractor() {
    // Utility class
  }

  /**
   * Extrai os campos ordenáveis de uma classe DTO.
   *
   * <p>Retorna um mapa onde a chave é o nome do campo no DTO (usado pelo frontend) e o valor é o
   * path da entidade JPA (usado no Sort).
   *
   * @param dtoClass classe do DTO a analisar
   * @return mapa de campo DTO → path da entidade
   */
  public static Map<String, String> extractSortableFields(Class<?> dtoClass) {
    return CACHE.computeIfAbsent(dtoClass, SortableFieldExtractor::doExtract);
  }

  /**
   * Retorna apenas os nomes dos campos ordenáveis (para validação de whitelist).
   *
   * @param dtoClass classe do DTO a analisar
   * @return conjunto de nomes de campos ordenáveis
   */
  public static Set<String> getAllowedSortProperties(Class<?> dtoClass) {
    return extractSortableFields(dtoClass).keySet();
  }

  /**
   * Traduz o nome do campo do DTO para o path da entidade.
   *
   * @param dtoClass classe do DTO
   * @param dtoFieldName nome do campo no DTO
   * @return path da entidade, ou o próprio nome se não encontrado
   */
  public static String translateToEntityPath(Class<?> dtoClass, String dtoFieldName) {
    Map<String, String> mappings = extractSortableFields(dtoClass);
    return mappings.getOrDefault(dtoFieldName, dtoFieldName);
  }

  private static Map<String, String> doExtract(Class<?> dtoClass) {
    Map<String, String> result = new HashMap<>();

    for (Field field : dtoClass.getDeclaredFields()) {
      SortableField annotation = field.getAnnotation(SortableField.class);
      if (annotation != null) {
        String dtoFieldName = field.getName();
        String entityPath =
            annotation.entityPath().isEmpty() ? dtoFieldName : annotation.entityPath();
        result.put(dtoFieldName, entityPath);
        log.debug(
            "Campo ordenável encontrado: {} -> {} em {}",
            dtoFieldName,
            entityPath,
            dtoClass.getSimpleName());
      }
    }

    log.debug(
        "Extraídos {} campos ordenáveis de {}: {}",
        result.size(),
        dtoClass.getSimpleName(),
        result.keySet());
    return result;
  }
}

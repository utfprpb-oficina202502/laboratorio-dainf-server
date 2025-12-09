package br.com.utfpr.gerenciamento.server.audit.service;

import static br.com.utfpr.gerenciamento.server.audit.AuditConstants.*;

import br.com.utfpr.gerenciamento.server.audit.AuditRevision;
import br.com.utfpr.gerenciamento.server.audit.dto.AuditEntryDto;
import br.com.utfpr.gerenciamento.server.audit.dto.AuditTimelineEntryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para consultar histórico de auditoria via Hibernate Envers.
 *
 * <p>Todas as operações são read-only e executadas em transação para garantir snapshot consistente
 * dos dados de auditoria.
 *
 * @author Rodrigo Izidoro
 * @see AuditRevision
 * @see AuditEntryDto
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  private static final String REVISION_TIMESTAMP_PROPERTY = "timestamp";

  private static final Set<String> SENSITIVE_FIELDS =
      Set.of("password", "codigoVerificacao", "documento", "telefone");

  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper;

  /**
   * Obtém o histórico de revisões de uma entidade com paginação.
   *
   * @param <T> tipo da entidade
   * @param entityClass classe da entidade
   * @param id identificador da entidade
   * @param pageable configuração de paginação
   * @return página de entradas de auditoria
   */
  @SuppressWarnings("unchecked")
  public <T> Page<AuditEntryDto> getHistoricoPaginado(
      Class<T> entityClass, Long id, Pageable pageable) {
    AuditReader reader = AuditReaderFactory.get(entityManager);

    long total = contarRevisoes(entityClass, id);

    if (total == 0) {
      return Page.empty(pageable);
    }

    List<Object[]> resultados =
        reader
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().eq(id))
            .addOrder(AuditEntity.revisionNumber().desc())
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

    List<AuditEntryDto> historico = processarResultados(resultados, entityClass);
    return new PageImpl<>(historico, pageable, total);
  }

  /**
   * Obtém o número total de revisões de uma entidade.
   *
   * <p>Usa projeção COUNT para evitar carregar todos os IDs de revisão na memória.
   *
   * @param <T> tipo da entidade
   * @param entityClass classe da entidade
   * @param id identificador da entidade
   * @return número total de revisões
   */
  public <T> long contarRevisoes(Class<T> entityClass, Long id) {
    AuditReader reader = AuditReaderFactory.get(entityManager);

    Number count =
        (Number)
            reader
                .createQuery()
                .forRevisionsOfEntity(entityClass, false, false)
                .add(AuditEntity.id().eq(id))
                .addProjection(AuditEntity.revisionNumber().count())
                .getSingleResult();

    return count != null ? count.longValue() : 0L;
  }

  /**
   * Obtém o estado da entidade em uma revisão específica.
   *
   * @param <T> tipo da entidade
   * @param entityClass classe da entidade
   * @param id identificador da entidade
   * @param revisao número da revisão
   * @return estado da entidade naquela revisão, ou null se não encontrada
   */
  public <T> T getEstadoEmRevisao(Class<T> entityClass, Long id, Long revisao) {
    AuditReader reader = AuditReaderFactory.get(entityManager);
    return reader.find(entityClass, id, revisao);
  }

  /**
   * Obtém o estado da entidade em um momento específico.
   *
   * @param <T> tipo da entidade
   * @param entityClass classe da entidade
   * @param id identificador da entidade
   * @param dataHora momento para consulta
   * @return estado da entidade naquele momento, ou null se não existia
   */
  public <T> T getEstadoEm(Class<T> entityClass, Long id, LocalDateTime dataHora) {
    AuditReader reader = AuditReaderFactory.get(entityManager);
    Date date = Date.from(dataHora.atZone(BRAZIL_ZONE).toInstant());

    Number revision = reader.getRevisionNumberForDate(date);
    if (revision == null) {
      return null;
    }

    return reader.find(entityClass, id, revision);
  }

  /**
   * Obtém informações de uma revisão específica.
   *
   * @param revisao número da revisão
   * @return informações da revisão, ou null se não encontrada
   */
  public AuditEntryDto getRevisao(Long revisao) {
    AuditReader reader = AuditReaderFactory.get(entityManager);
    AuditRevision revInfo = reader.findRevision(AuditRevision.class, revisao);

    if (revInfo == null) {
      return null;
    }

    return AuditEntryDto.builder()
        .revisao(revisao)
        .dataHora(timestampToLocalDateTime(revInfo.getTimestamp()))
        .usuario(revInfo.getUsuario())
        .ip(revInfo.getIp())
        .build();
  }

  /**
   * Obtém a timeline global de auditoria com filtros opcionais.
   *
   * <p>Busca revisões de todas as entidades auditadas, ordena por data/hora decrescente e aplica
   * paginação. Suporta filtros por período, usuário, entidade e tipo de operação.
   *
   * <p><strong>Nota de Performance:</strong> Quando nenhum filtro de data é informado, aplica
   * automaticamente um filtro dos últimos {@value #DEFAULT_TIMELINE_DAYS} dias. Cada entidade
   * retorna no máximo {@value #MAX_TIMELINE_RESULTS_PER_ENTITY} registros.
   *
   * @param pageable configuração de paginação
   * @param dataInicio data inicial do período (opcional, default: últimos 30 dias)
   * @param dataFim data final do período (opcional, default: hoje)
   * @param usuario username do usuário (opcional)
   * @param entidadeTipo tipo de entidade (opcional)
   * @param tipoOperacao tipo de operação: CRIACAO, ALTERACAO, EXCLUSAO (opcional)
   * @return página de entradas da timeline
   */
  @SuppressWarnings("unchecked")
  public Page<AuditTimelineEntryDto> getTimelineGlobal(
      Pageable pageable,
      LocalDate dataInicio,
      LocalDate dataFim,
      String usuario,
      String entidadeTipo,
      String tipoOperacao) {

    AuditReader reader = AuditReaderFactory.get(entityManager);
    List<AuditTimelineEntryDto> allEntries = new ArrayList<>();

    // Aplica filtro de data default quando não informado
    LocalDate dataInicioEfetiva = dataInicio;
    LocalDate dataFimEfetiva = dataFim;

    if (dataInicioEfetiva == null && dataFimEfetiva == null) {
      dataFimEfetiva = LocalDate.now();
      dataInicioEfetiva = dataFimEfetiva.minusDays(DEFAULT_TIMELINE_DAYS);
      log.debug(
          "Timeline sem filtro de data - aplicando default: {} a {}",
          dataInicioEfetiva,
          dataFimEfetiva);
    }

    // Determina quais entidades buscar
    Set<String> entidadesParaBuscar =
        entidadeTipo != null && ENTITY_MAP.containsKey(entidadeTipo)
            ? Set.of(entidadeTipo)
            : ENTITY_MAP.keySet();

    // Converte datas para timestamps
    Long timestampInicio =
        dataInicioEfetiva != null
            ? dataInicioEfetiva.atStartOfDay(BRAZIL_ZONE).toInstant().toEpochMilli()
            : null;
    Long timestampFim =
        dataFimEfetiva != null
            ? dataFimEfetiva.plusDays(1).atStartOfDay(BRAZIL_ZONE).toInstant().toEpochMilli()
            : null;

    // Converte tipo de operação
    RevisionType filtroRevisionType = parseRevisionType(tipoOperacao);

    // Busca revisões de cada entidade
    for (String entidadeKey : entidadesParaBuscar) {
      Class<?> entityClass = ENTITY_MAP.get(entidadeKey);
      String entityLabel = ENTITY_LABELS.get(entidadeKey);

      try {
        List<AuditTimelineEntryDto> entries =
            buscarRevisoesEntidade(
                reader,
                entityClass,
                entidadeKey,
                entityLabel,
                timestampInicio,
                timestampFim,
                usuario,
                filtroRevisionType);
        allEntries.addAll(entries);
      } catch (Exception e) {
        log.warn("Erro ao buscar revisões de {}: {}", entidadeKey, e.getMessage());
      }
    }

    // Ordena por data/hora decrescente
    allEntries.sort(Comparator.comparing(AuditTimelineEntryDto::getDataHora).reversed());

    // Aplica paginação manual
    int total = allEntries.size();
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), total);

    List<AuditTimelineEntryDto> pageContent =
        start < total ? allEntries.subList(start, end) : Collections.emptyList();

    return new PageImpl<>(pageContent, pageable, total);
  }

  /**
   * Retorna as chaves das entidades auditáveis.
   *
   * @return conjunto de chaves de entidades
   */
  public Set<String> getEntidadesAuditaveis() {
    return ENTITY_MAP.keySet();
  }

  /**
   * Retorna o mapa de labels das entidades em pt-BR.
   *
   * @return mapa de labels
   */
  public Map<String, String> getEntityLabels() {
    return ENTITY_LABELS;
  }

  @SuppressWarnings("unchecked")
  private <T> List<AuditEntryDto> processarResultados(
      List<Object[]> resultados, Class<T> entityClass) {
    List<AuditEntryDto> historico = new ArrayList<>();

    for (Object[] resultado : resultados) {
      try {
        T entidade = (T) resultado[0];
        AuditRevision revInfo = (AuditRevision) resultado[1];
        RevisionType tipo = (RevisionType) resultado[2];

        AuditEntryDto entry =
            AuditEntryDto.builder()
                .revisao(revInfo.getId())
                .dataHora(timestampToLocalDateTime(revInfo.getTimestamp()))
                .usuario(revInfo.getUsuario())
                .ip(revInfo.getIp())
                .tipoOperacao(traduzirTipo(tipo))
                .entidade(entityToMap(entidade))
                .build();

        historico.add(entry);
      } catch (ClassCastException e) {
        log.warn("Revisão corrompida para {}: {}", entityClass.getSimpleName(), e.getMessage());
      } catch (Exception e) {
        log.error("Erro crítico ao processar revisão para {}: ", entityClass.getSimpleName(), e);
      }
    }

    return historico;
  }

  @SuppressWarnings("unchecked")
  private List<AuditTimelineEntryDto> buscarRevisoesEntidade(
      AuditReader reader,
      Class<?> entityClass,
      String entidadeTipo,
      String entidadeLabel,
      Long timestampInicio,
      Long timestampFim,
      String usuario,
      RevisionType tipoOperacao) {

    List<AuditTimelineEntryDto> entries = new ArrayList<>();

    AuditQuery query = reader.createQuery().forRevisionsOfEntity(entityClass, false, true);

    // Aplica filtro de tipo de operação na query
    if (tipoOperacao != null) {
      query.add(AuditEntity.revisionType().eq(tipoOperacao));
    }

    // Aplica filtros de data na query (otimização)
    if (timestampInicio != null) {
      query.add(AuditEntity.revisionProperty(REVISION_TIMESTAMP_PROPERTY).ge(timestampInicio));
    }
    if (timestampFim != null) {
      query.add(AuditEntity.revisionProperty(REVISION_TIMESTAMP_PROPERTY).lt(timestampFim));
    }

    // Ordena por timestamp decrescente
    query.addOrder(AuditEntity.revisionProperty(REVISION_TIMESTAMP_PROPERTY).desc());

    // Limita resultados para performance
    query.setMaxResults(MAX_TIMELINE_RESULTS_PER_ENTITY);

    List<Object[]> resultados = query.getResultList();

    for (Object[] resultado : resultados) {
      try {
        Object entidade = resultado[0];
        AuditRevision revInfo = (AuditRevision) resultado[1];
        RevisionType tipo = (RevisionType) resultado[2];

        // Aplica filtro de usuário (não suportado nativamente pelo Envers)
        if (usuario != null
            && !usuario.isBlank()
            && !usuario.equalsIgnoreCase(revInfo.getUsuario())) {
          continue;
        }

        Long entidadeId = extractEntityId(entidade);

        AuditTimelineEntryDto entry =
            AuditTimelineEntryDto.builder()
                .revisao(revInfo.getId())
                .dataHora(timestampToLocalDateTime(revInfo.getTimestamp()))
                .usuario(revInfo.getUsuario())
                .ip(revInfo.getIp())
                .tipoOperacao(traduzirTipo(tipo))
                .entidadeTipo(entidadeTipo)
                .entidadeLabel(entidadeLabel)
                .entidadeId(entidadeId)
                .entidade(entityToMap(entidade))
                .build();

        entries.add(entry);
      } catch (Exception e) {
        log.warn("Erro ao processar revisão de {}: {}", entidadeTipo, e.getMessage());
      }
    }

    return entries;
  }

  private RevisionType parseRevisionType(String tipoOperacao) {
    if (tipoOperacao == null) {
      return null;
    }
    return switch (tipoOperacao.toUpperCase()) {
      case "CRIACAO" -> RevisionType.ADD;
      case "ALTERACAO" -> RevisionType.MOD;
      case "EXCLUSAO" -> RevisionType.DEL;
      default -> null;
    };
  }

  private String traduzirTipo(RevisionType tipo) {
    return switch (tipo) {
      case ADD -> "CRIACAO";
      case MOD -> "ALTERACAO";
      case DEL -> "EXCLUSAO";
    };
  }

  private LocalDateTime timestampToLocalDateTime(Long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(BRAZIL_ZONE).toLocalDateTime();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> entityToMap(Object entity) {
    if (entity == null) {
      return Collections.emptyMap();
    }

    try {
      Map<String, Object> map = objectMapper.convertValue(entity, Map.class);

      SENSITIVE_FIELDS.forEach(map::remove);

      map.entrySet()
          .removeIf(
              entry -> {
                Object value = entry.getValue();
                return value != null && value.getClass().getName().contains("HibernateProxy");
              });

      return map;
    } catch (IllegalArgumentException e) {
      log.warn("Erro ao converter entidade para Map: {}", e.getMessage());
      return Collections.emptyMap();
    }
  }

  /**
   * Extrai o ID de uma entidade usando reflection.
   *
   * @param entity entidade
   * @return ID da entidade ou null se não encontrado
   */
  private Long extractEntityId(Object entity) {
    if (entity == null) {
      return null;
    }
    try {
      var idMethod = entity.getClass().getDeclaredMethod("getId");
      Object id = idMethod.invoke(entity);
      if (id instanceof Long longId) {
        return longId;
      } else if (id instanceof Integer intId) {
        return intId.longValue();
      }
    } catch (Exception e) {
      log.trace("Não foi possível extrair ID da entidade: {}", e.getMessage());
    }
    return null;
  }
}

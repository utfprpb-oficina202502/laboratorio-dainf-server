package br.com.utfpr.gerenciamento.server.audit.controller;

import static br.com.utfpr.gerenciamento.server.audit.AuditConstants.*;

import br.com.utfpr.gerenciamento.server.audit.dto.AuditEntryDto;
import br.com.utfpr.gerenciamento.server.audit.dto.AuditTimelineEntryDto;
import br.com.utfpr.gerenciamento.server.audit.service.AuditService;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.Audited;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para consulta de histórico de auditoria.
 *
 * <p>Todos os endpoints são protegidos por ROLE_ADMIN, garantindo que apenas administradores possam
 * consultar o histórico de alterações das entidades.
 *
 * @author Rodrigo Izidoro
 * @see AuditService
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Validated
@Slf4j
public class AuditController {

  private final AuditService auditService;

  /**
   * Obtém o histórico de revisões de uma entidade com paginação.
   *
   * @param entidade nome da entidade (ex: emprestimo, item, usuario)
   * @param id identificador da entidade
   * @param page número da página (0-indexed)
   * @param size tamanho da página (máximo 100)
   * @return página de entradas de auditoria
   */
  @GetMapping("/{entidade}/{id}")
  public ResponseEntity<Page<AuditEntryDto>> getHistorico(
      @PathVariable String entidade,
      @PathVariable @Min(1) Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Class<?> entityClass = validarEntidade(entidade);
    if (entityClass == null) {
      return ResponseEntity.notFound().build();
    }

    int safeSize = normalizarTamanhoPagina(size);

    PageRequest pageRequest =
        PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "revisao"));
    Page<AuditEntryDto> historico = auditService.getHistoricoPaginado(entityClass, id, pageRequest);
    return ResponseEntity.ok(historico);
  }

  /**
   * Obtém o número total de revisões de uma entidade.
   *
   * @param entidade nome da entidade
   * @param id identificador da entidade
   * @return contagem de revisões
   */
  @GetMapping("/{entidade}/{id}/count")
  public ResponseEntity<Long> contarRevisoes(
      @PathVariable String entidade, @PathVariable @Min(1) Long id) {

    Class<?> entityClass = validarEntidade(entidade);
    if (entityClass == null) {
      return ResponseEntity.notFound().build();
    }

    long count = auditService.contarRevisoes(entityClass, id);
    return ResponseEntity.ok(count);
  }

  /**
   * Obtém informações de uma revisão específica.
   *
   * @param revisao número da revisão
   * @return informações da revisão
   */
  @GetMapping("/revisao/{revisao}")
  public ResponseEntity<AuditEntryDto> getRevisao(@PathVariable @Min(1) Long revisao) {
    AuditEntryDto revInfo = auditService.getRevisao(revisao);
    if (revInfo == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(revInfo);
  }

  /**
   * Lista as entidades disponíveis para consulta de auditoria.
   *
   * @return lista de nomes de entidades
   */
  @GetMapping("/entidades")
  public ResponseEntity<List<String>> listarEntidades() {
    return ResponseEntity.ok(ENTITY_MAP.keySet().stream().sorted().toList());
  }

  /**
   * Obtém a timeline global de auditoria com filtros opcionais.
   *
   * <p>Retorna uma visão consolidada de todas as alterações do sistema, ordenadas por data/hora
   * decrescente. Suporta filtros por período, usuário, entidade e tipo de operação.
   *
   * <p><strong>Nota:</strong> Quando nenhum filtro de data é informado, retorna os últimos 30 dias.
   * Cada entidade retorna no máximo 500 registros para garantir performance.
   *
   * @param page número da página (0-indexed)
   * @param size tamanho da página (máximo 100)
   * @param dataInicio data inicial do período (formato ISO: yyyy-MM-dd)
   * @param dataFim data final do período (formato ISO: yyyy-MM-dd)
   * @param usuario username do usuário que realizou a alteração
   * @param entidade tipo de entidade (ex: emprestimo, item, usuario)
   * @param tipoOperacao tipo de operação: CRIACAO, ALTERACAO, EXCLUSAO
   * @return página de entradas da timeline
   */
  @GetMapping("/timeline")
  public ResponseEntity<Page<AuditTimelineEntryDto>> getTimeline(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      @RequestParam(required = false) String usuario,
      @RequestParam(required = false) String entidade,
      @RequestParam(required = false) String tipoOperacao) {

    int safeSize = normalizarTamanhoPagina(size);

    PageRequest pageRequest = PageRequest.of(page, safeSize);
    Page<AuditTimelineEntryDto> timeline =
        auditService.getTimelineGlobal(
            pageRequest, dataInicio, dataFim, usuario, entidade, tipoOperacao);

    return ResponseEntity.ok(timeline);
  }

  /**
   * Retorna o mapa de labels das entidades em pt-BR.
   *
   * @return mapa de labels (chave técnica -> label pt-BR)
   */
  @GetMapping("/entidades/labels")
  public ResponseEntity<Map<String, String>> getEntidadeLabels() {
    return ResponseEntity.ok(ENTITY_LABELS);
  }

  private int normalizarTamanhoPagina(int size) {
    int safeSize = Math.min(size, MAX_PAGE_SIZE);
    if (safeSize <= 0) {
      safeSize = DEFAULT_PAGE_SIZE;
    }
    return safeSize;
  }

  private Class<?> validarEntidade(String entidade) {
    if (entidade == null || !entidade.matches("^[a-z\\-]+$")) {
      log.warn("Nome de entidade inválido: {}", entidade);
      return null;
    }

    Class<?> entityClass = ENTITY_MAP.get(entidade.toLowerCase());
    if (entityClass == null) {
      log.warn("Entidade não encontrada: {}", entidade);
      return null;
    }

    if (!entityClass.isAnnotationPresent(Audited.class)) {
      log.error("SECURITY: Tentativa de acesso a entidade não auditada: {}", entityClass.getName());
      return null;
    }

    return entityClass;
  }
}

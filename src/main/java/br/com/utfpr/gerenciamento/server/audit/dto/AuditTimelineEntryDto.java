package br.com.utfpr.gerenciamento.server.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * DTO representando uma entrada na timeline global de auditoria.
 *
 * <p>Estende as informações de {@link AuditEntryDto} com campos adicionais para identificar o tipo
 * de entidade, permitindo uma visualização consolidada de todas as alterações do sistema.
 *
 * @author Rodrigo Izidoro
 * @see AuditEntryDto
 */
@Data
@Builder
public class AuditTimelineEntryDto {

  /** Número da revisão no Hibernate Envers. */
  private Long revisao;

  /** Data e hora da operação (timezone America/Sao_Paulo). */
  private LocalDateTime dataHora;

  /** Username do usuário que realizou a operação. */
  private String usuario;

  /** Endereço IP da requisição. */
  private String ip;

  /** Tipo da operação: CRIACAO, ALTERACAO ou EXCLUSAO. */
  private String tipoOperacao;

  /** Identificador técnico da entidade (ex: "emprestimo", "item"). */
  private String entidadeTipo;

  /** Label amigável da entidade em pt-BR (ex: "Empréstimo", "Item"). */
  private String entidadeLabel;

  /** ID da entidade alterada. */
  private Long entidadeId;

  /** Estado da entidade no momento da revisão (campos sensíveis removidos). */
  private Map<String, Object> entidade;
}

package br.com.utfpr.gerenciamento.server.dto.dashboards;

import java.time.LocalDateTime;

/**
 * DTO de resposta para atividades recentes do usuario (timeline).
 *
 * @param dataHora Data e hora da atividade
 * @param tipo Tipo da atividade: EMPRESTIMO_RETIRADA, EMPRESTIMO_DEVOLUCAO, RESERVA_CRIADA
 * @param titulo Titulo descritivo da atividade
 * @param descricao Descricao detalhada
 * @param referenciaId ID do emprestimo ou reserva
 * @param referenciaTipo Tipo da referencia: EMPRESTIMO ou RESERVA
 */
public record AtividadeUsuarioDto(
    LocalDateTime dataHora,
    String tipo,
    String titulo,
    String descricao,
    Long referenciaId,
    String referenciaTipo) {}

package br.com.utfpr.gerenciamento.server.dto.dashboards;

import java.time.LocalDate;

/**
 * DTO de resposta para eventos do calendario do usuario.
 *
 * @param data Data do evento
 * @param tipo Tipo do evento: RETIRADA, DEVOLUCAO_PREVISTA, DEVOLUCAO_REALIZADA, ATRASADO
 * @param emprestimoId ID do emprestimo relacionado
 * @param descricao Descricao com nomes dos itens
 */
public record EventoCalendarioDto(
    LocalDate data, String tipo, Long emprestimoId, String descricao) {}

package br.com.utfpr.gerenciamento.server.dto.dashboards;

/**
 * DTO de resposta para historico de uso mensal do usuario.
 *
 * @param mes Mes no formato "2025-01"
 * @param mesLabel Label do mes no formato "Jan/25" (pt-BR)
 * @param quantidade Total de emprestimos no mes
 */
public record HistoricoUsoMensalDto(String mes, String mesLabel, int quantidade) {}

package br.com.utfpr.gerenciamento.server.dto.dashboards;

/**
 * Projection para agregacao de estatisticas de emprestimos do usuario.
 *
 * @param emAberto Emprestimos em aberto (dentro do prazo)
 * @param emAtraso Emprestimos em atraso (prazo vencido)
 * @param total Total de emprestimos
 */
public record EstatisticasEmprestimoProjection(Long emAberto, Long emAtraso, Long total) {}

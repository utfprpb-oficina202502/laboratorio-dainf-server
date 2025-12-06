package br.com.utfpr.gerenciamento.server.dto.dashboards;

import java.time.LocalDate;

/**
 * DTO de resposta para estatisticas pessoais do usuario logado.
 *
 * @param emprestimosEmAberto Emprestimos ativos dentro do prazo
 * @param emprestimosEmAtraso Emprestimos ativos com prazo vencido
 * @param emprestimosTotal Total de emprestimos do usuario
 * @param proximaDevolucao Data da proxima devolucao pendente
 * @param diasParaProximaDevolucao Dias ate a proxima devolucao (negativo se atrasado)
 */
public record EstatisticasUsuarioDto(
    int emprestimosEmAberto,
    int emprestimosEmAtraso,
    int emprestimosTotal,
    LocalDate proximaDevolucao,
    Integer diasParaProximaDevolucao) {}

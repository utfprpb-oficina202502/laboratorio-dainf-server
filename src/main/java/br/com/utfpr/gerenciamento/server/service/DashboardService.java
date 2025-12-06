package br.com.utfpr.gerenciamento.server.service;

import br.com.utfpr.gerenciamento.server.dto.dashboards.*;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

  DashboardEmprestimoCountRangeResponseDto findDadosEmprestimoCountRange(
      LocalDate dtIni, LocalDate dtFim);

  List<DashboardEmprestimoDiaResponseDto> findTotalEmprestimoByDia(
      LocalDate dtIni, LocalDate dtFim);

  List<DashboardItensEmprestadosResponseDto> findItensMaisEmprestados(
      LocalDate dtIni, LocalDate dtFim);

  List<DashboardItensAdquiridosResponseDto> findItensMaisAdquiridos(
      LocalDate dtIni, LocalDate dtFim);

  List<DashboardItensSaidasResponseDto> findItensComMaisSaidas(LocalDate dtIni, LocalDate dtFim);

  // ========== DASHBOARD PESSOAL DO USUARIO (Aluno/Professor) ==========

  /**
   * Retorna estatisticas pessoais do usuario logado.
   *
   * @return Estatisticas de emprestimos do usuario
   */
  EstatisticasUsuarioDto findEstatisticasUsuarioLogado();

  /**
   * Retorna os itens mais emprestados pelo usuario logado.
   *
   * @param limit Quantidade maxima de itens
   * @return Lista de itens frequentes
   */
  List<ItemFrequenteUsuarioDto> findItensFrequentesUsuarioLogado(int limit);

  /**
   * Retorna o historico de uso mensal do usuario logado.
   *
   * @param meses Quantidade de meses para buscar
   * @return Lista de uso por mes
   */
  List<HistoricoUsoMensalDto> findHistoricoUsoUsuarioLogado(int meses);

  /**
   * Retorna as atividades recentes do usuario logado.
   *
   * @param limit Quantidade maxima de atividades
   * @return Lista de atividades
   */
  List<AtividadeUsuarioDto> findAtividadesUsuarioLogado(int limit);

  /**
   * Retorna os eventos de calendario do usuario logado.
   *
   * @param dtIni Data inicial
   * @param dtFim Data final
   * @return Lista de eventos
   */
  List<EventoCalendarioDto> findEventosCalendarioUsuarioLogado(LocalDate dtIni, LocalDate dtFim);
}

package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.dashboards.*;
import br.com.utfpr.gerenciamento.server.service.DashboardService;
import br.com.utfpr.gerenciamento.server.util.DateUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("dashboard")
@Validated
public class HomeController {

  private final DashboardService dashboardService;

  public HomeController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  // ========== DASHBOARD ADMINISTRATIVO (Sistema) ==========

  @GetMapping("emprestimo-count-range")
  public DashboardEmprestimoCountRangeResponseDto findDadosEmprestimoCountRange(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findDadosEmprestimoCountRange(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }

  @GetMapping("emprestimo-count-day-range")
  public List<DashboardEmprestimoDiaResponseDto> findDadosEmprestimoByDayRange(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findTotalEmprestimoByDia(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }

  @GetMapping("itens-mais-emprestados")
  public List<DashboardItensEmprestadosResponseDto> findItensMaisEmprestados(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findItensMaisEmprestados(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }

  @GetMapping("itens-mais-adquiridos")
  public List<DashboardItensAdquiridosResponseDto> findItensMaisAdquiridos(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findItensMaisAdquiridos(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }

  @GetMapping("itens-mais-saidas")
  public List<DashboardItensSaidasResponseDto> findItensMaisSaidas(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findItensComMaisSaidas(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }

  // ========== DASHBOARD PESSOAL DO USUARIO (Aluno/Professor) ==========

  /**
   * Retorna estatisticas pessoais do usuario logado. Filtra automaticamente pelo username do JWT.
   */
  @GetMapping("my-stats")
  public EstatisticasUsuarioDto findEstatisticasUsuarioLogado() {
    return dashboardService.findEstatisticasUsuarioLogado();
  }

  /**
   * Retorna os itens mais emprestados pelo usuario logado.
   *
   * @param limit Quantidade maxima de itens (1-20, default 5)
   */
  @GetMapping("my-frequent-items")
  public List<ItemFrequenteUsuarioDto> findItensFrequentesUsuarioLogado(
      @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
    return dashboardService.findItensFrequentesUsuarioLogado(limit);
  }

  /**
   * Retorna o historico de uso mensal do usuario logado.
   *
   * @param meses Quantidade de meses para buscar (1-24, default 6)
   */
  @GetMapping("my-usage-history")
  public List<HistoricoUsoMensalDto> findHistoricoUsoUsuarioLogado(
      @RequestParam(defaultValue = "6") @Min(1) @Max(24) int meses) {
    return dashboardService.findHistoricoUsoUsuarioLogado(meses);
  }

  /**
   * Retorna as atividades recentes do usuario logado (timeline).
   *
   * @param limit Quantidade maxima de atividades (1-100, default 20)
   */
  @GetMapping("my-activity")
  public List<AtividadeUsuarioDto> findAtividadesUsuarioLogado(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return dashboardService.findAtividadesUsuarioLogado(limit);
  }

  /**
   * Retorna os eventos de calendario do usuario logado.
   *
   * @param dtIni Data inicial (formato dd/MM/yyyy)
   * @param dtFim Data final (formato dd/MM/yyyy)
   */
  @GetMapping("my-calendar-events")
  public List<EventoCalendarioDto> findEventosCalendarioUsuarioLogado(
      @RequestParam("dtIni") String dtIni, @RequestParam("dtFim") String dtFim) {
    return dashboardService.findEventosCalendarioUsuarioLogado(
        DateUtil.parseStringToLocalDate(dtIni), DateUtil.parseStringToLocalDate(dtFim));
  }
}

package br.com.utfpr.gerenciamento.server.service.impl;

import br.com.utfpr.gerenciamento.server.dto.dashboards.*;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.repository.EmprestimoRepository;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
import br.com.utfpr.gerenciamento.server.service.CompraService;
import br.com.utfpr.gerenciamento.server.service.DashboardService;
import br.com.utfpr.gerenciamento.server.service.EmprestimoService;
import br.com.utfpr.gerenciamento.server.service.SaidaService;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

  private static final Locale PT_BR = Locale.of("pt", "BR");
  private static final DateTimeFormatter MES_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("MMM/yy", PT_BR);

  private final EmprestimoService emprestimoService;
  private final EmprestimoRepository emprestimoRepository;
  private final ReservaRepository reservaRepository;
  private final CompraService compraService;
  private final SaidaService saidaService;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(
      value = "dashboard-emprestimo-range",
      keyGenerator = "dashboardCacheKeyGenerator",
      unless = "#result == null")
  public DashboardEmprestimoCountRangeResponseDto findDadosEmprestimoCountRange(
      LocalDate dtIni, LocalDate dtFim) {
    var result = emprestimoRepository.countEmprestimosByStatusInRange(dtIni, dtFim);

    if (result == null) {
      return new DashboardEmprestimoCountRangeResponseDto(0L, 0L, 0L, 0L);
    }

    return new DashboardEmprestimoCountRangeResponseDto(
        result.total(), result.emAndamento(), result.emAtraso(), result.finalizado());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardEmprestimoDiaResponseDto> findTotalEmprestimoByDia(
      LocalDate dtIni, LocalDate dtFim) {
    return emprestimoService.countByDataEmprestimo(dtIni, dtFim).stream()
        .map(m -> new DashboardEmprestimoDiaResponseDto(m.qtde(), m.dtEmprestimo()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardItensEmprestadosResponseDto> findItensMaisEmprestados(
      LocalDate dtIni, LocalDate dtFim) {
    return emprestimoService.findItensMaisEmprestados(dtIni, dtFim).stream()
        .map(m -> new DashboardItensEmprestadosResponseDto(m.qtde(), m.item()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardItensAdquiridosResponseDto> findItensMaisAdquiridos(
      LocalDate dtIni, LocalDate dtFim) {
    return compraService.findItensMaisAdquiridos(dtIni, dtFim).stream()
        .map(m -> new DashboardItensAdquiridosResponseDto(m.qtde(), m.item()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardItensSaidasResponseDto> findItensComMaisSaidas(
      LocalDate dtIni, LocalDate dtFim) {
    return saidaService.findItensMaisSaidas(dtIni, dtFim).stream()
        .map(m -> new DashboardItensSaidasResponseDto(m.qtde(), m.item()))
        .toList();
  }

  // ========== DASHBOARD PESSOAL DO USUARIO (Aluno/Professor) ==========

  @Override
  @Transactional(readOnly = true)
  public EstatisticasUsuarioDto findEstatisticasUsuarioLogado() {
    String username = SecurityUtils.getAuthenticatedUsername();

    EstatisticasEmprestimoProjection stats =
        emprestimoRepository.countEstatisticasByUsername(username);
    LocalDate proximaDevolucao = emprestimoRepository.findProximaDevolucaoByUsername(username);

    int emAberto = stats != null ? stats.emAberto().intValue() : 0;
    int emAtraso = stats != null ? stats.emAtraso().intValue() : 0;
    int total = stats != null ? stats.total().intValue() : 0;

    Integer diasParaProximaDevolucao = null;
    if (proximaDevolucao != null) {
      diasParaProximaDevolucao = (int) ChronoUnit.DAYS.between(LocalDate.now(), proximaDevolucao);
    }

    return new EstatisticasUsuarioDto(
        emAberto, emAtraso, total, proximaDevolucao, diasParaProximaDevolucao);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemFrequenteUsuarioDto> findItensFrequentesUsuarioLogado(int limit) {
    String username = SecurityUtils.getAuthenticatedUsername();
    return emprestimoRepository.findItensMaisEmprestadosByUsername(
        username, PageRequest.of(0, limit));
  }

  @Override
  @Transactional(readOnly = true)
  public List<HistoricoUsoMensalDto> findHistoricoUsoUsuarioLogado(int meses) {
    String username = SecurityUtils.getAuthenticatedUsername();
    LocalDate dtFim = LocalDate.now();
    LocalDate dtIni = dtFim.minusMonths(meses);

    List<Object[]> resultados =
        emprestimoRepository.countEmprestimosPorMesByUsername(username, dtIni, dtFim);

    return resultados.stream()
        .map(
            row -> {
              int ano = ((Number) row[0]).intValue();
              int mes = ((Number) row[1]).intValue();
              int quantidade = ((Number) row[2]).intValue();

              LocalDate data = LocalDate.of(ano, mes, 1);
              String mesFormatado = String.format("%d-%02d", ano, mes);
              String mesLabel = data.format(MES_LABEL_FORMATTER);
              // Capitaliza a primeira letra (jan/25 -> Jan/25)
              mesLabel = mesLabel.substring(0, 1).toUpperCase() + mesLabel.substring(1);

              return new HistoricoUsoMensalDto(mesFormatado, mesLabel, quantidade);
            })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AtividadeUsuarioDto> findAtividadesUsuarioLogado(int limit) {
    String username = SecurityUtils.getAuthenticatedUsername();

    List<AtividadeUsuarioDto> atividades = new ArrayList<>();

    // Buscar emprestimos do usuario
    List<Emprestimo> emprestimos =
        emprestimoRepository.findEmprestimosParaAtividadesByUsername(
            username, PageRequest.of(0, limit));

    for (Emprestimo e : emprestimos) {
      String itensDesc =
          e.getEmprestimoItem().stream()
              .map(ei -> ei.getItem().getNome())
              .collect(Collectors.joining(", "));

      int qtdeItens = e.getEmprestimoItem().size();

      // Atividade de retirada
      atividades.add(
          new AtividadeUsuarioDto(
              e.getDataEmprestimo().atStartOfDay(),
              "EMPRESTIMO_RETIRADA",
              "Retirada de itens",
              qtdeItens + " item(ns): " + itensDesc,
              e.getId(),
              "EMPRESTIMO"));

      // Atividade de devolucao (se houve)
      if (e.getDataDevolucao() != null) {
        atividades.add(
            new AtividadeUsuarioDto(
                e.getDataDevolucao().atStartOfDay(),
                "EMPRESTIMO_DEVOLUCAO",
                "Devolucao de itens",
                qtdeItens + " item(ns) devolvido(s)",
                e.getId(),
                "EMPRESTIMO"));
      }
    }

    // Buscar reservas do usuario
    List<Reserva> reservas =
        reservaRepository.findReservasParaAtividadesByUsername(username, PageRequest.of(0, limit));

    for (Reserva r : reservas) {
      String itensDesc =
          r.getReservaItem().stream()
              .map(ri -> ri.getItem().getNome())
              .collect(Collectors.joining(", "));

      int qtdeItens = r.getReservaItem().size();

      atividades.add(
          new AtividadeUsuarioDto(
              r.getDataReserva().atStartOfDay(),
              "RESERVA_CRIADA",
              "Reserva criada",
              qtdeItens + " item(ns): " + itensDesc,
              r.getId(),
              "RESERVA"));
    }

    // Ordenar por data decrescente e limitar
    return atividades.stream()
        .sorted(Comparator.comparing(AtividadeUsuarioDto::dataHora).reversed())
        .limit(limit)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<EventoCalendarioDto> findEventosCalendarioUsuarioLogado(
      LocalDate dtIni, LocalDate dtFim) {
    String username = SecurityUtils.getAuthenticatedUsername();

    List<Emprestimo> emprestimos =
        emprestimoRepository.findEmprestimosParaCalendarioByUsername(username, dtIni, dtFim);

    List<EventoCalendarioDto> eventos = new ArrayList<>();

    for (Emprestimo e : emprestimos) {
      adicionarEventosDoEmprestimo(e, dtIni, dtFim, eventos);
    }

    return eventos;
  }

  /**
   * Adiciona eventos de calendario para um emprestimo especifico.
   *
   * @param emprestimo O emprestimo a processar
   * @param dtIni Data inicial do periodo
   * @param dtFim Data final do periodo
   * @param eventos Lista onde os eventos serao adicionados
   */
  private void adicionarEventosDoEmprestimo(
      Emprestimo emprestimo, LocalDate dtIni, LocalDate dtFim, List<EventoCalendarioDto> eventos) {

    String itensDesc =
        emprestimo.getEmprestimoItem().stream()
            .map(ei -> ei.getItem().getNome())
            .collect(Collectors.joining(", "));

    adicionarEventoRetirada(emprestimo, dtIni, dtFim, itensDesc, eventos);
    adicionarEventoDevolucao(emprestimo, dtIni, dtFim, itensDesc, eventos);
  }

  private void adicionarEventoRetirada(
      Emprestimo e,
      LocalDate dtIni,
      LocalDate dtFim,
      String itensDesc,
      List<EventoCalendarioDto> eventos) {

    if (isDataDentroDoRange(e.getDataEmprestimo(), dtIni, dtFim)) {
      eventos.add(new EventoCalendarioDto(e.getDataEmprestimo(), "RETIRADA", e.getId(), itensDesc));
    }
  }

  private void adicionarEventoDevolucao(
      Emprestimo e,
      LocalDate dtIni,
      LocalDate dtFim,
      String itensDesc,
      List<EventoCalendarioDto> eventos) {

    if (e.getDataDevolucao() != null) {
      // Emprestimo devolvido
      if (isDataDentroDoRange(e.getDataDevolucao(), dtIni, dtFim)) {
        eventos.add(
            new EventoCalendarioDto(
                e.getDataDevolucao(), "DEVOLUCAO_REALIZADA", e.getId(), itensDesc));
      }
    } else {
      // Emprestimo em aberto - mostrar prazo
      adicionarEventoPrazoDevolucao(e, dtIni, dtFim, itensDesc, eventos);
    }
  }

  private void adicionarEventoPrazoDevolucao(
      Emprestimo e,
      LocalDate dtIni,
      LocalDate dtFim,
      String itensDesc,
      List<EventoCalendarioDto> eventos) {

    if (isDataDentroDoRange(e.getPrazoDevolucao(), dtIni, dtFim)) {
      String tipo =
          e.getPrazoDevolucao().isBefore(LocalDate.now()) ? "ATRASADO" : "DEVOLUCAO_PREVISTA";
      eventos.add(new EventoCalendarioDto(e.getPrazoDevolucao(), tipo, e.getId(), itensDesc));
    }
  }

  private boolean isDataDentroDoRange(LocalDate data, LocalDate dtIni, LocalDate dtFim) {
    return !data.isBefore(dtIni) && !data.isAfter(dtFim);
  }
}

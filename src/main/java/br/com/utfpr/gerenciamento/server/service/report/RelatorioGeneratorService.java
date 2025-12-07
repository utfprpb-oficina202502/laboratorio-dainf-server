package br.com.utfpr.gerenciamento.server.service.report;

import br.com.utfpr.gerenciamento.server.dto.relatorios.*;
import br.com.utfpr.gerenciamento.server.enumeration.FormatoRelatorio;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço orquestrador para geração de relatórios.
 *
 * <p>Coordena a busca de dados (ReportDataService) com a geração de arquivos (PdfGeneratorService,
 * ExcelGeneratorService).
 *
 * <p>Suporta os 6 relatórios migrados do Jasper Reports: 1. Histórico de Empréstimo do Usuário 2.
 * Itens Sem Estoque 3. Empréstimos Realizados Entre Datas 4. Reservas do Item 5. Solicitações do
 * Item 6. Itens que Atingiram Quantidade Mínima
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioGeneratorService {

  private final ReportDataService reportDataService;
  private final PdfGeneratorService pdfGeneratorService;
  private final ExcelGeneratorService excelGeneratorService;

  // ========== 1. HISTÓRICO DE EMPRÉSTIMO DO USUÁRIO ==========

  /**
   * Gera relatório de histórico de empréstimos de um usuário.
   *
   * @param documento RA ou SIAPE do usuário
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarHistoricoEmprestimo(String documento, FormatoRelatorio formato) {
    log.info(
        "Gerando relatório Histórico de Empréstimo - Documento: {}, Formato: {}",
        documento,
        formato);

    List<HistoricoEmprestimoDto> dados = reportDataService.getHistoricoEmprestimo(documento);

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("documento", documento);
        model.put("emprestimos", dados);
        yield pdfGeneratorService.generatePdf("historico-emprestimo", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Histórico de Empréstimo",
              new String[] {
                "Cód",
                "Nome do Usuário",
                "Dt Empréstimo",
                "Prazo Devolução",
                "Dt Devolução",
                "Situação"
              },
              dados,
              HistoricoEmprestimoDto::getCod,
              HistoricoEmprestimoDto::getNomeUsuario,
              HistoricoEmprestimoDto::getDataEmprestimo,
              HistoricoEmprestimoDto::getPrazoDevolucao,
              HistoricoEmprestimoDto::getDataDevolucao,
              HistoricoEmprestimoDto::getSituacao);
    };
  }

  // ========== 2. ITENS SEM ESTOQUE ==========

  /**
   * Gera relatório de itens com saldo zero.
   *
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarItensSemEstoque(FormatoRelatorio formato) {
    log.info("Gerando relatório Itens Sem Estoque - Formato: {}", formato);

    List<ItemSemEstoqueDto> dados = reportDataService.getItensSemEstoque();

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("itens", dados);
        yield pdfGeneratorService.generatePdf("itens-sem-estoque", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Itens Sem Estoque",
              new String[] {"Cód", "Nome", "Patrimônio", "SIORG", "Qtde Mínima", "Grupo"},
              dados,
              ItemSemEstoqueDto::getCod,
              ItemSemEstoqueDto::getNome,
              ItemSemEstoqueDto::getPatrimonio,
              ItemSemEstoqueDto::getSiorg,
              ItemSemEstoqueDto::getQtdeMinima,
              ItemSemEstoqueDto::getGrupo);
    };
  }

  // ========== 3. EMPRÉSTIMOS REALIZADOS ENTRE DATAS ==========

  /**
   * Gera relatório de empréstimos em um período.
   *
   * @param dataInicio Data inicial do período
   * @param dataFim Data final do período
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarEmprestimosRealizados(
      LocalDate dataInicio, LocalDate dataFim, FormatoRelatorio formato) {
    log.info(
        "Gerando relatório Empréstimos Realizados - Período: {} a {}, Formato: {}",
        dataInicio,
        dataFim,
        formato);

    List<EmprestimoRealizadoDto> dados =
        reportDataService.getEmprestimosRealizados(dataInicio, dataFim);

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("dataInicio", dataInicio);
        model.put("dataFim", dataFim);
        model.put("emprestimos", dados);
        yield pdfGeneratorService.generatePdf("emprestimos-realizados", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Empréstimos Realizados",
              new String[] {
                "Cód", "Aluno/Professor", "Usuário Responsável", "Dt Empréstimo", "Situação"
              },
              dados,
              EmprestimoRealizadoDto::getCod,
              EmprestimoRealizadoDto::getUsuarioEmprestimo,
              EmprestimoRealizadoDto::getUsuarioResponsavel,
              EmprestimoRealizadoDto::getDataEmprestimo,
              EmprestimoRealizadoDto::getSituacao);
    };
  }

  // ========== 4. RESERVAS DO ITEM ==========

  /**
   * Gera relatório de reservas de um item específico.
   *
   * @param itemId ID do item
   * @param nomeItem Nome do item (para exibição no relatório)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarReservasDoItem(Long itemId, String nomeItem, FormatoRelatorio formato) {
    log.info("Gerando relatório Reservas do Item - Item ID: {}, Formato: {}", itemId, formato);

    List<ReservaItemDto> dados = reportDataService.getReservasDoItem(itemId);

    String nomeExibicao = resolverNomeItem(nomeItem, itemId, dados, ReservaItemDto::getNomeItem);

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("nomeItem", nomeExibicao);
        model.put("reservas", dados);
        yield pdfGeneratorService.generatePdf("reserva-item", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Reservas - " + nomeExibicao,
              new String[] {"Cód", "Dt Reserva", "Dt Retirada", "Usuário da Reserva", "Qtde"},
              dados,
              ReservaItemDto::getCod,
              ReservaItemDto::getDataReserva,
              ReservaItemDto::getDataRetirada,
              ReservaItemDto::getUsuarioReserva,
              ReservaItemDto::getQtde);
    };
  }

  // ========== 5. SOLICITAÇÕES DO ITEM ==========

  /**
   * Gera relatório de solicitações de compra de um item.
   *
   * @param itemId ID do item
   * @param nomeItem Nome do item (para exibição no relatório)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarSolicitacoesDoItem(Long itemId, String nomeItem, FormatoRelatorio formato) {
    log.info("Gerando relatório Solicitações do Item - Item ID: {}, Formato: {}", itemId, formato);

    List<SolicitacaoItemDto> dados = reportDataService.getSolicitacoesDoItem(itemId);

    String nomeExibicao =
        resolverNomeItem(nomeItem, itemId, dados, SolicitacaoItemDto::getNomeItem);

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("nomeItem", nomeExibicao);
        model.put("solicitacoes", dados);
        yield pdfGeneratorService.generatePdf("solicitacao-item", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Solicitações - " + nomeExibicao,
              new String[] {"Cód", "Dt Solicitação", "Descrição", "Usuário Solicitação", "Qtde"},
              dados,
              SolicitacaoItemDto::getCod,
              SolicitacaoItemDto::getDataSolicitacao,
              SolicitacaoItemDto::getDescricao,
              SolicitacaoItemDto::getUsuarioSolicitacao,
              SolicitacaoItemDto::getQtde);
    };
  }

  // ========== 6. ITENS QUE ATINGIRAM QUANTIDADE MÍNIMA ==========

  /**
   * Gera relatório de itens com saldo menor ou igual à quantidade mínima.
   *
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Array de bytes do arquivo gerado
   */
  public byte[] gerarItensQtdeMinima(FormatoRelatorio formato) {
    log.info("Gerando relatório Itens Quantidade Mínima - Formato: {}", formato);

    List<ItemQtdeMinimaDto> dados = reportDataService.getItensQtdeMinima();

    return switch (formato) {
      case PDF -> {
        Map<String, Object> model = new HashMap<>();
        model.put("itens", dados);
        yield pdfGeneratorService.generatePdf("itens-qtde-minima", model);
      }
      case EXCEL ->
          excelGeneratorService.generateExcel(
              "Itens Qtde Mínima",
              new String[] {"Cód", "Nome", "Grupo", "Qtde Mínima", "Saldo"},
              dados,
              ItemQtdeMinimaDto::getCod,
              ItemQtdeMinimaDto::getNome,
              ItemQtdeMinimaDto::getGrupo,
              ItemQtdeMinimaDto::getQtdeMinima,
              ItemQtdeMinimaDto::getSaldo);
    };
  }

  // ========== MÉTODOS AUXILIARES ==========

  /**
   * Resolve o nome do item para exibição no relatório.
   *
   * <p>Usa o nome fornecido se disponível, caso contrário tenta obter do primeiro registro da
   * lista. Se a lista estiver vazia, usa um fallback genérico.
   *
   * @param <T> Tipo dos dados da lista
   * @param nomeItem Nome fornecido externamente (pode ser null ou vazio)
   * @param itemId ID do item (usado no fallback)
   * @param dados Lista de dados (para extrair nome do primeiro registro)
   * @param nomeExtractor Função para extrair o nome do item do DTO
   * @return Nome do item para exibição
   */
  private <T> String resolverNomeItem(
      String nomeItem, Long itemId, List<T> dados, Function<T, String> nomeExtractor) {
    if (nomeItem != null && !nomeItem.isBlank()) {
      return nomeItem;
    }
    return dados.stream().findFirst().map(nomeExtractor).orElse("Item " + itemId);
  }
}

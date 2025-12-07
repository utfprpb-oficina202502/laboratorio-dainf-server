package br.com.utfpr.gerenciamento.server.controller;

import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_ADMINISTRADOR_NAME;
import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_LABORATORISTA_NAME;

import br.com.utfpr.gerenciamento.server.enumeration.FormatoRelatorio;
import br.com.utfpr.gerenciamento.server.exception.RelatorioException;
import br.com.utfpr.gerenciamento.server.service.report.RelatorioGeneratorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para geração de relatórios em PDF e Excel.
 *
 * <p>Endpoints para os 6 relatórios disponíveis: 1. /relatorio/historico-emprestimo - Histórico de
 * empréstimos por documento 2. /relatorio/itens-sem-estoque - Itens com saldo zero 3.
 * /relatorio/emprestimos-realizados - Empréstimos em um período 4. /relatorio/reservas-item -
 * Reservas de um item 5. /relatorio/solicitacoes-item - Solicitações de compra de um item 6.
 * /relatorio/itens-qtde-minima - Itens abaixo da quantidade mínima
 *
 * <p>Todos os endpoints aceitam o parâmetro 'formato' (PDF ou EXCEL, padrão PDF).
 */
@Slf4j
@Validated
@RestController
@RequestMapping("relatorio")
@RequiredArgsConstructor
public class RelatorioController {

  private final RelatorioGeneratorService relatorioGeneratorService;

  /** Período máximo permitido para consultas de relatório (2 anos). */
  private static final int PERIODO_MAXIMO_ANOS = 2;

  /**
   * Gera relatório de histórico de empréstimos de um usuário.
   *
   * @param documento RA ou SIAPE do usuário (obrigatório)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("historico-emprestimo")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarHistoricoEmprestimo(
      @RequestParam @NotBlank(message = "Documento é obrigatório") String documento,
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info("Requisição de relatório Histórico de Empréstimo - Documento: {}", documento);

    byte[] conteudo = relatorioGeneratorService.gerarHistoricoEmprestimo(documento, formato);

    return buildResponse(conteudo, "historico-emprestimo-" + documento, formato);
  }

  /**
   * Gera relatório de itens sem estoque.
   *
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("itens-sem-estoque")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarItensSemEstoque(
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info("Requisição de relatório Itens Sem Estoque");

    byte[] conteudo = relatorioGeneratorService.gerarItensSemEstoque(formato);

    return buildResponse(conteudo, "itens-sem-estoque", formato);
  }

  /**
   * Gera relatório de empréstimos realizados em um período.
   *
   * @param dataInicio Data inicial (formato dd/MM/yyyy, obrigatório)
   * @param dataFim Data final (formato dd/MM/yyyy, obrigatório)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("emprestimos-realizados")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarEmprestimosRealizados(
      @RequestParam
          @NotNull(message = "Data inicial é obrigatória") @DateTimeFormat(pattern = "dd/MM/yyyy")
          LocalDate dataInicio,
      @RequestParam
          @NotNull(message = "Data final é obrigatória") @DateTimeFormat(pattern = "dd/MM/yyyy")
          LocalDate dataFim,
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info(
        "Requisição de relatório Empréstimos Realizados - Período: {} a {}", dataInicio, dataFim);

    // Validar período de datas (prevenção de DoS via consultas muito amplas)
    validarPeriodoDatas(dataInicio, dataFim);

    byte[] conteudo =
        relatorioGeneratorService.gerarEmprestimosRealizados(dataInicio, dataFim, formato);

    return buildResponse(conteudo, "emprestimos-realizados", formato);
  }

  /**
   * Gera relatório de reservas de um item.
   *
   * @param itemId ID do item (obrigatório, deve ser positivo)
   * @param nomeItem Nome do item (opcional, para exibição)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("reservas-item")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarReservasDoItem(
      @RequestParam @NotNull(message = "ID do item é obrigatório") @Positive Long itemId,
      @RequestParam(required = false) String nomeItem,
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info("Requisição de relatório Reservas do Item - ID: {}", itemId);

    byte[] conteudo = relatorioGeneratorService.gerarReservasDoItem(itemId, nomeItem, formato);

    return buildResponse(conteudo, "reservas-item-" + itemId, formato);
  }

  /**
   * Gera relatório de solicitações de compra de um item.
   *
   * @param itemId ID do item (obrigatório, deve ser positivo)
   * @param nomeItem Nome do item (opcional, para exibição)
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("solicitacoes-item")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarSolicitacoesDoItem(
      @RequestParam @NotNull(message = "ID do item é obrigatório") @Positive Long itemId,
      @RequestParam(required = false) String nomeItem,
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info("Requisição de relatório Solicitações do Item - ID: {}", itemId);

    byte[] conteudo = relatorioGeneratorService.gerarSolicitacoesDoItem(itemId, nomeItem, formato);

    return buildResponse(conteudo, "solicitacoes-item-" + itemId, formato);
  }

  /**
   * Gera relatório de itens que atingiram a quantidade mínima.
   *
   * @param formato Formato de saída (PDF ou EXCEL)
   * @return Arquivo do relatório
   */
  @GetMapping("itens-qtde-minima")
  @PreAuthorize("hasAnyRole('" + ROLE_LABORATORISTA_NAME + "', '" + ROLE_ADMINISTRADOR_NAME + "')")
  public ResponseEntity<byte[]> gerarItensQtdeMinima(
      @RequestParam(defaultValue = "PDF") FormatoRelatorio formato) {

    log.info("Requisição de relatório Itens Quantidade Mínima");

    byte[] conteudo = relatorioGeneratorService.gerarItensQtdeMinima(formato);

    return buildResponse(conteudo, "itens-qtde-minima", formato);
  }

  /**
   * Constrói a resposta HTTP com headers apropriados para download de arquivo.
   *
   * <p>Sanitiza o nome do arquivo para prevenir ataques de Path Traversal (OWASP A01:2021).
   *
   * @param conteudo Bytes do arquivo
   * @param nomeBase Nome base do arquivo (sem extensão)
   * @param formato Formato do relatório
   * @return ResponseEntity configurado para download
   */
  private ResponseEntity<byte[]> buildResponse(
      byte[] conteudo, String nomeBase, FormatoRelatorio formato) {

    String nomeSanitizado = sanitizarNomeArquivo(nomeBase);
    String nomeArquivo = nomeSanitizado + "." + formato.getExtensao();

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
        .contentType(formato.getMediaType())
        .contentLength(conteudo.length)
        .body(conteudo);
  }

  /**
   * Valida o período de datas para relatórios.
   *
   * <p>Regras de validação: - Data final não pode ser anterior à data inicial - Data final não pode
   * ser no futuro - Período máximo de 2 anos (prevenção de DoS via consultas muito amplas)
   *
   * @param dataInicio Data inicial do período
   * @param dataFim Data final do período
   * @throws RelatorioException Se o período for inválido
   */
  private void validarPeriodoDatas(LocalDate dataInicio, LocalDate dataFim) {
    LocalDate hoje = LocalDate.now();

    if (dataFim.isBefore(dataInicio)) {
      throw new RelatorioException("Data final não pode ser anterior à data inicial");
    }

    if (dataFim.isAfter(hoje)) {
      throw new RelatorioException("Data final não pode ser no futuro");
    }

    if (dataInicio.plusYears(PERIODO_MAXIMO_ANOS).isBefore(dataFim)) {
      throw new RelatorioException("Período não pode exceder " + PERIODO_MAXIMO_ANOS + " anos");
    }
  }

  /**
   * Sanitiza nome de arquivo removendo caracteres potencialmente perigosos.
   *
   * <p>Remove: - Caracteres de controle (0x00-0x1F, 0x7F) - Separadores de caminho (/, \, ..) -
   * Caracteres especiais do sistema - Espaços excessivos
   *
   * @param nomeBase Nome original
   * @return Nome sanitizado seguro para uso em Content-Disposition
   */
  private String sanitizarNomeArquivo(String nomeBase) {
    if (nomeBase == null || nomeBase.isEmpty()) {
      return "relatorio";
    }

    // Remove caracteres de controle, path traversal e especiais
    return nomeBase
        .replaceAll("[\\x00-\\x1F\\x7F]", "") // Remove caracteres de controle
        .replaceAll("[/\\\\]", "") // Remove separadores de path
        .replace("..", "") // Remove .. (path traversal)
        .replaceAll("[<>:\"|?*]", "") // Remove caracteres inválidos em Windows
        .replaceAll("\\s+", "-") // Substitui espaços por hífen
        .replaceAll("-+", "-") // Remove hífens duplicados
        .replaceAll("(^-)|(-$)", ""); // Remove hífens no início/fim
  }
}

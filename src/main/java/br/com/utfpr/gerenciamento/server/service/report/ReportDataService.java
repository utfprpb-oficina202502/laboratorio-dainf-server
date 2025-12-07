package br.com.utfpr.gerenciamento.server.service.report;

import br.com.utfpr.gerenciamento.server.dto.relatorios.*;
import br.com.utfpr.gerenciamento.server.model.*;
import br.com.utfpr.gerenciamento.server.repository.*;
import br.com.utfpr.gerenciamento.server.specification.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar dados para geração de relatórios.
 *
 * <p>Utiliza JPA Specifications para construir queries dinâmicas, substituindo as queries SQL
 * hardcoded dos templates Jasper.
 *
 * <p>A lógica de negócio de cada relatório é preservada conforme documentado nos arquivos .jrxml
 * originais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportDataService {

  private final EmprestimoRepository emprestimoRepository;
  private final ItemRepository itemRepository;
  private final ReservaRepository reservaRepository;
  private final SolicitacaoRepository solicitacaoRepository;

  // ========== 1. HISTÓRICO DE EMPRÉSTIMO DO USUÁRIO ==========

  /**
   * Busca histórico de empréstimos de um usuário pelo documento (RA/SIAPE).
   *
   * <p>Preserva a lógica do relatório Jasper HistoricoEmprestimoUsuario.jrxml: - Filtra por
   * usuario.documento - Calcula situação: "Em atraso", "Em andamento", "Finalizado" - Ordena por ID
   * do empréstimo
   *
   * @param documento Documento (RA ou SIAPE) do usuário
   * @return Lista de empréstimos formatados para relatório
   */
  public List<HistoricoEmprestimoDto> getHistoricoEmprestimo(String documento) {
    log.debug("Buscando histórico de empréstimo para documento: {}", documento);

    return emprestimoRepository
        .findAll(
            EmprestimoSpecifications.byUsuarioDocumento(documento),
            Sort.by(Sort.Direction.ASC, "id"))
        .stream()
        .map(this::toHistoricoEmprestimoDto)
        .toList();
  }

  private HistoricoEmprestimoDto toHistoricoEmprestimoDto(Emprestimo e) {
    return HistoricoEmprestimoDto.builder()
        .cod(e.getId())
        .nomeUsuario(Optional.ofNullable(e.getUsuarioEmprestimo()).map(Usuario::getNome).orElse(""))
        .dataEmprestimo(e.getDataEmprestimo())
        .prazoDevolucao(e.getPrazoDevolucao())
        .dataDevolucao(e.getDataDevolucao())
        .situacao(calcularSituacao(e))
        .build();
  }

  // ========== 2. ITENS SEM ESTOQUE ==========

  /**
   * Busca itens com saldo igual a zero.
   *
   * <p>Preserva a lógica do relatório Jasper ItensSemEstoque.jrxml: - Filtra WHERE saldo = 0 -
   * Inclui dados do grupo
   *
   * @return Lista de itens sem estoque
   */
  public List<ItemSemEstoqueDto> getItensSemEstoque() {
    log.debug("Buscando itens sem estoque");

    return itemRepository.findAll(ItemSpecifications.forRelatorioItensSemEstoque()).stream()
        .map(this::toItemSemEstoqueDto)
        .toList();
  }

  private ItemSemEstoqueDto toItemSemEstoqueDto(Item i) {
    return ItemSemEstoqueDto.builder()
        .cod(i.getId())
        .nome(i.getNome())
        .patrimonio(i.getPatrimonio())
        .siorg(i.getSiorg())
        .qtdeMinima(i.getQtdeMinima())
        .grupo(Optional.ofNullable(i.getGrupo()).map(Grupo::getDescricao).orElse(""))
        .build();
  }

  // ========== 3. EMPRÉSTIMOS REALIZADOS ENTRE DATAS ==========

  /**
   * Busca empréstimos realizados em um período.
   *
   * <p>Preserva a lógica do relatório Jasper EmprestimosRealizadosEntre.jrxml: - Filtra por
   * dataEmprestimo BETWEEN inicio AND fim - Inclui usuário do empréstimo e responsável - Calcula
   * situação igual ao relatório 1
   *
   * @param inicio Data inicial do período
   * @param fim Data final do período
   * @return Lista de empréstimos do período
   */
  public List<EmprestimoRealizadoDto> getEmprestimosRealizados(LocalDate inicio, LocalDate fim) {
    log.debug("Buscando empréstimos realizados entre {} e {}", inicio, fim);

    return emprestimoRepository
        .findAll(EmprestimoSpecifications.byDataEmprestimoBetween(inicio, fim))
        .stream()
        .map(this::toEmprestimoRealizadoDto)
        .toList();
  }

  private EmprestimoRealizadoDto toEmprestimoRealizadoDto(Emprestimo e) {
    return EmprestimoRealizadoDto.builder()
        .cod(e.getId())
        .usuarioEmprestimo(
            Optional.ofNullable(e.getUsuarioEmprestimo()).map(Usuario::getNome).orElse(""))
        .usuarioResponsavel(
            Optional.ofNullable(e.getUsuarioResponsavel()).map(Usuario::getNome).orElse(""))
        .dataEmprestimo(e.getDataEmprestimo())
        .situacao(calcularSituacao(e))
        .build();
  }

  // ========== 4. RESERVAS DO ITEM ==========

  /**
   * Busca reservas de um item específico.
   *
   * <p>Preserva a lógica do relatório Jasper ReservaDoItem.jrxml: - Filtra por item_id através da
   * tabela reserva_item - Inclui dados da reserva, usuário e quantidade - Ordena por data de
   * retirada
   *
   * @param itemId ID do item
   * @return Lista de reservas do item
   */
  public List<ReservaItemDto> getReservasDoItem(Long itemId) {
    log.debug("Buscando reservas do item: {}", itemId);

    return reservaRepository
        .findAll(ReservaSpecifications.forRelatorioReservaDoItem(itemId))
        .stream()
        .flatMap(
            r ->
                r.getReservaItem().stream()
                    .filter(ri -> ri.getItem() != null && ri.getItem().getId().equals(itemId))
                    .map(ri -> toReservaItemDto(r, ri)))
        .sorted((a, b) -> compareNullSafe(a.getDataRetirada(), b.getDataRetirada()))
        .toList();
  }

  private ReservaItemDto toReservaItemDto(Reserva r, ReservaItem ri) {
    return ReservaItemDto.builder()
        .cod(r.getId())
        .dataReserva(r.getDataReserva())
        .dataRetirada(r.getDataRetirada())
        .qtde(ri.getQtde())
        .usuarioReserva(Optional.ofNullable(r.getUsuario()).map(Usuario::getNome).orElse(""))
        .nomeItem(Optional.ofNullable(ri.getItem()).map(Item::getNome).orElse(""))
        .build();
  }

  // ========== 5. SOLICITAÇÕES DO ITEM ==========

  /**
   * Busca solicitações de compra de um item específico.
   *
   * <p>Preserva a lógica do relatório Jasper SolicitacaoItem.jrxml: - Filtra por item_id através da
   * tabela solicitacao_item - Inclui dados da solicitação, usuário e quantidade - Ordena por data
   * de solicitação
   *
   * @param itemId ID do item
   * @return Lista de solicitações do item
   */
  public List<SolicitacaoItemDto> getSolicitacoesDoItem(Long itemId) {
    log.debug("Buscando solicitações do item: {}", itemId);

    return solicitacaoRepository
        .findAll(SolicitacaoSpecifications.forRelatorioSolicitacaoItem(itemId))
        .stream()
        .flatMap(
            s ->
                s.getSolicitacaoItem().stream()
                    .filter(si -> si.getItem() != null && si.getItem().getId().equals(itemId))
                    .map(si -> toSolicitacaoItemDto(s, si)))
        .sorted((a, b) -> compareNullSafe(a.getDataSolicitacao(), b.getDataSolicitacao()))
        .toList();
  }

  private SolicitacaoItemDto toSolicitacaoItemDto(Solicitacao s, SolicitacaoItem si) {
    return SolicitacaoItemDto.builder()
        .cod(s.getId())
        .descricao(s.getDescricao())
        .dataSolicitacao(s.getDataSolicitacao())
        .qtde(si.getQtde())
        .usuarioSolicitacao(Optional.ofNullable(s.getUsuario()).map(Usuario::getNome).orElse(""))
        .nomeItem(Optional.ofNullable(si.getItem()).map(Item::getNome).orElse(""))
        .build();
  }

  // ========== 6. ITENS QUE ATINGIRAM QUANTIDADE MÍNIMA ==========

  /**
   * Busca itens onde saldo é menor ou igual à quantidade mínima.
   *
   * <p>Preserva a lógica do relatório Jasper ItensAtingiramQtdeMinima.jrxml: - Filtra WHERE saldo
   * <= qtde_minima - Inclui dados do grupo
   *
   * @return Lista de itens com estoque baixo
   */
  public List<ItemQtdeMinimaDto> getItensQtdeMinima() {
    log.debug("Buscando itens que atingiram quantidade mínima");

    return itemRepository.findAll(ItemSpecifications.forRelatorioItensQtdeMinima()).stream()
        .map(this::toItemQtdeMinimaDto)
        .toList();
  }

  private ItemQtdeMinimaDto toItemQtdeMinimaDto(Item i) {
    return ItemQtdeMinimaDto.builder()
        .cod(i.getId())
        .nome(i.getNome())
        .qtdeMinima(i.getQtdeMinima())
        .saldo(i.getSaldo())
        .grupo(Optional.ofNullable(i.getGrupo()).map(Grupo::getDescricao).orElse(""))
        .build();
  }

  // ========== MÉTODOS AUXILIARES ==========

  /**
   * Calcula a situação do empréstimo baseado nas datas.
   *
   * <p>Lógica preservada dos relatórios Jasper (CASE WHEN): - "Em atraso": sem data de devolução e
   * prazo vencido - "Em andamento": sem data de devolução e prazo não vencido - "Finalizado": com
   * data de devolução
   *
   * @param emprestimo Empréstimo a avaliar
   * @return String com a situação
   */
  private String calcularSituacao(Emprestimo emprestimo) {
    if (emprestimo.getDataDevolucao() != null) {
      return "Finalizado";
    }

    if (emprestimo.getPrazoDevolucao() != null
        && emprestimo.getPrazoDevolucao().isBefore(LocalDate.now())) {
      return "Em atraso";
    }

    return "Em andamento";
  }

  private <T extends Comparable<T>> int compareNullSafe(T a, T b) {
    if (a == null && b == null) return 0;
    if (a == null) return 1;
    if (b == null) return -1;
    return a.compareTo(b);
  }
}

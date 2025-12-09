package br.com.utfpr.gerenciamento.server.service.impl;

import br.com.utfpr.gerenciamento.server.dto.SaidaListDto;
import br.com.utfpr.gerenciamento.server.dto.SaidaResponseDTO;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.EmprestimoDevolucaoItem;
import br.com.utfpr.gerenciamento.server.model.Saida;
import br.com.utfpr.gerenciamento.server.model.SaidaItem;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardItensSaidas;
import br.com.utfpr.gerenciamento.server.repository.SaidaRepository;
import br.com.utfpr.gerenciamento.server.repository.projection.SaidaListProjection;
import br.com.utfpr.gerenciamento.server.service.SaidaService;
import br.com.utfpr.gerenciamento.server.util.HistoricoTransicaoUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaidaServiceImpl extends CrudServiceImpl<Saida, Long, SaidaResponseDTO>
    implements SaidaService {

  private final SaidaRepository saidaRepository;
  private final ModelMapper modelMapper;

  public SaidaServiceImpl(SaidaRepository saidaRepository, ModelMapper modelMapper) {
    this.saidaRepository = saidaRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  protected JpaRepository<Saida, Long> getRepository() {
    return saidaRepository;
  }

  @Override
  protected Map<String, String> getSearchableFieldMappings() {
    return Map.of(
        "id", "id",
        "dataSaida", "dataSaida",
        "observacao", "observacao",
        "usuarioResponsavelNome", "usuarioResponsavel.nome",
        "qtdeTotal", "qtdeTotal");
  }

  @Override
  public SaidaResponseDTO toDto(Saida entity) {
    return modelMapper.map(entity, SaidaResponseDTO.class);
  }

  @Override
  public Saida toEntity(SaidaResponseDTO saidaResponseDTO) {
    return modelMapper.map(saidaResponseDTO, Saida.class);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SaidaListDto> findAllPagedList(String filter, Pageable pageable) {
    Page<SaidaListProjection> page;
    if (filter != null && !filter.isBlank()) {
      page = saidaRepository.findAllProjectedWithFilter(filter, pageable);
    } else {
      page = saidaRepository.findAllProjected(pageable);
    }
    return page.map(SaidaListDto::fromProjection);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardItensSaidas> findItensMaisSaidas(LocalDate dtIni, LocalDate dtFim) {
    return saidaRepository.findItensMaisSaidas(dtIni, dtFim);
  }

  @Override
  @Transactional
  public void createSaidaByDevolucaoEmprestimo(
      List<EmprestimoDevolucaoItem> emprestimoDevolucaoItem) {
    if (emprestimoDevolucaoItem == null || emprestimoDevolucaoItem.isEmpty()) {
      throw new IllegalArgumentException("Lista de itens de devolução não pode estar vazia");
    }

    Emprestimo emprestimo =
        Objects.requireNonNull(
            emprestimoDevolucaoItem.get(0).getEmprestimo(),
            "Empréstimo do item de devolução não pode ser nulo");

    Usuario usuarioResponsavel =
        Objects.requireNonNull(
            emprestimo.getUsuarioResponsavel(), "Usuário responsável do empréstimo é obrigatório");
    Usuario usuarioEmprestimo =
        Objects.requireNonNull(
            emprestimo.getUsuarioEmprestimo(), "Usuário do empréstimo é obrigatório");

    String responsavelNome =
        Objects.requireNonNull(
            usuarioResponsavel.getNome(), "Nome do usuário responsável não pode ser nulo");
    String usuarioEmprestimoNome =
        Objects.requireNonNull(
            usuarioEmprestimo.getNome(), "Nome do usuário do empréstimo não pode ser nulo");

    Saida saida = new Saida();
    saida.setIdEmprestimo(emprestimo.getId());
    saida.setDataSaida(LocalDate.now());

    String observacaoComHistorico =
        HistoricoTransicaoUtil.formatarHistoricoEmprestimoParaSaida(
            emprestimo.getId(),
            responsavelNome,
            emprestimo.getDataEmprestimo(),
            usuarioEmprestimoNome,
            emprestimo.getObservacao());
    saida.setObservacao(observacaoComHistorico);
    saida.setUsuarioResponsavel(usuarioResponsavel);

    List<SaidaItem> saidaItemList =
        emprestimoDevolucaoItem.stream()
            .map(
                itemDevToSaida -> {
                  SaidaItem saidaItem = new SaidaItem();
                  saidaItem.setItem(itemDevToSaida.getItem());
                  saidaItem.setQtde(itemDevToSaida.getQtde());
                  saidaItem.setSaida(saida);
                  return saidaItem;
                })
            .toList();

    saida.setSaidaItem(saidaItemList);
    saidaRepository.save(saida);
  }

  @Override
  @Transactional
  public void deleteSaidaByEmprestimo(Long idEmprestimo) {
    var saidaToDelete = saidaRepository.findByIdEmprestimo(idEmprestimo);
    if (saidaToDelete != null) {
      saidaRepository.delete(saidaToDelete);
    }
  }
}

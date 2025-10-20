package br.com.utfpr.gerenciamento.server.service.impl;

import br.com.utfpr.gerenciamento.server.dto.CompraResponseDTO;
import br.com.utfpr.gerenciamento.server.dto.EmprestimoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Compra;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardItensAdquiridos;
import br.com.utfpr.gerenciamento.server.repository.CompraRepository;
import br.com.utfpr.gerenciamento.server.service.CompraService;
import java.time.LocalDate;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public  class CompraServiceImpl extends CrudServiceImpl<Compra, Long, CompraResponseDTO> implements CompraService {

  private final CompraRepository compraRepository;
  private final ModelMapper modelMapper;

  public CompraServiceImpl(CompraRepository compraRepository, ModelMapper modelMapper) {
    this.compraRepository = compraRepository;
      this.modelMapper = modelMapper;
  }

  @Override
  protected JpaRepository<Compra, Long> getRepository() {
    return compraRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DashboardItensAdquiridos> findItensMaisAdquiridos(LocalDate dtIni, LocalDate dtFim) {
    return compraRepository.findItensMaisAdquiridos(dtIni, dtFim);
  }

  @Override
  public CompraResponseDTO convertToDTO(Compra entity) {
    return modelMapper.map(entity, CompraResponseDTO.class);
  }

  @Override
  public Compra convertToEntity(CompraResponseDTO entity) {
    return modelMapper.map(entity, Compra.class);
  }
}

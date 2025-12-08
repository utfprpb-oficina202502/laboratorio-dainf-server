package br.com.utfpr.gerenciamento.server.service.impl;

import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_ADMINISTRADOR_NAME;
import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_LABORATORISTA_NAME;

import br.com.utfpr.gerenciamento.server.dto.SolicitacaoListDto;
import br.com.utfpr.gerenciamento.server.dto.SolicitacaoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Solicitacao;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.repository.SolicitacaoRepository;
import br.com.utfpr.gerenciamento.server.repository.projection.SolicitacaoListProjection;
import br.com.utfpr.gerenciamento.server.service.SolicitacaoService;
import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SolicitacaoServiceImpl
    extends CrudServiceImpl<Solicitacao, Long, SolicitacaoResponseDto>
    implements SolicitacaoService {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioService usuarioService;
  private final ModelMapper modelMapper;

  public SolicitacaoServiceImpl(
      SolicitacaoRepository solicitacaoRepository,
      UsuarioService usuarioService,
      ModelMapper modelMapper) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioService = usuarioService;
    this.modelMapper = modelMapper;
  }

  @Override
  protected JpaRepository<Solicitacao, Long> getRepository() {
    return solicitacaoRepository;
  }

  @Override
  protected Map<String, String> getSearchableFieldMappings() {
    return Map.of(
        "id", "id",
        "descricao", "descricao",
        "dataSolicitacao", "dataSolicitacao",
        "usuarioNome", "usuario.nome");
  }

  @Override
  public SolicitacaoResponseDto toDto(Solicitacao entity) {
    return modelMapper.map(entity, SolicitacaoResponseDto.class);
  }

  @Override
  public Solicitacao toEntity(SolicitacaoResponseDto solicitacaoResponseDto) {
    return modelMapper.map(solicitacaoResponseDto, Solicitacao.class);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SolicitacaoListDto> findAllPagedList(String filter, Pageable pageable) {
    Page<SolicitacaoListProjection> page;
    if (filter != null && !filter.isBlank()) {
      page = solicitacaoRepository.findAllProjectedWithFilter(filter, pageable);
    } else {
      page = solicitacaoRepository.findAllProjected(pageable);
    }
    return page.map(SolicitacaoListDto::fromProjection);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SolicitacaoResponseDto> findAllByUsername(String username) {
    // Busca o usuário alvo pelo username fornecido
    Usuario usuarioAlvo = usuarioService.toEntity(usuarioService.findByUsername(username));
    if (usuarioAlvo == null) {
      return Collections.emptyList();
    }

    // SEGURANÇA: Validação por ID - evita problemas de case-sensitivity
    // Admins e laboratoristas podem ver solicitações de qualquer usuário
    String authenticatedUsername = SecurityUtils.getAuthenticatedUsername();
    List<String> userRoles = SecurityUtils.getAuthenticatedUserRoles();

    boolean isAdminOrLaboratorista =
        userRoles.contains("ROLE_" + ROLE_ADMINISTRADOR_NAME)
            || userRoles.contains("ROLE_" + ROLE_LABORATORISTA_NAME);

    if (!isAdminOrLaboratorista) {
      // Usuários comuns só podem ver suas próprias solicitações
      Usuario usuarioLogado =
          usuarioService.toEntity(usuarioService.findByUsername(authenticatedUsername));
      if (usuarioLogado == null) {
        log.warn(
            "Tentativa de consultar solicitações com usuário não encontrado: username={}",
            authenticatedUsername);
        throw new AccessDeniedException("Usuário autenticado não encontrado");
      }

      if (!usuarioAlvo.getId().equals(usuarioLogado.getId())) {
        log.warn(
            "Tentativa de consultar solicitações não autorizada: usuário {} (ID: {}) tentou consultar solicitações do usuário {} (ID: {})",
            authenticatedUsername,
            usuarioLogado.getId(),
            username,
            usuarioAlvo.getId());
        throw new AccessDeniedException(
            "Usuário não tem permissão para consultar solicitações de outro usuário");
      }
    }

    return solicitacaoRepository.findAllByUsuario(usuarioAlvo).stream().map(this::toDto).toList();
  }
}

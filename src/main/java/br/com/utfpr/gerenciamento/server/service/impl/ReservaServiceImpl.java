package br.com.utfpr.gerenciamento.server.service.impl;

import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_ADMINISTRADOR_NAME;
import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_LABORATORISTA_NAME;

import br.com.utfpr.gerenciamento.server.dto.ReservaListDto;
import br.com.utfpr.gerenciamento.server.dto.ReservaResponseDto;
import br.com.utfpr.gerenciamento.server.event.reserva.ReservaCriadaEvent;
import br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException;
import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.model.modelTemplateEmail.ReservaTemplate;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
import br.com.utfpr.gerenciamento.server.repository.projection.ReservaListProjection;
import br.com.utfpr.gerenciamento.server.service.EmailService;
import br.com.utfpr.gerenciamento.server.service.ReservaService;
import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import br.com.utfpr.gerenciamento.server.util.DateUtil;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ReservaServiceImpl extends CrudServiceImpl<Reserva, Long, ReservaResponseDto>
    implements ReservaService {

  private final ReservaRepository reservaRepository;
  private final UsuarioService usuarioService;
  private final EmailService emailService;
  private final ModelMapper modelMapper;
  private final ApplicationEventPublisher eventPublisher;

  public ReservaServiceImpl(
      ReservaRepository reservaRepository,
      UsuarioService usuarioService,
      EmailService emailService,
      ModelMapper modelMapper,
      ApplicationEventPublisher eventPublisher) {
    this.reservaRepository = reservaRepository;
    this.usuarioService = usuarioService;
    this.emailService = emailService;
    this.modelMapper = modelMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected JpaRepository<Reserva, Long> getRepository() {
    return reservaRepository;
  }

  @Override
  protected Map<String, String> getSearchableFieldMappings() {
    return Map.of(
        "id", "id",
        "descricao", "descricao",
        "dataReserva", "dataReserva",
        "dataRetirada", "dataRetirada",
        "usuarioNome", "usuario.nome");
  }

  @Override
  public ReservaResponseDto toDto(Reserva entity) {
    return modelMapper.map(entity, ReservaResponseDto.class);
  }

  @Override
  public Reserva toEntity(ReservaResponseDto reservaResponseDto) {
    return modelMapper.map(reservaResponseDto, Reserva.class);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReservaListDto> findAllPagedList(String filter, Pageable pageable) {
    Page<ReservaListProjection> page;
    if (filter != null && !filter.isBlank()) {
      page = reservaRepository.findAllProjectedWithFilter(filter, pageable);
    } else {
      page = reservaRepository.findAllProjected(pageable);
    }
    return page.map(ReservaListDto::fromProjection);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReservaListDto> findAllPagedListByUser(
      String filter, Pageable pageable, String username) {
    Page<ReservaListProjection> page;
    if (filter != null && !filter.isBlank()) {
      page = reservaRepository.findAllProjectedByUsernameWithFilter(username, filter, pageable);
    } else {
      page = reservaRepository.findAllProjectedByUsername(username, pageable);
    }
    return page.map(ReservaListDto::fromProjection);
  }

  @Override
  @Transactional
  public ReservaResponseDto save(Reserva reserva) {
    String username = SecurityUtils.getAuthenticatedUsername();
    reserva.setUsuario(usuarioService.toEntity(usuarioService.findByUsername(username)));
    ReservaResponseDto reservaResponseDto = super.save(reserva);
    // Publica evento para envio de email APÓS commit da transação
    eventPublisher.publishEvent(
        new ReservaCriadaEvent(this, reserva.getId(), reserva.getUsuario().getEmail()));
    return reservaResponseDto;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReservaResponseDto> findAllByAuthenticatedUser() {
    String username = SecurityUtils.getAuthenticatedUsername();
    Usuario usuario = usuarioService.toEntity(usuarioService.findByUsername(username));
    return reservaRepository.findAllByUsuario(usuario).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReservaResponseDto> findAllByIdItem(Long idItem) {
    return reservaRepository.findReservaByIdItem(idItem).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional
  public void finalizarReserva(Long idReserva) {
    Reserva reserva =
        reservaRepository
            .findById(idReserva)
            .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

    // SEGURANÇA: Validação por ID - evita problemas de case-sensitivity
    // Admins e laboratoristas podem finalizar qualquer reserva (conversão para empréstimo)
    String authenticatedUsername = SecurityUtils.getAuthenticatedUsername();
    List<String> userRoles = SecurityUtils.getAuthenticatedUserRoles();

    // As roles vêm do Spring Security com prefixo "ROLE_"
    boolean isAdminOrLaboratorista =
        userRoles.contains("ROLE_" + ROLE_ADMINISTRADOR_NAME)
            || userRoles.contains("ROLE_" + ROLE_LABORATORISTA_NAME);

    if (!isAdminOrLaboratorista) {
      // Usuários comuns só podem finalizar suas próprias reservas
      Usuario usuarioLogado =
          usuarioService.toEntity(usuarioService.findByUsername(authenticatedUsername));
      if (usuarioLogado == null) {
        log.warn(
            "Tentativa de finalizar reserva com usuário não encontrado: username={}",
            authenticatedUsername);
        throw new AccessDeniedException("Usuário autenticado não encontrado");
      }

      if (!reserva.getUsuario().getId().equals(usuarioLogado.getId())) {
        log.warn(
            "Tentativa de finalizar reserva não autorizada: usuário {} (ID: {}) tentou finalizar reserva ID: {} do usuário {} (ID: {})",
            authenticatedUsername,
            usuarioLogado.getId(),
            idReserva,
            reserva.getUsuario().getUsername(),
            reserva.getUsuario().getId());
        throw new AccessDeniedException("Usuário não tem permissão para finalizar esta reserva");
      }
    }

    emailService.sendEmailWithTemplate(
        converterObjectToTemplateEmail(reserva),
        reserva.getUsuario().getEmail(),
        "Reserva Finalizada",
        "templateFinalizacaoReserva");
    reservaRepository.deleteById(idReserva);
  }

  public ReservaTemplate converterObjectToTemplateEmail(Reserva reserva) {
    ReservaTemplate toReturn = new ReservaTemplate();
    toReturn.setUsuario(reserva.getUsuario().getNome());
    toReturn.setDtReserva(DateUtil.parseLocalDateToString(reserva.getDataReserva()));
    toReturn.setDtRetirada(DateUtil.parseLocalDateToString(reserva.getDataRetirada()));
    toReturn.setReservaItem(reserva.getReservaItem());
    return toReturn;
  }
}

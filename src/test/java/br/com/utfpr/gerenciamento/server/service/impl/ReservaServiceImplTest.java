package br.com.utfpr.gerenciamento.server.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.dto.ReservaResponseDto;
import br.com.utfpr.gerenciamento.server.dto.UsuarioResponseDto;
import br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException;
import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
import br.com.utfpr.gerenciamento.server.service.EmailService;
import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceImplTest {

  @Mock private ReservaRepository reservaRepository;
  @Mock private UsuarioService usuarioService;
  @Mock private EmailService emailService;
  @Mock private ModelMapper modelMapper;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ReservaServiceImpl reservaService;

  private Usuario usuarioDono;
  private Usuario outroUsuario;
  private Reserva reserva;

  @BeforeEach
  void setUp() {
    usuarioDono = new Usuario();
    usuarioDono.setId(1L);
    usuarioDono.setUsername("dono@utfpr.edu.br");
    usuarioDono.setEmail("dono@utfpr.edu.br");
    usuarioDono.setNome("Usuario Dono");

    outroUsuario = new Usuario();
    outroUsuario.setId(2L);
    outroUsuario.setUsername("outro@utfpr.edu.br");
    outroUsuario.setEmail("outro@utfpr.edu.br");
    outroUsuario.setNome("Outro Usuario");

    reserva = new Reserva();
    reserva.setId(1L);
    reserva.setUsuario(usuarioDono);
    reserva.setDataReserva(LocalDate.now());
    reserva.setDataRetirada(LocalDate.now().plusDays(1));
  }

  @Test
  void testGetRepository_DeveRetornarReservaRepository() {
    // When
    var result = reservaService.getRepository();

    // Then
    assertThat(result).isEqualTo(reservaRepository);
  }

  @Test
  void testToDto_DeveConverterReservaParaDTO() {
    // Given
    ReservaResponseDto dto = new ReservaResponseDto();
    when(modelMapper.map(reserva, ReservaResponseDto.class)).thenReturn(dto);

    // When
    ReservaResponseDto result = reservaService.toDto(reserva);

    // Then
    assertThat(result).isEqualTo(dto);
    verify(modelMapper).map(reserva, ReservaResponseDto.class);
  }

  @Test
  void testToEntity_DeveConverterDTOParaReserva() {
    // Given
    ReservaResponseDto dto = new ReservaResponseDto();
    when(modelMapper.map(dto, Reserva.class)).thenReturn(reserva);

    // When
    Reserva result = reservaService.toEntity(dto);

    // Then
    assertThat(result).isEqualTo(reserva);
    verify(modelMapper).map(dto, Reserva.class);
  }

  // ========== Testes para finalizarReserva() ==========

  @Test
  void finalizarReserva_DeveFinalizarQuandoUsuarioEhDonoDaReserva() {
    // Given
    UsuarioResponseDto usuarioDto = new UsuarioResponseDto();
    usuarioDto.setId(1L);

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("dono@utfpr.edu.br");

      when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
      when(usuarioService.findByUsername("dono@utfpr.edu.br")).thenReturn(usuarioDto);
      when(usuarioService.toEntity(usuarioDto)).thenReturn(usuarioDono);
      doNothing()
          .when(emailService)
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
      doNothing().when(reservaRepository).deleteById(1L);

      // When
      reservaService.finalizarReserva(1L);

      // Then
      verify(reservaRepository).findById(1L);
      verify(emailService)
          .sendEmailWithTemplate(
              any(), eq("dono@utfpr.edu.br"), eq("Reserva Finalizada"), anyString());
      verify(reservaRepository).deleteById(1L);
    }
  }

  @Test
  void finalizarReserva_DeveLancarExcecaoQuandoReservaNaoExiste() {
    // Given
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("dono@utfpr.edu.br");

      when(reservaRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      EntityNotFoundException exception =
          assertThrows(EntityNotFoundException.class, () -> reservaService.finalizarReserva(999L));

      // EntityNotFoundException usa ProblemDetail - mensagem está no detail
      assertEquals("Reserva não encontrada.", exception.getBody().getDetail());
      verify(reservaRepository, never()).deleteById(any());
      verify(emailService, never())
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
    }
  }

  @Test
  void finalizarReserva_DeveLancarExcecaoQuandoUsuarioNaoEhDono() {
    // Given: Usuário logado (ID=2) tentando finalizar reserva de outro usuário (ID=1)
    UsuarioResponseDto usuarioDto = new UsuarioResponseDto();
    usuarioDto.setId(2L); // ID diferente do dono da reserva

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("outro@utfpr.edu.br");

      when(reservaRepository.findById(1L))
          .thenReturn(Optional.of(reserva)); // Reserva pertence ao usuário ID=1
      when(usuarioService.findByUsername("outro@utfpr.edu.br")).thenReturn(usuarioDto);
      when(usuarioService.toEntity(usuarioDto)).thenReturn(outroUsuario); // Usuário logado tem ID=2

      // When/Then
      AccessDeniedException exception =
          assertThrows(AccessDeniedException.class, () -> reservaService.finalizarReserva(1L));

      assertEquals("Usuário não tem permissão para finalizar esta reserva", exception.getMessage());
      verify(reservaRepository, never()).deleteById(any());
      verify(emailService, never())
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
    }
  }

  @Test
  void finalizarReserva_DeveLancarExcecaoQuandoUsuarioLogadoNaoEncontrado() {
    // Given
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils
          .when(SecurityUtils::getAuthenticatedUsername)
          .thenReturn("inexistente@utfpr.edu.br");

      when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
      when(usuarioService.findByUsername("inexistente@utfpr.edu.br")).thenReturn(null);
      when(usuarioService.toEntity(null)).thenReturn(null);

      // When/Then
      AccessDeniedException exception =
          assertThrows(AccessDeniedException.class, () -> reservaService.finalizarReserva(1L));

      assertEquals("Usuário autenticado não encontrado", exception.getMessage());
      verify(reservaRepository, never()).deleteById(any());
      verify(emailService, never())
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
    }
  }

  @Test
  void finalizarReserva_DeveValidarPorIdNaoPorUsername() {
    // Given: Cenário de segurança - mesmo username (case diferente) mas IDs diferentes
    Usuario usuarioComMesmoUsername = new Usuario();
    usuarioComMesmoUsername.setId(999L); // ID diferente
    usuarioComMesmoUsername.setUsername("DONO@UTFPR.edu.br"); // Mesmo username, case diferente

    UsuarioResponseDto usuarioDto = new UsuarioResponseDto();
    usuarioDto.setId(999L);

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("DONO@UTFPR.edu.br");

      when(reservaRepository.findById(1L))
          .thenReturn(Optional.of(reserva)); // Reserva do usuário ID=1
      when(usuarioService.findByUsername("DONO@UTFPR.edu.br")).thenReturn(usuarioDto);
      when(usuarioService.toEntity(usuarioDto))
          .thenReturn(usuarioComMesmoUsername); // Usuário ID=999

      // When/Then: Deve bloquear porque IDs são diferentes (1 vs 999)
      AccessDeniedException exception =
          assertThrows(AccessDeniedException.class, () -> reservaService.finalizarReserva(1L));

      assertEquals("Usuário não tem permissão para finalizar esta reserva", exception.getMessage());
      verify(reservaRepository, never()).deleteById(any());
    }
  }

  @Test
  void converterObjectToTemplateEmail_DeveConverterReservaParaTemplate() {
    // Given
    reserva.setDataReserva(LocalDate.of(2025, 12, 8));
    reserva.setDataRetirada(LocalDate.of(2025, 12, 10));

    // When
    var template = reservaService.converterObjectToTemplateEmail(reserva);

    // Then
    assertThat(template).isNotNull();
    assertThat(template.getUsuario()).isEqualTo("Usuario Dono");
  }

  @Test
  void finalizarReserva_AdminPodeFinalizarReservaDeOutroUsuario() {
    // Given: Admin finalizando reserva de outro usuário (conversão para empréstimo)
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("admin@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ADMINISTRADOR"));

      when(reservaRepository.findById(1L))
          .thenReturn(Optional.of(reserva)); // Reserva de usuarioDono
      doNothing()
          .when(emailService)
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
      doNothing().when(reservaRepository).deleteById(1L);

      // When
      reservaService.finalizarReserva(1L);

      // Then: Deve permitir admin finalizar reserva de outro usuário
      verify(reservaRepository).findById(1L);
      verify(emailService)
          .sendEmailWithTemplate(
              any(), eq("dono@utfpr.edu.br"), eq("Reserva Finalizada"), anyString());
      verify(reservaRepository).deleteById(1L);
      // Não deve buscar usuário logado pois admin tem permissão direta
      verify(usuarioService, never()).findByUsername(anyString());
    }
  }

  @Test
  void finalizarReserva_LaboratoristaPodeFinalizarReservaDeOutroUsuario() {
    // Given: Laboratorista finalizando reserva de outro usuário
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("lab@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_LABORATORISTA"));

      when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
      doNothing()
          .when(emailService)
          .sendEmailWithTemplate(any(), anyString(), anyString(), anyString());
      doNothing().when(reservaRepository).deleteById(1L);

      // When
      reservaService.finalizarReserva(1L);

      // Then: Deve permitir laboratorista finalizar reserva de outro usuário
      verify(reservaRepository).deleteById(1L);
      verify(usuarioService, never()).findByUsername(anyString());
    }
  }

  @Test
  void finalizarReserva_AlunoNaoPodeFinalizarReservaDeOutroUsuario() {
    // Given: Aluno tentando finalizar reserva de outro usuário
    UsuarioResponseDto alunoDto = new UsuarioResponseDto();
    alunoDto.setId(99L); // ID diferente do dono

    Usuario aluno = new Usuario();
    aluno.setId(99L);
    aluno.setUsername("aluno@utfpr.edu.br");

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      when(reservaRepository.findById(1L))
          .thenReturn(Optional.of(reserva)); // Reserva de usuarioDono (ID=1)
      when(usuarioService.findByUsername("aluno@utfpr.edu.br")).thenReturn(alunoDto);
      when(usuarioService.toEntity(alunoDto)).thenReturn(aluno); // Aluno tem ID=99

      // When/Then: Deve bloquear aluno de finalizar reserva de outro
      AccessDeniedException exception =
          assertThrows(AccessDeniedException.class, () -> reservaService.finalizarReserva(1L));

      assertEquals("Usuário não tem permissão para finalizar esta reserva", exception.getMessage());
      verify(reservaRepository, never()).deleteById(any());
    }
  }
}

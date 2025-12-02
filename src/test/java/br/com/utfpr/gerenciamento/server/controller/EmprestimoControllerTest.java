package br.com.utfpr.gerenciamento.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.utfpr.gerenciamento.server.dto.EmprestimoListDto;
import br.com.utfpr.gerenciamento.server.dto.EmprestimoResponseDto;
import br.com.utfpr.gerenciamento.server.dto.UsuarioResponseDto;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.EmprestimoItem;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.model.filter.EmprestimoFilter;
import br.com.utfpr.gerenciamento.server.service.EmprestimoService;
import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import br.com.utfpr.gerenciamento.server.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Testes unitários para o EmprestimoController com foco nos filtros de dados por role (ALUNO e
 * PROFESSOR).
 */
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
class EmprestimoControllerTest {

  private MockMvc mockMvc;

  @Mock private EmprestimoService emprestimoService;

  @Mock private UsuarioService usuarioService;

  @InjectMocks private EmprestimoController emprestimoController;

  private EmprestimoFilter emprestimoFilter;
  private Usuario usuarioAluno;
  private Usuario usuarioProfessor;
  private List<EmprestimoResponseDto> emprestimosLista;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(emprestimoController).build();
    emprestimoFilter = new EmprestimoFilter();

    // Setup usuário aluno
    usuarioAluno = new Usuario();
    usuarioAluno.setUsername("aluno@utfpr.edu.br");
    usuarioAluno.setNome("Aluno Teste");

    // Setup usuário professor
    usuarioProfessor = new Usuario();
    usuarioProfessor.setUsername("professor@utfpr.edu.br");
    usuarioProfessor.setNome("Professor Teste");

    // Setup lista de empréstimos mock
    emprestimosLista = List.of(new EmprestimoResponseDto());
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Test
  void findAll_comRoleAluno_deveRetornarApenasEmprestimosDoUsuario() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      when(emprestimoService.findAllEmprestimosAbertosByUsuario("aluno@utfpr.edu.br"))
          .thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.findAll();

      // Assert
      assertEquals(emprestimosLista, resultado);
      verify(emprestimoService).findAllEmprestimosAbertosByUsuario("aluno@utfpr.edu.br");
      verify(emprestimoService, never()).findAllEmprestimosAbertos();
    }
  }

  @Test
  void findAll_comRoleProfessor_deveRetornarApenasEmprestimosDoUsuario() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils
          .when(SecurityUtils::getAuthenticatedUsername)
          .thenReturn("professor@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_PROFESSOR"));

      when(emprestimoService.findAllEmprestimosAbertosByUsuario("professor@utfpr.edu.br"))
          .thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.findAll();

      // Assert
      assertEquals(emprestimosLista, resultado);
      verify(emprestimoService).findAllEmprestimosAbertosByUsuario("professor@utfpr.edu.br");
      verify(emprestimoService, never()).findAllEmprestimosAbertos();
    }
  }

  @Test
  void findAll_comRoleAdministrador_deveRetornarTodosEmprestimos() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("admin@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ADMINISTRADOR"));

      when(emprestimoService.findAllEmprestimosAbertos()).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.findAll();

      // Assert
      assertEquals(emprestimosLista, resultado);
      verify(emprestimoService).findAllEmprestimosAbertos();
      verify(emprestimoService, never()).findAllEmprestimosAbertosByUsuario(anyString());
    }
  }

  @Test
  void findAll_comRoleLaboratorista_deveRetornarTodosEmprestimos() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("lab@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_LABORATORISTA"));

      when(emprestimoService.findAllEmprestimosAbertos()).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.findAll();

      // Assert
      assertEquals(emprestimosLista, resultado);
      verify(emprestimoService).findAllEmprestimosAbertos();
      verify(emprestimoService, never()).findAllEmprestimosAbertosByUsuario(anyString());
    }
  }

  @Test
  void filter_comRoleAlunoESemUsuarioNoFiltro_deveAdicionarUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      when(usuarioService.findByUsername("aluno@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioAluno);

      when(emprestimoService.filter(any(EmprestimoFilter.class))).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioAluno, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("aluno@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleProfessorESemUsuarioNoFiltro_deveAdicionarUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils
          .when(SecurityUtils::getAuthenticatedUsername)
          .thenReturn("professor@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_PROFESSOR"));

      when(usuarioService.findByUsername("professor@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioProfessor);

      when(emprestimoService.filter(any(EmprestimoFilter.class))).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioProfessor, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("professor@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleAlunoEUsuarioNoFiltro_deveSubstituirPeloUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      Usuario usuarioExistente = new Usuario();
      usuarioExistente.setUsername("outro@utfpr.edu.br");
      emprestimoFilter.setUsuarioEmprestimo(usuarioExistente);

      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      when(usuarioService.findByUsername("aluno@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioAluno);

      when(emprestimoService.filter(emprestimoFilter)).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioAluno, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("aluno@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleAlunoEUsuarioDiferenteNoFiltro_deveSubstituirPeloUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      Usuario usuarioDiferente = new Usuario();
      usuarioDiferente.setUsername("outro@utfpr.edu.br");
      emprestimoFilter.setUsuarioEmprestimo(usuarioDiferente);

      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      when(usuarioService.findByUsername("aluno@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioAluno);

      when(emprestimoService.filter(any(EmprestimoFilter.class))).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioAluno, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("aluno@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleProfessorEUsuarioDiferenteNoFiltro_deveSubstituirPeloUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      Usuario usuarioDiferente = new Usuario();
      usuarioDiferente.setUsername("outro@utfpr.edu.br");
      emprestimoFilter.setUsuarioEmprestimo(usuarioDiferente);

      securityUtils
          .when(SecurityUtils::getAuthenticatedUsername)
          .thenReturn("professor@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_PROFESSOR"));

      when(usuarioService.findByUsername("professor@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioProfessor);

      when(emprestimoService.filter(any(EmprestimoFilter.class))).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioProfessor, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("professor@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleAdministrador_deveUsarFiltroOriginal() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("admin@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ADMINISTRADOR"));

      when(emprestimoService.filter(emprestimoFilter)).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertNull(emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService, never()).findByUsername(anyString());
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comRoleLaboratorista_deveUsarFiltroOriginal() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("lab@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_LABORATORISTA"));

      when(emprestimoService.filter(emprestimoFilter)).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertNull(emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService, never()).findByUsername(anyString());
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void filter_comMultiplasRolesIncluindoAluno_deveAdicionarUsuarioAutenticado() {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO", "ROLE_USUARIO"));

      when(usuarioService.findByUsername("aluno@utfpr.edu.br"))
          .thenReturn(new UsuarioResponseDto());
      when(usuarioService.toEntity(any(UsuarioResponseDto.class))).thenReturn(usuarioAluno);

      when(emprestimoService.filter(any(EmprestimoFilter.class))).thenReturn(emprestimosLista);

      // Act
      List<EmprestimoResponseDto> resultado = emprestimoController.filter(emprestimoFilter);

      // Assert
      assertEquals(emprestimosLista, resultado);
      assertEquals(usuarioAluno, emprestimoFilter.getUsuarioEmprestimo());
      verify(usuarioService).findByUsername("aluno@utfpr.edu.br");
      verify(emprestimoService).filter(emprestimoFilter);
    }
  }

  @Test
  void testFindByItemId_DeveRetornarListaVaziaQuandoNenhumEmprestimoEncontrado() throws Exception {
    // Given
    Long itemId = 999L;

    PageRequest pageRequest = PageRequest.of(0, 10);
    Page<EmprestimoResponseDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
    when(emprestimoService.findAllByItemIdPaged(eq(itemId), any(Pageable.class)))
        .thenReturn(emptyPage);

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-by-item/{itemId}", itemId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void testFindByItemId_DeveRetornarListaDeEmprestimos() throws Exception {
    // Given
    Long itemId = 1L;
    EmprestimoResponseDto emprestimoDto = new EmprestimoResponseDto();
    emprestimoDto.setId(1L);

    PageRequest pageRequest = PageRequest.of(0, 10);
    Page<EmprestimoResponseDto> page =
        new PageImpl<>(Collections.singletonList(emprestimoDto), pageRequest, 1);
    when(emprestimoService.findAllByItemIdPaged(eq(itemId), any(Pageable.class))).thenReturn(page);

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-by-item/{itemId}", itemId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void testSave_DeveSalvarEmprestimo() throws Exception {
    // Given
    Emprestimo emprestimo = new Emprestimo();
    emprestimo.setId(1L);
    emprestimo.setDataEmprestimo(LocalDate.now());
    emprestimo.setEmprestimoItem(new HashSet<>());
    emprestimo.getEmprestimoItem().add(new EmprestimoItem());

    EmprestimoResponseDto responseDto = new EmprestimoResponseDto();
    responseDto.setId(1L);

    when(emprestimoService.processEmprestimo(any(Emprestimo.class), eq(0L)))
        .thenReturn(responseDto);

    // When & Then
    mockMvc
        .perform(
            post("/emprestimo/save-emprestimo")
                .param("idReserva", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emprestimo)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1L));

    verify(emprestimoService).processEmprestimo(any(Emprestimo.class), eq(0L));
  }

  @Test
  void testSaveDevolucao_DeveSalvarDevolucao() throws Exception {
    // Given
    Emprestimo emprestimo = new Emprestimo();
    emprestimo.setId(1L);
    emprestimo.setDataEmprestimo(LocalDate.now());
    emprestimo.setEmprestimoItem(new HashSet<>());
    emprestimo.getEmprestimoItem().add(new EmprestimoItem());

    EmprestimoResponseDto responseDto = new EmprestimoResponseDto();
    responseDto.setId(1L);

    when(emprestimoService.processDevolucao(any(Emprestimo.class))).thenReturn(responseDto);

    // When & Then
    mockMvc
        .perform(
            post("/emprestimo/save-devolucao")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emprestimo)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1L));

    verify(emprestimoService).processDevolucao(any(Emprestimo.class));
  }

  @Test
  void testFindAllByUsuarioEmprestimo_DeveRetornarEmprestimosDoUsuario() throws Exception {
    // Given
    String username = "aluno@utfpr.edu.br";
    when(emprestimoService.findAllUsuarioEmprestimo(username)).thenReturn(emprestimosLista);

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-all-by-username/{username}", username)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    verify(emprestimoService).findAllUsuarioEmprestimo(username);
  }

  @Test
  void testChangePrazoDevolucao_DeveAlterarPrazo() throws Exception {
    // Given
    Long id = 1L;
    String novaData = "31/12/2025";

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/change-prazo-devolucao")
                .param("id", id.toString())
                .param("novaData", novaData))
        .andExpect(status().isOk());

    verify(emprestimoService).changePrazoDevolucao(eq(id), any());
  }

  @Test
  void testFindAllPaged_comRoleAluno_deveRetornarEmprestimosDoUsuario() throws Exception {
    try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getAuthenticatedUsername).thenReturn("aluno@utfpr.edu.br");
      mockedSecurity
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ALUNO"));

      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("id").ascending());
      Page<EmprestimoListDto> page = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
      when(emprestimoService.findAllPagedListByUser(null, pageRequest, "aluno@utfpr.edu.br"))
          .thenReturn(page);

      // Act & Assert
      mockMvc
          .perform(
              get("/emprestimo/page")
                  .param("page", "0")
                  .param("size", "10")
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON));

      verify(emprestimoService).findAllPagedListByUser(null, pageRequest, "aluno@utfpr.edu.br");
    }
  }

  @Test
  void testFindAllPaged_comRoleAdministrador_deveRetornarTodosEmprestimos() throws Exception {
    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      // Arrange
      securityUtils.when(SecurityUtils::getAuthenticatedUsername).thenReturn("admin@utfpr.edu.br");
      securityUtils
          .when(SecurityUtils::getAuthenticatedUserRoles)
          .thenReturn(List.of("ROLE_ADMINISTRADOR"));

      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("id").ascending());
      Page<EmprestimoListDto> page = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
      when(emprestimoService.findAllPagedList(null, pageRequest)).thenReturn(page);

      // Act & Assert
      mockMvc
          .perform(
              get("/emprestimo/page")
                  .param("page", "0")
                  .param("size", "10")
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON));

      verify(emprestimoService).findAllPagedList(null, pageRequest);
    }
  }

  @Test
  void testFindByItemId_comItemIdInvalido_deveLancarExcecao() throws Exception {
    // Given
    Long itemId = 0L;

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-by-item/{itemId}", itemId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verify(emprestimoService, never()).findAllByItemIdPaged(anyLong(), any());
  }

  @Test
  void testPreSave_deveChamarPrepareEmprestimo() {
    Emprestimo emprestimo = new Emprestimo();
    emprestimoController.preSave(emprestimo);
    verify(emprestimoService).prepareEmprestimo(emprestimo);
  }

  @Test
  void testPostSave_deveChamarFinalizeEmprestimo() {
    Emprestimo emprestimo = new Emprestimo();
    emprestimoController.postSave(emprestimo);
    verify(emprestimoService).finalizeEmprestimo(emprestimo);
  }

  @Test
  void testPostDelete_deveChamarCleanupAfterDelete() {
    Emprestimo emprestimo = new Emprestimo();
    emprestimoController.postDelete(emprestimo);
    verify(emprestimoService).cleanupAfterDelete(emprestimo);
  }

  @Test
  void testSaveEmprestimo_comIdReserva_deveChamarProcessEmprestimo() throws Exception {
    // Given
    Emprestimo emprestimo = new Emprestimo();
    emprestimo.setId(1L);
    emprestimo.setDataEmprestimo(LocalDate.now());
    emprestimo.setEmprestimoItem(new HashSet<>());
    emprestimo.getEmprestimoItem().add(new EmprestimoItem());

    EmprestimoResponseDto responseDto = new EmprestimoResponseDto();
    responseDto.setId(1L);

    when(emprestimoService.processEmprestimo(any(Emprestimo.class), eq(-1L)))
        .thenReturn(responseDto);

    // When & Then
    mockMvc
        .perform(
            post("/emprestimo/save-emprestimo")
                .param("idReserva", "-1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emprestimo)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1L));

    verify(emprestimoService).processEmprestimo(any(Emprestimo.class), eq(-1L));
  }

  @Test
  void testChangePrazoDevolucao_comDataInvalida_deveLancarExcecao() throws Exception {
    // Given
    long id = 1L;
    String novaData = "data-invalida";

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/change-prazo-devolucao")
                .param("id", String.valueOf(id))
                .param("novaData", novaData))
        .andExpect(status().isBadRequest());

    verify(emprestimoService, never()).changePrazoDevolucao(anyLong(), any());
  }
}

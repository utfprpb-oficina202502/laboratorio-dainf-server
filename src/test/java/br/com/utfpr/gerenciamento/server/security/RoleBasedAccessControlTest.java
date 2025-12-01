package br.com.utfpr.gerenciamento.server.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de controle de acesso baseado em papéis (Role-Based Access Control - RBAC).
 *
 * <p>Valida que diferentes tipos de usuários têm acesso correto aos endpoints: - ADMINISTRADOR:
 * Acesso total a todos os endpoints - LABORATORISTA: Acesso a endpoints de laboratório, NÃO acesso
 * a administração de usuários - PROFESSOR/ALUNO: Acesso apenas a endpoints básicos autenticados -
 * Não autenticado: Acesso apenas a endpoints públicos
 *
 * <p>Baseado na configuração de segurança em WebSecurity.java
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleBasedAccessControlTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String baseUrl;

  // Tokens para usuários existentes nos dados iniciais
  private String adminToken;
  private String laboratoristaToken;
  private String professorToken;
  private String alunoToken;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port;

    // Autenticar apenas usuários existentes nos dados iniciais
    // Usando emails completos conforme migração V3.1
    adminToken = authenticateUser("utfprapps-pb@utfpr.edu.br", "123");
    laboratoristaToken = authenticateUser("joao@alunos.utfpr.edu.br", "123");
    professorToken = authenticateUser("favarim@professores.utfpr.edu.br", "123");
    alunoToken = authenticateUser("gzaffani@alunos.utfpr.edu.br", "123");
  }

  private String authenticateUser(String username, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    Map<String, String> loginRequest =
        Map.of(
            "username", username,
            "password", password);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(loginRequest, headers);

    try {
      ResponseEntity<String> response =
          restTemplate.postForEntity(baseUrl + "/login", request, String.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        String token = response.getBody();
        // Validar formato JWT
        if (token != null && token.split("\\.").length == 3) {
          return token;
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private HttpHeaders createAuthHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    if (token != null) {
      headers.set("Authorization", "Bearer " + token);
    }
    return headers;
  }

  /**
   * Retorna o token para o role especificado.
   *
   * @param role Nome do role (ALUNO, PROFESSOR, LABORATORISTA, ADMINISTRADOR)
   * @return Token JWT para o role
   */
  private String getTokenForRole(String role) {
    return switch (role) {
      case "ALUNO" -> alunoToken;
      case "PROFESSOR" -> professorToken;
      case "LABORATORISTA" -> laboratoristaToken;
      case "ADMINISTRADOR" -> adminToken;
      default -> throw new IllegalArgumentException("Role desconhecido: " + role);
    };
  }

  // ========================================================================
  // ENDPOINTS PÚBLICOS (não requerem autenticação)
  // ========================================================================

  @Test
  @DisplayName("Endpoint público: POST /auth deve ser acessível sem autenticação")
  void endpointPublicoAuth_DeveSerAcessivelSemToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/auth", request, String.class);

    // Deve retornar 401 (não autorizado) mas não 403 (proibido)
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Endpoint público deve aceitar requisições sem token");
  }

  @Test
  @DisplayName("Endpoint público: POST /usuario/new-user deve ser acessível sem autenticação")
  void endpointPublicoNewUser_DeveSerAcessivelSemToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    Map<String, Object> newUser =
        Map.of(
            "nome", "Test User",
            "username", "testuser@test.com",
            "email", "testuser@test.com",
            "password", "123456",
            "documento", "12345678901",
            "telefone", "12345678901");

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(newUser, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/usuario/new-user", request, String.class);

    // Deve aceitar requisição (pode retornar 200, 400, 422, etc.) mas não 403
    assertNotEquals(
        HttpStatus.FORBIDDEN,
        response.getStatusCode(),
        "Endpoint público deve aceitar requisições sem token");
    assertNotEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Endpoint público deve aceitar requisições sem token");
  }

  @Test
  @DisplayName("Endpoint público: POST /login deve ser acessível sem autenticação")
  void endpointPublicoLogin_DeveSerAcessivelSemToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    Map<String, String> loginRequest =
        Map.of(
            "username", "gzaffani@alunos.utfpr.edu.br",
            "password", "123");

    HttpEntity<Map<String, String>> request = new HttpEntity<>(loginRequest, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/login", request, String.class);

    assertEquals(
        HttpStatus.OK, response.getStatusCode(), "Login deve funcionar para usuário válido");
    assertNotNull(response.getBody(), "Token deve ser retornado");
  }

  // ========================================================================
  // ENDPOINTS AUTENTICADOS (requerem qualquer usuário autenticado)
  // ========================================================================

  @Test
  @DisplayName(
      "Endpoint autenticado: GET /usuario/user-info deve ser acessível por qualquer usuário autenticado")
  void endpointAutenticadoUserInfo_DeveSerAcessivelPorQualquerUsuario() {
    assertNotNull(alunoToken, "Token de aluno deve existir para este teste");
    HttpHeaders headers = createAuthHeaders(alunoToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/usuario/user-info", HttpMethod.GET, request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "Aluno deve acessar user-info");
  }

  @Test
  @DisplayName("Endpoint autenticado: GET /usuario/user-info deve ser acessível por laboratorista")
  void endpointAutenticadoUserInfo_DeveSerAcessivelPorLaboratorista() {
    assertNotNull(laboratoristaToken, "Token de laboratorista deve existir para este teste");
    HttpHeaders headers = createAuthHeaders(laboratoristaToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/usuario/user-info", HttpMethod.GET, request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "Laboratorista deve acessar user-info");
  }

  @Test
  @DisplayName("Endpoint autenticado: GET /usuario/user-info deve ser acessível por professor")
  void endpointAutenticadoUserInfo_DeveSerAcessivelPorProfessor() {
    assertNotNull(professorToken, "Professor token deve existir");
    HttpHeaders headers = createAuthHeaders(professorToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/usuario/user-info", HttpMethod.GET, request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "Professor deve acessar user-info");
  }

  @Test
  @DisplayName("Endpoint autenticado: GET /usuario/user-info deve ser acessível por administrador")
  void endpointAutenticadoUserInfo_DeveSerAcessivelPorAdministrador() {
    assertNotNull(adminToken, "Admin token deve existir");
    HttpHeaders headers = createAuthHeaders(adminToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/usuario/user-info", HttpMethod.GET, request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "Administrador deve acessar user-info");
  }

  @Test
  @DisplayName("Endpoint autenticado: POST /usuario/user-info deve bloquear acesso sem token")
  void endpointAutenticadoUserInfo_DeveBloquearAcessoSemToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    // Sem token

    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/usuario/user-info", request, String.class);

    // RFC 7807: Sem token retorna 401 Unauthorized (autenticacao necessaria)
    // 403 Forbidden e usado quando autenticado mas sem permissao
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Endpoint autenticado deve bloquear acesso sem token");
  }

  @Test
  @DisplayName("Endpoint autenticado: POST /usuario/user-info deve bloquear token inválido")
  void endpointAutenticadoUserInfo_DeveBloquearTokenInvalido() {
    HttpHeaders headers = createAuthHeaders("token_invalido_qualquer_coisa");
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/usuario/user-info", request, String.class);

    // RFC 7807: Token invalido retorna 401 Unauthorized (autenticacao falhou)
    // 403 Forbidden e usado quando autenticado mas sem permissao
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Endpoint autenticado deve bloquear token inválido");
  }

  // ========================================================================
  // ENDPOINTS DE ADMINISTRAÇÃO (requerem papel ADMINISTRADOR)
  // ========================================================================

  @Test
  @DisplayName("Endpoint administrador: GET /usuario deve ser acessível apenas por ADMINISTRADOR")
  void endpointAdminUsuario_DeveSerAcessivelApenasPorAdmin() {
    assertNotNull(adminToken, "Admin token deve existir");
    HttpHeaders headers = createAuthHeaders(adminToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/usuario", HttpMethod.GET, request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "Admin deve acessar lista de usuários");
  }

  @Test
  @DisplayName("Endpoint administrador: GET /usuario deve bloquear ALUNO")
  void endpointAdminUsuario_DeveBloquearAluno() {
    assertNotNull(alunoToken, "Aluno token deve existir");
    HttpHeaders headers = createAuthHeaders(alunoToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/usuario", HttpMethod.GET, request, String.class);

    assertEquals(
        HttpStatus.FORBIDDEN, response.getStatusCode(), "Aluno não deve acessar lista de usuários");
  }

  @Test
  @DisplayName("Endpoint administrador: GET /usuario deve bloquear LABORATORISTA")
  void endpointAdminUsuario_DeveBloquearLaboratorista() {
    assertNotNull(laboratoristaToken, "Laboratorista token deve existir");
    HttpHeaders headers = createAuthHeaders(laboratoristaToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/usuario", HttpMethod.GET, request, String.class);

    assertEquals(
        HttpStatus.FORBIDDEN,
        response.getStatusCode(),
        "Laboratorista não deve acessar lista de usuários");
  }

  @Test
  @DisplayName("Endpoint administrador: GET /usuario deve bloquear PROFESSOR")
  void endpointAdminUsuario_DeveBloquearProfessor() {
    assertNotNull(professorToken, "Professor token deve existir");
    HttpHeaders headers = createAuthHeaders(professorToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/usuario", HttpMethod.GET, request, String.class);

    // Professor não deve acessar lista de usuários (apenas ROLE_ALUNO e ROLE_PROFESSOR)
    assertEquals(
        HttpStatus.FORBIDDEN,
        response.getStatusCode(),
        "Professor não deve acessar lista de usuários - apenas ROLE_ALUNO e ROLE_PROFESSOR");
  }

  // ========================================================================
  // ENDPOINTS ESPECÍFICOS (testar outros endpoints relevantes)
  // ========================================================================

  @Test
  @DisplayName("Endpoint específico: GET /item deve ser acessível por qualquer usuário autenticado")
  void endpointItem_DeveSerAcessivelPorQualquerUsuarioAutenticado() {
    assertNotNull(alunoToken, "Aluno token deve existir");
    HttpHeaders headers = createAuthHeaders(alunoToken);
    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/item", HttpMethod.GET, request, String.class);

    assertNotEquals(
        HttpStatus.FORBIDDEN, response.getStatusCode(), "Aluno deve acessar lista de itens");
    assertNotEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Aluno autenticado não deve receber 401");
  }

  @Test
  @DisplayName("Endpoint específico: GET /item deve bloquear acesso sem autenticação")
  void endpointItem_DeveBloquearAcessoSemAutenticacao() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    HttpEntity<String> request = new HttpEntity<>("", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/item", HttpMethod.GET, request, String.class);

    // RFC 7807: Sem autenticacao retorna 401 Unauthorized
    // 403 Forbidden e usado quando autenticado mas sem permissao
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Endpoint de itens deve bloquear acesso sem autenticação");
  }

  // ========================================================================
  // VALIDAÇÕES DE INTEGRIDADE DOS TOKENS
  // ========================================================================

  @Test
  @DisplayName("Tokens de usuários diferentes devem ser diferentes")
  void tokensDeUsuariosDiferentes_DevemSerDiferentes() {
    assertNotNull(alunoToken, "Token de aluno não deve ser nulo");
    assertNotNull(laboratoristaToken, "Token de laboratorista não deve ser nulo");
    assertNotNull(professorToken, "Token de professor não deve ser nulo");
    assertNotNull(adminToken, "Token de administrador não deve ser nulo");

    // Tokens devem ser diferentes entre usuários
    assertNotEquals(
        alunoToken, laboratoristaToken, "Tokens de usuários diferentes devem ser diferentes");
    assertNotEquals(
        alunoToken, professorToken, "Tokens de usuários diferentes devem ser diferentes");
    assertNotEquals(alunoToken, adminToken, "Tokens de usuários diferentes devem ser diferentes");
  }

  @Test
  @DisplayName("Tokens devem ter formato JWT válido")
  void tokens_DevemTerFormatoJWT() {
    // Verificar que tokens não são nulos
    assertNotNull(adminToken, "Token admin não deve ser nulo");
    assertNotNull(laboratoristaToken, "Token laboratorista não deve ser nulo");
    assertNotNull(professorToken, "Token professor não deve ser nulo");
    assertNotNull(alunoToken, "Token aluno não deve ser nulo");

    // Todos devem ter formato JWT válido (3 partes separadas por pontos)
    assertEquals(3, adminToken.split("\\.").length, "Token admin deve ter formato JWT");
    assertEquals(
        3, laboratoristaToken.split("\\.").length, "Token laboratorista deve ter formato JWT");
    assertEquals(3, professorToken.split("\\.").length, "Token professor deve ter formato JWT");
    assertEquals(3, alunoToken.split("\\.").length, "Token aluno deve ter formato JWT");
  }

  // ========================================================================
  // TESTES PARAMETRIZADOS - Acesso Bloqueado (deve retornar 403)
  // ========================================================================

  /**
   * Fornece argumentos para testes de acesso bloqueado. Formato: role, metodo HTTP, endpoint,
   * descricao
   */
  static Stream<Arguments> acessoBloqueadoProvider() {
    return Stream.of(
        // RESERVA - PUT/DELETE bloqueado para ALUNO e PROFESSOR
        Arguments.of("ALUNO", "PUT", "/reserva/1", "Aluno nao deve editar reservas"),
        Arguments.of("PROFESSOR", "PUT", "/reserva/1", "Professor nao deve editar reservas"),
        Arguments.of("ALUNO", "DELETE", "/reserva/1", "Aluno nao deve excluir reservas"),
        Arguments.of("PROFESSOR", "DELETE", "/reserva/1", "Professor nao deve excluir reservas"),

        // SOLICITACAO DE COMPRA - POST/PUT/DELETE bloqueado para ALUNO e PROFESSOR
        Arguments.of(
            "ALUNO", "POST", "/solicitacao-compra", "Aluno nao deve criar solicitacoes de compra"),
        Arguments.of(
            "PROFESSOR",
            "POST",
            "/solicitacao-compra",
            "Professor nao deve criar solicitacoes de compra"),
        Arguments.of(
            "ALUNO",
            "PUT",
            "/solicitacao-compra/1",
            "Aluno nao deve editar solicitacoes de compra"),
        Arguments.of(
            "PROFESSOR",
            "PUT",
            "/solicitacao-compra/1",
            "Professor nao deve editar solicitacoes de compra"),
        Arguments.of(
            "ALUNO",
            "DELETE",
            "/solicitacao-compra/1",
            "Aluno nao deve excluir solicitacoes de compra"),
        Arguments.of(
            "PROFESSOR",
            "DELETE",
            "/solicitacao-compra/1",
            "Professor nao deve excluir solicitacoes de compra"),

        // NADA CONSTA - Todos os endpoints bloqueados para ALUNO e PROFESSOR
        Arguments.of("ALUNO", "GET", "/nadaconsta", "Aluno nao deve acessar nada consta"),
        Arguments.of("PROFESSOR", "GET", "/nadaconsta", "Professor nao deve acessar nada consta"),
        Arguments.of(
            "ALUNO", "POST", "/nadaconsta/solicitar", "Aluno nao deve solicitar nada consta"),
        Arguments.of(
            "PROFESSOR", "POST", "/nadaconsta/solicitar", "Professor nao deve solicitar nada consta"),
        Arguments.of(
            "ALUNO",
            "PUT",
            "/nadaconsta/verificar-pendencias/1",
            "Aluno nao deve verificar pendencias"),
        Arguments.of(
            "PROFESSOR", "PUT", "/nadaconsta/invalidar/1", "Professor nao deve invalidar nada consta"));
  }

  @ParameterizedTest(name = "{3}")
  @MethodSource("acessoBloqueadoProvider")
  @DisplayName("Acesso bloqueado")
  void deveBloquearAcesso(String role, String metodo, String endpoint, String descricao) {
    String token = getTokenForRole(role);
    assertNotNull(token, "Token de " + role + " deve existir");

    HttpHeaders headers = createAuthHeaders(token);
    String body = metodo.equals("GET") || metodo.equals("DELETE") ? "" : "{}";
    HttpEntity<String> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + endpoint, HttpMethod.valueOf(metodo), request, String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), descricao);
  }

  // ========================================================================
  // TESTES PARAMETRIZADOS - Acesso Permitido (nao deve retornar 403)
  // ========================================================================

  /** Fornece argumentos para testes de acesso permitido. */
  static Stream<Arguments> acessoPermitidoProvider() {
    return Stream.of(
        // RESERVA - POST permitido para todos, PUT/DELETE para ADMIN/LAB
        Arguments.of("ALUNO", "POST", "/reserva", "Aluno deve poder criar reservas"),
        Arguments.of("PROFESSOR", "POST", "/reserva", "Professor deve poder criar reservas"),
        Arguments.of("LABORATORISTA", "PUT", "/reserva/1", "Laboratorista deve poder editar reservas"),
        Arguments.of("ADMINISTRADOR", "PUT", "/reserva/1", "Administrador deve poder editar reservas"),

        // SOLICITACAO DE COMPRA - GET para todos, POST/PUT/DELETE para ADMIN/LAB
        Arguments.of(
            "ALUNO", "GET", "/solicitacao-compra", "Aluno deve poder visualizar solicitacoes"),
        Arguments.of(
            "LABORATORISTA",
            "POST",
            "/solicitacao-compra",
            "Laboratorista deve poder criar solicitacoes"),
        Arguments.of(
            "ADMINISTRADOR",
            "POST",
            "/solicitacao-compra",
            "Administrador deve poder criar solicitacoes"),

        // NADA CONSTA - Todos endpoints para ADMIN/LAB
        Arguments.of("LABORATORISTA", "GET", "/nadaconsta", "Laboratorista deve acessar nada consta"),
        Arguments.of("ADMINISTRADOR", "GET", "/nadaconsta", "Administrador deve acessar nada consta"));
  }

  @ParameterizedTest(name = "{3}")
  @MethodSource("acessoPermitidoProvider")
  @DisplayName("Acesso permitido")
  void devePermitirAcesso(String role, String metodo, String endpoint, String descricao) {
    String token = getTokenForRole(role);
    assertNotNull(token, "Token de " + role + " deve existir");

    HttpHeaders headers = createAuthHeaders(token);
    String body = metodo.equals("GET") || metodo.equals("DELETE") ? "" : "{}";
    HttpEntity<String> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + endpoint, HttpMethod.valueOf(metodo), request, String.class);

    // Nao deve retornar 403 (pode retornar 400/404/422 por dados invalidos, mas nao 403)
    assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), descricao);
  }

  // ========================================================================
  // VALIDACAO DE ISOLAMENTO DE TESTE
  // ========================================================================

}

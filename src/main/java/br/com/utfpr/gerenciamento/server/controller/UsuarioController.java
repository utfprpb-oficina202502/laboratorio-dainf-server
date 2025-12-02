package br.com.utfpr.gerenciamento.server.controller;

import static br.com.utfpr.gerenciamento.server.enumeration.UserRole.ROLE_ADMINISTRADOR_NAME;

import br.com.utfpr.gerenciamento.server.dto.ConfirmEmailRequestDto;
import br.com.utfpr.gerenciamento.server.dto.GenericResponse;
import br.com.utfpr.gerenciamento.server.dto.PermissaoResponseDTO;
import br.com.utfpr.gerenciamento.server.dto.RecoverPasswordRequestDto;
import br.com.utfpr.gerenciamento.server.dto.UsuarioListDto;
import br.com.utfpr.gerenciamento.server.dto.UsuarioResponseDto;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.service.PermissaoService;
import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("usuario")
@Validated
public class UsuarioController {

  private static final String DEFAULT_SORT_PROPERTY = "id";
  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "nome", "email", "username", "documento", "ativo");

  private final UsuarioService usuarioService;

  private final PermissaoService permissaoService;

  public UsuarioController(UsuarioService usuarioService, PermissaoService permissaoService) {
    this.usuarioService = usuarioService;
    this.permissaoService = permissaoService;
  }

  @GetMapping
  public List<UsuarioResponseDto> findAll() {
    return usuarioService.findAll();
  }

  /**
   * Busca usuário por ID.
   *
   * <p>Usuário pode ver seu próprio perfil. Administradores podem ver qualquer perfil.
   *
   * @param id ID do usuário
   * @return DTO do usuário
   */
  @GetMapping("{id}")
  @PostAuthorize(
      "returnObject.username == authentication.name || hasRole('" + ROLE_ADMINISTRADOR_NAME + "')")
  public UsuarioResponseDto findOne(@PathVariable("id") Long id) {
    return usuarioService.findOne(id);
  }

  /**
   * Lista paginada de usuários com filtro textual usando DTO simplificado.
   *
   * <p><b>Otimização:</b> Retorna apenas campos necessários para listagem via projeção SQL.
   *
   * @param page Número da página (0-indexed, mínimo: 0)
   * @param size Tamanho da página (mínimo: 1, máximo: 100)
   * @param filter Filtro opcional (busca textual em todos os campos)
   * @param sort Ordenacao no formato "campo,direcao" (ex: "nome,desc")
   * @return Página de usuários simplificados
   */
  @GetMapping("page")
  public Page<UsuarioListDto> findAllPagedList(
      @RequestParam("page") @Min(0) int page,
      @RequestParam("size") @Min(1) @Max(100) int size,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String sort) {
    Sort sortObj = parseSortParameter(sort);
    PageRequest pageRequest = PageRequest.of(page, size, sortObj);
    return usuarioService.findAllPagedList(filter, pageRequest);
  }

  private Sort parseSortParameter(String sort) {
    if (sort == null || sort.isBlank()) {
      return Sort.by(DEFAULT_SORT_PROPERTY);
    }
    String[] parts = sort.split(",");
    String property = parts[0].trim();

    // Valida propriedade contra whitelist
    if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
      return Sort.by(DEFAULT_SORT_PROPERTY);
    }

    Sort.Direction direction =
        (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    return Sort.by(direction, property);
  }

  @PostMapping
  public UsuarioResponseDto save(@RequestBody @Valid Usuario usuario) {
    return usuarioService.save(usuario);
  }

  @GetMapping("permissao")
  public List<PermissaoResponseDTO> findAllPermissao() {
    return permissaoService.findAll();
  }

  @PostMapping("change-senha")
  public UsuarioResponseDto redefinirSenha(
      @RequestBody @Valid Usuario usuario, @RequestParam("senhaAtual") String senhaAtual) {
    return usuarioService.updatePassword(usuario, senhaAtual);
  }

  @DeleteMapping("{id}")
  public void delete(@PathVariable("id") Long id) {
    usuarioService.delete(id);
  }

  @GetMapping("/complete")
  public Page<UsuarioResponseDto> complete(
      @RequestParam("query") String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return usuarioService.usuarioComplete(query, pageable);
  }

  @GetMapping("/complete-custom")
  public Page<UsuarioResponseDto> completeByUserOrDocOrNome(
      @RequestParam("query") String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return usuarioService.usuarioCompleteByUserAndDocAndNome(query, pageable);
  }

  @GetMapping("/complete-users-lab")
  public Page<UsuarioResponseDto> completeUserLabs(
      @RequestParam("query") String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return usuarioService.usuarioCompleteLab(query, pageable);
  }

  @GetMapping("/find-by-username")
  public UsuarioResponseDto findByUsername(@RequestParam("username") String username) {
    return usuarioService.findByUsername(username);
  }

  @GetMapping("/user-info")
  public Principal principal(Principal principal) {
    return principal;
  }

  @PostMapping("/update-user")
  public void atualizarUsuario(@RequestBody @Valid Usuario usuario) {
    usuarioService.updateUsuario(usuario);
  }

  @PostMapping(path = "resend-confirm-email")
  public ResponseEntity<GenericResponse> resendEmail(
      @RequestBody @Valid ConfirmEmailRequestDto confirmEmailRequestDto) {
    return ResponseEntity.ok(
        GenericResponse.builder()
            .message(usuarioService.resendEmail(confirmEmailRequestDto))
            .build());
  }

  @Value("${utfpr.front.url}")
  private String frontBaseUrl;

  @PostMapping("new-user")
  public UsuarioResponseDto saveNewUser(@RequestBody @Valid Usuario usuario) {
    return usuarioService.saveNewUser(usuario);
  }

  @PostMapping(path = "confirm-email")
  public ResponseEntity<GenericResponse> confirmEmail(
      @RequestBody @Valid ConfirmEmailRequestDto confirmEmailRequestDto) {
    return ResponseEntity.ok(usuarioService.confirmEmail(confirmEmailRequestDto));
  }

  @PostMapping(path = "request-code-reset-password")
  public ResponseEntity<GenericResponse> sendEmailCodeRecoverPassword(
      @RequestBody @Valid ConfirmEmailRequestDto confirmEmailRequestDto) {
    return ResponseEntity.ok(
        usuarioService.sendEmailCodeRecoverPassword(confirmEmailRequestDto.getEmail()));
  }

  @PostMapping(path = "reset-password")
  public ResponseEntity<GenericResponse> resetPassword(
      @RequestBody @Valid RecoverPasswordRequestDto recoverPasswordRequestDto) {
    return ResponseEntity.ok(usuarioService.resetPassword(recoverPasswordRequestDto));
  }
}

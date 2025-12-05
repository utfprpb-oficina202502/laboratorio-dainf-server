package br.com.utfpr.gerenciamento.server.repository.projection;

import java.util.Set;

/**
 * Projeção JPA para listagem paginada de Usuários.
 *
 * <p>Esta interface otimiza o endpoint /usuario/page incluindo apenas campos essenciais para
 * exibição em tabelas.
 *
 * <p><b>Campos incluídos:</b>
 *
 * <ul>
 *   <li>id - Identificador único do usuário
 *   <li>nome - Nome completo do usuário
 *   <li>email - Email do usuário (identificador principal de login)
 *   <li>permissoes - Grupos de acesso (apenas id e nome)
 * </ul>
 */
public interface UsuarioListProjection {

  /** Identificador único do usuário. */
  Long getId();

  /** Nome completo do usuário. */
  String getNome();

  /** Email do usuário (identificador principal de login). */
  String getEmail();

  /** Permissões/grupos de acesso do usuário. */
  Set<PermissaoProjection> getPermissoes();

  /** Projeção aninhada para permissões. */
  interface PermissaoProjection {
    Long getId();

    String getNome();
  }
}

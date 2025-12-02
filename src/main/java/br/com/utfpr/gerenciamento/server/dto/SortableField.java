package br.com.utfpr.gerenciamento.server.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo do DTO como ordenável e define o mapeamento para o path da entidade JPA.
 *
 * <p>Quando o nome do campo no DTO difere do path na entidade (ex: campos aninhados), use o
 * atributo {@code entityPath} para especificar o caminho correto.
 *
 * <p>Exemplos de uso:
 *
 * <pre>
 * // Campo simples - nome do DTO == nome da entidade
 * &#64;SortableField
 * private String nome;
 *
 * // Campo aninhado - precisa especificar o path
 * &#64;SortableField(entityPath = "usuario.nome")
 * private String usuarioNome;
 *
 * // Campo não ordenável - não anota
 * private String observacao;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SortableField {

  /**
   * Path do campo na entidade JPA para ordenação.
   *
   * <p>Se vazio, assume que o nome do campo no DTO é igual ao nome na entidade. Para campos
   * aninhados (JOINs), especifique o path completo (ex: "usuario.nome", "fornecedor.razaoSocial").
   *
   * @return path da entidade para ordenação
   */
  String entityPath() default "";
}

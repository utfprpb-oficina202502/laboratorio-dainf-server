package br.com.utfpr.gerenciamento.server.repository.projection;

/**
 * Projeção simplificada de Item para listagens que precisam apenas de id e nome.
 *
 * <p>Usada para otimizar consultas onde não é necessário carregar todos os campos do Item.
 */
public interface ItemSimpleProjection {

  Long getId();

  String getNome();
}

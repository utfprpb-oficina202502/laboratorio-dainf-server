package br.com.utfpr.gerenciamento.server.dto;

/**
 * Interface marcadora para DTOs de listagem simplificados.
 *
 * <p>Todos os DTOs usados em endpoints de paginacao (/page) devem implementar esta interface. Isso
 * permite tipagem segura no CrudController sem necessidade de casting.
 */
public interface BaseListDto {}

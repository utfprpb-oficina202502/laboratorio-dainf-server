package br.com.utfpr.gerenciamento.server.dto.dashboards;

import java.math.BigDecimal;

/**
 * DTO de resposta para itens mais emprestados pelo usuario.
 *
 * @param itemId ID do item
 * @param itemNome Nome do item
 * @param qtde Quantidade de vezes que o usuario emprestou este item
 * @param saldo Saldo disponivel atual do item
 */
public record ItemFrequenteUsuarioDto(Long itemId, String itemNome, Long qtde, BigDecimal saldo) {}

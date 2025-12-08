package br.com.utfpr.gerenciamento.server.specification;

import br.com.utfpr.gerenciamento.server.model.Solicitacao;
import br.com.utfpr.gerenciamento.server.model.SolicitacaoItem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas de Solicitacao usando Criteria API.
 *
 * <p>Usadas principalmente para relatórios de solicitações de compra.
 */
public class SolicitacaoSpecifications {

  private SolicitacaoSpecifications() {}

  /**
   * Filtra solicitações que contêm um item específico.
   *
   * <p>Usado no relatório SolicitacaoItem.
   *
   * @param itemId ID do item
   * @return Specification que filtra solicitações pelo item
   */
  public static Specification<Solicitacao> byItemId(Long itemId) {
    return (root, query, cb) -> {
      if (itemId == null) {
        return cb.conjunction();
      }

      // Previne duplicação de resultados
      if (Objects.requireNonNull(query).getResultType() != Long.class
          && query.getResultType() != long.class) {
        query.distinct(true);
      }

      Join<Solicitacao, SolicitacaoItem> solicitacaoItemJoin =
          root.join("solicitacaoItem", JoinType.LEFT);
      return cb.equal(solicitacaoItemJoin.get("item").get("id"), itemId);
    };
  }

  /**
   * Adiciona fetch joins para usuário e itens da solicitação, evitando N+1.
   *
   * @return Specification com fetch joins
   */
  public static Specification<Solicitacao> withFetchJoins() {
    return (root, query, cb) -> {
      if (Objects.requireNonNull(query).getResultType() != Long.class
          && query.getResultType() != long.class) {
        query.distinct(true);
        root.fetch("usuario", JoinType.LEFT);
        root.fetch("solicitacaoItem", JoinType.LEFT).fetch("item", JoinType.LEFT);
      }
      return cb.conjunction();
    };
  }

  /**
   * Combina filtro por item com fetch joins para relatório.
   *
   * @param itemId ID do item
   * @return Specification completa para relatório SolicitacaoItem
   */
  public static Specification<Solicitacao> forRelatorioSolicitacaoItem(Long itemId) {
    return byItemId(itemId).and(withFetchJoins());
  }
}

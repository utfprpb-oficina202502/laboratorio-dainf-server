package br.com.utfpr.gerenciamento.server.specification;

import br.com.utfpr.gerenciamento.server.model.Item;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas de Item usando Criteria API.
 *
 * <p>Usadas principalmente para relatórios de estoque.
 */
public class ItemSpecifications {

  private ItemSpecifications() {}

  /**
   * Filtra itens com saldo igual a zero.
   *
   * <p>Usado no relatório ItensSemEstoque.
   *
   * @return Specification que filtra WHERE saldo = 0
   */
  public static Specification<Item> bySaldoZero() {
    return (root, query, cb) -> cb.equal(root.get("saldo"), BigDecimal.ZERO);
  }

  /**
   * Filtra itens onde saldo é menor ou igual à quantidade mínima.
   *
   * <p>Usado no relatório ItensAtingiramQtdeMinima.
   *
   * @return Specification que filtra WHERE saldo <= qtdeMinima
   */
  public static Specification<Item> bySaldoMenorOuIgualQtdeMinima() {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("saldo"), root.get("qtdeMinima"));
  }

  /**
   * Adiciona fetch join para grupo do item, evitando N+1.
   *
   * @return Specification com fetch join para grupo
   */
  public static Specification<Item> withGrupoFetch() {
    return (root, query, cb) -> {
      if (Objects.requireNonNull(query).getResultType() != Long.class
          && query.getResultType() != long.class) {
        root.fetch("grupo", JoinType.LEFT);
      }
      return cb.conjunction();
    };
  }

  /**
   * Combina filtro de saldo zero com fetch de grupo.
   *
   * @return Specification para relatório ItensSemEstoque
   */
  public static Specification<Item> forRelatorioItensSemEstoque() {
    return bySaldoZero().and(withGrupoFetch());
  }

  /**
   * Combina filtro de quantidade mínima com fetch de grupo.
   *
   * @return Specification para relatório ItensAtingiramQtdeMinima
   */
  public static Specification<Item> forRelatorioItensQtdeMinima() {
    return bySaldoMenorOuIgualQtdeMinima().and(withGrupoFetch());
  }
}

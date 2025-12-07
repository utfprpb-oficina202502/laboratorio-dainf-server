package br.com.utfpr.gerenciamento.server.specification;

import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.ReservaItem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas de Reserva usando Criteria API.
 *
 * <p>Usadas principalmente para relatórios de reservas.
 */
public class ReservaSpecifications {

  private ReservaSpecifications() {}

  /**
   * Filtra reservas que contêm um item específico.
   *
   * <p>Usado no relatório ReservaDoItem.
   *
   * @param itemId ID do item
   * @return Specification que filtra reservas pelo item
   */
  public static Specification<Reserva> byItemId(Long itemId) {
    return (root, query, cb) -> {
      if (itemId == null) {
        return cb.conjunction();
      }

      // Previne duplicação de resultados
      if (Objects.requireNonNull(query).getResultType() != Long.class
          && query.getResultType() != long.class) {
        query.distinct(true);
      }

      Join<Reserva, ReservaItem> reservaItemJoin = root.join("reservaItem", JoinType.LEFT);
      return cb.equal(reservaItemJoin.get("item").get("id"), itemId);
    };
  }

  /**
   * Adiciona fetch joins para usuário e itens da reserva, evitando N+1.
   *
   * @return Specification com fetch joins
   */
  public static Specification<Reserva> withFetchJoins() {
    return (root, query, cb) -> {
      if (Objects.requireNonNull(query).getResultType() != Long.class
          && query.getResultType() != long.class) {
        query.distinct(true);
        root.fetch("usuario", JoinType.LEFT);
        root.fetch("reservaItem", JoinType.LEFT).fetch("item", JoinType.LEFT);
      }
      return cb.conjunction();
    };
  }

  /**
   * Combina filtro por item com fetch joins para relatório.
   *
   * @param itemId ID do item
   * @return Specification completa para relatório ReservaDoItem
   */
  public static Specification<Reserva> forRelatorioReservaDoItem(Long itemId) {
    return byItemId(itemId).and(withFetchJoins());
  }
}

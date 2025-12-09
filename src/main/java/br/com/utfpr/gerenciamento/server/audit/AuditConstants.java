package br.com.utfpr.gerenciamento.server.audit;

import br.com.utfpr.gerenciamento.server.model.*;
import java.util.Map;

/**
 * Constantes centralizadas para o módulo de auditoria.
 *
 * <p>Contém o mapeamento de entidades auditáveis e seus labels em pt-BR. Centraliza essas
 * definições para evitar duplicação entre Controller e Service.
 *
 * @author Rodrigo Izidoro
 */
public final class AuditConstants {

  /** Limite máximo de registros por página. */
  public static final int MAX_PAGE_SIZE = 100;

  /** Tamanho padrão de página. */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /** Limite máximo de registros por entidade na timeline. */
  public static final int MAX_TIMELINE_RESULTS_PER_ENTITY = 500;

  /** Período padrão da timeline em dias (quando nenhum filtro de data é informado). */
  public static final int DEFAULT_TIMELINE_DAYS = 30;

  /** Mapa de entidades auditáveis: chave técnica -> classe. */
  public static final Map<String, Class<?>> ENTITY_MAP =
      Map.ofEntries(
          Map.entry("emprestimo", Emprestimo.class),
          Map.entry("emprestimo-item", EmprestimoItem.class),
          Map.entry("emprestimo-devolucao-item", EmprestimoDevolucaoItem.class),
          Map.entry("item", Item.class),
          Map.entry("item-image", ItemImage.class),
          Map.entry("usuario", Usuario.class),
          Map.entry("saida", Saida.class),
          Map.entry("saida-item", SaidaItem.class),
          Map.entry("reserva", Reserva.class),
          Map.entry("reserva-item", ReservaItem.class),
          Map.entry("compra", Compra.class),
          Map.entry("compra-item", CompraItem.class),
          Map.entry("solicitacao", Solicitacao.class),
          Map.entry("solicitacao-item", SolicitacaoItem.class),
          Map.entry("grupo", Grupo.class),
          Map.entry("fornecedor", Fornecedor.class),
          Map.entry("nada-consta", NadaConsta.class));

  /** Mapa de labels em pt-BR para as entidades. */
  public static final Map<String, String> ENTITY_LABELS =
      Map.ofEntries(
          Map.entry("emprestimo", "Empréstimo"),
          Map.entry("emprestimo-item", "Item de Empréstimo"),
          Map.entry("emprestimo-devolucao-item", "Devolução de Empréstimo"),
          Map.entry("item", "Item"),
          Map.entry("item-image", "Imagem de Item"),
          Map.entry("usuario", "Usuário"),
          Map.entry("saida", "Saída"),
          Map.entry("saida-item", "Item de Saída"),
          Map.entry("reserva", "Reserva"),
          Map.entry("reserva-item", "Item de Reserva"),
          Map.entry("compra", "Compra"),
          Map.entry("compra-item", "Item de Compra"),
          Map.entry("solicitacao", "Solicitação de Compra"),
          Map.entry("solicitacao-item", "Item de Solicitação"),
          Map.entry("grupo", "Grupo"),
          Map.entry("fornecedor", "Fornecedor"),
          Map.entry("nada-consta", "Nada Consta"));

  private AuditConstants() {
    // Classe utilitária - não instanciar
  }
}

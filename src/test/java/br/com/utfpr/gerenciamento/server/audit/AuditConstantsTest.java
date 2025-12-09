package br.com.utfpr.gerenciamento.server.audit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.utfpr.gerenciamento.server.model.*;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários para AuditConstants.
 *
 * <p>Valida a consistência dos mapas de entidades e labels.
 */
@DisplayName("AuditConstants")
class AuditConstantsTest {

  @Nested
  @DisplayName("ENTITY_MAP")
  class EntityMapTests {

    @Test
    @DisplayName("Deve conter todas as 17 entidades auditadas")
    void deveConterTodasAs17EntidadesAuditadas() {
      assertThat(AuditConstants.ENTITY_MAP).hasSize(17);
    }

    @Test
    @DisplayName("Deve mapear chaves corretas para classes de entidades")
    void deveMapearChavesCorretasParaClassesDeEntidades() {
      assertThat(AuditConstants.ENTITY_MAP)
          .containsEntry("emprestimo", Emprestimo.class)
          .containsEntry("emprestimo-item", EmprestimoItem.class)
          .containsEntry("emprestimo-devolucao-item", EmprestimoDevolucaoItem.class)
          .containsEntry("item", Item.class)
          .containsEntry("item-image", ItemImage.class)
          .containsEntry("usuario", Usuario.class)
          .containsEntry("saida", Saida.class)
          .containsEntry("saida-item", SaidaItem.class)
          .containsEntry("reserva", Reserva.class)
          .containsEntry("reserva-item", ReservaItem.class)
          .containsEntry("compra", Compra.class)
          .containsEntry("compra-item", CompraItem.class)
          .containsEntry("solicitacao", Solicitacao.class)
          .containsEntry("solicitacao-item", SolicitacaoItem.class)
          .containsEntry("grupo", Grupo.class)
          .containsEntry("fornecedor", Fornecedor.class)
          .containsEntry("nada-consta", NadaConsta.class);
    }

    @Test
    @DisplayName("Todas as entidades devem ter anotação @Audited")
    void todasAsEntidadesDevemTerAnotacaoAudited() {
      for (Class<?> entityClass : AuditConstants.ENTITY_MAP.values()) {
        assertThat(entityClass.isAnnotationPresent(Audited.class))
            .as("Entidade %s deve ter @Audited", entityClass.getSimpleName())
            .isTrue();
      }
    }

    @Test
    @DisplayName("Chaves devem seguir padrão lowercase com hífen")
    void chavesDevemSeguirPadraoLowercaseComHifen() {
      for (String key : AuditConstants.ENTITY_MAP.keySet()) {
        assertThat(key).matches("^[a-z]+(-[a-z]+)*$");
      }
    }
  }

  @Nested
  @DisplayName("ENTITY_LABELS")
  class EntityLabelsTests {

    @Test
    @DisplayName("Deve ter o mesmo tamanho que ENTITY_MAP")
    void deveTerOMesmoTamanhoQueEntityMap() {
      assertThat(AuditConstants.ENTITY_LABELS).hasSameSizeAs(AuditConstants.ENTITY_MAP);
    }

    @Test
    @DisplayName("Deve ter labels para todas as chaves do ENTITY_MAP")
    void deveTerLabelsParaTodasAsChavesDoEntityMap() {
      for (String key : AuditConstants.ENTITY_MAP.keySet()) {
        assertThat(AuditConstants.ENTITY_LABELS)
            .as("ENTITY_LABELS deve conter chave '%s'", key)
            .containsKey(key);
      }
    }

    @Test
    @DisplayName("Labels devem estar em português brasileiro")
    void labelsDevemEstarEmPortuguesBrasileiro() {
      assertThat(AuditConstants.ENTITY_LABELS)
          .containsEntry("emprestimo", "Empréstimo")
          .containsEntry("usuario", "Usuário")
          .containsEntry("saida", "Saída")
          .containsEntry("solicitacao", "Solicitação de Compra");
    }

    @Test
    @DisplayName("Labels não devem estar vazios ou nulos")
    void labelsNaoDevemEstarVaziosOuNulos() {
      for (String label : AuditConstants.ENTITY_LABELS.values()) {
        assertThat(label).isNotNull().isNotBlank();
      }
    }
  }

  @Nested
  @DisplayName("Constantes de paginação")
  class ConstantesDePaginacao {

    @Test
    @DisplayName("MAX_PAGE_SIZE deve ser 100")
    void maxPageSizeDeveSer100() {
      assertThat(AuditConstants.MAX_PAGE_SIZE).isEqualTo(100);
    }

    @Test
    @DisplayName("DEFAULT_PAGE_SIZE deve ser 20")
    void defaultPageSizeDeveSer20() {
      assertThat(AuditConstants.DEFAULT_PAGE_SIZE).isEqualTo(20);
    }

    @Test
    @DisplayName("MAX_TIMELINE_RESULTS_PER_ENTITY deve ser 500")
    void maxTimelineResultsPerEntityDeveSer500() {
      assertThat(AuditConstants.MAX_TIMELINE_RESULTS_PER_ENTITY).isEqualTo(500);
    }

    @Test
    @DisplayName("DEFAULT_TIMELINE_DAYS deve ser 30")
    void defaultTimelineDaysDeveSer30() {
      assertThat(AuditConstants.DEFAULT_TIMELINE_DAYS).isEqualTo(30);
    }
  }
}

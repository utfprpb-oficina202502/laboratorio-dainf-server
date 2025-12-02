package br.com.utfpr.gerenciamento.server.util;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.utfpr.gerenciamento.server.dto.CompraListDto;
import br.com.utfpr.gerenciamento.server.dto.EmprestimoListDto;
import br.com.utfpr.gerenciamento.server.dto.FornecedorListDto;
import br.com.utfpr.gerenciamento.server.dto.GrupoResponseDto;
import br.com.utfpr.gerenciamento.server.dto.ItemListDto;
import br.com.utfpr.gerenciamento.server.dto.ReservaListDto;
import br.com.utfpr.gerenciamento.server.dto.SaidaListDto;
import br.com.utfpr.gerenciamento.server.dto.SolicitacaoListDto;
import br.com.utfpr.gerenciamento.server.dto.UsuarioListDto;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes unitarios para SortableFieldExtractor.
 *
 * <p>Valida a extracao automatica de campos ordenaves via reflection e o cache de metadados.
 */
class SortableFieldExtractorTest {

  // =================================================================
  // Testes de extractSortableFields()
  // =================================================================

  @Nested
  @DisplayName("extractSortableFields()")
  class ExtractSortableFieldsTests {

    @Test
    @DisplayName("Deve extrair campos simples sem entityPath")
    void deveExtrairCamposSimples() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(GrupoResponseDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("descricao", "descricao")
          .hasSize(2);
    }

    @Test
    @DisplayName("Deve extrair campos com entityPath customizado")
    void deveExtrairCamposComEntityPath() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(EmprestimoListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("dataEmprestimo", "dataEmprestimo")
          .containsEntry("prazoDevolucao", "prazoDevolucao")
          .containsEntry("dataDevolucao", "dataDevolucao")
          .containsEntry("usuarioEmprestimoNome", "usuarioEmprestimo.nome");
    }

    @Test
    @DisplayName("Deve extrair campos de ItemListDto incluindo grupo aninhado")
    void deveExtrairCamposDeItem() {
      // When
      Map<String, String> fields = SortableFieldExtractor.extractSortableFields(ItemListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("nome", "nome")
          .containsEntry("localizacao", "localizacao")
          .containsEntry("saldo", "saldo")
          .containsEntry("grupo", "grupo.descricao")
          .hasSize(5);
    }

    @Test
    @DisplayName("Deve extrair campos de CompraListDto com fornecedor aninhado")
    void deveExtrairCamposDeCompra() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(CompraListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("dataCompra", "dataCompra")
          .containsEntry("fornecedorRazaoSocial", "fornecedor.razaoSocial")
          .containsEntry("fornecedorNomeFantasia", "fornecedor.nomeFantasia");
    }

    @Test
    @DisplayName("Deve extrair campos de ReservaListDto com usuario aninhado")
    void deveExtrairCamposDeReserva() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(ReservaListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("usuarioNome", "usuario.nome")
          .containsEntry("dataReserva", "dataReserva")
          .containsEntry("dataRetirada", "dataRetirada");
    }

    @Test
    @DisplayName("Deve extrair campos de SolicitacaoListDto")
    void deveExtrairCamposDeSolicitacao() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(SolicitacaoListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("descricao", "descricao")
          .containsEntry("dataSolicitacao", "dataSolicitacao")
          .containsEntry("usuarioNome", "usuario.nome");
    }

    @Test
    @DisplayName("Deve extrair campos de SaidaListDto com usuarioResponsavel aninhado")
    void deveExtrairCamposDeSaida() {
      // When
      Map<String, String> fields = SortableFieldExtractor.extractSortableFields(SaidaListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("dataSaida", "dataSaida")
          .containsEntry("observacao", "observacao")
          .containsEntry("usuarioResponsavelNome", "usuarioResponsavel.nome");
    }

    @Test
    @DisplayName("Deve extrair campos de FornecedorListDto")
    void deveExtrairCamposDeFornecedor() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(FornecedorListDto.class);

      // Then
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("razaoSocial", "razaoSocial")
          .containsEntry("nomeFantasia", "nomeFantasia")
          .containsEntry("cnpj", "cnpj")
          .hasSize(4);
    }

    @Test
    @DisplayName("Deve extrair campos de UsuarioListDto (sem permissoes)")
    void deveExtrairCamposDeUsuario() {
      // When
      Map<String, String> fields =
          SortableFieldExtractor.extractSortableFields(UsuarioListDto.class);

      // Then - permissoes nao e ordenavel (colecao)
      assertThat(fields)
          .containsEntry("id", "id")
          .containsEntry("nome", "nome")
          .containsEntry("email", "email")
          .doesNotContainKey("permissoes")
          .hasSize(3);
    }
  }

  // =================================================================
  // Testes de Cache
  // =================================================================

  @Nested
  @DisplayName("Cache de Metadados")
  class CacheTests {

    @Test
    @DisplayName("Deve retornar mesma instancia do cache em chamadas consecutivas")
    void deveRetornarMesmaInstanciaDoCache() {
      // When
      Map<String, String> primeiraLeitura =
          SortableFieldExtractor.extractSortableFields(ItemListDto.class);
      Map<String, String> segundaLeitura =
          SortableFieldExtractor.extractSortableFields(ItemListDto.class);

      // Then - mesma referencia de objeto (cache hit)
      assertThat(primeiraLeitura).isSameAs(segundaLeitura);
    }

    @Test
    @DisplayName("Deve cachear resultados de diferentes classes independentemente")
    void deveCachearDiferentesClassesIndependentemente() {
      // When
      Map<String, String> itemFields =
          SortableFieldExtractor.extractSortableFields(ItemListDto.class);
      Map<String, String> emprestimoFields =
          SortableFieldExtractor.extractSortableFields(EmprestimoListDto.class);

      // Then - diferentes instancias, diferentes tamanhos
      assertThat(itemFields).isNotSameAs(emprestimoFields).hasSize(5);
    }
  }

  // =================================================================
  // Testes de getAllowedSortProperties()
  // =================================================================

  @Nested
  @DisplayName("getAllowedSortProperties()")
  class GetAllowedSortPropertiesTests {

    @Test
    @DisplayName("Deve retornar apenas nomes dos campos DTO")
    void deveRetornarApenasNomesDosCampos() {
      // When
      Set<String> allowedProperties =
          SortableFieldExtractor.getAllowedSortProperties(EmprestimoListDto.class);

      // Then - apenas nomes DTO, nao paths de entidade
      assertThat(allowedProperties)
          .contains(
              "id", "dataEmprestimo", "prazoDevolucao", "dataDevolucao", "usuarioEmprestimoNome")
          .doesNotContain("usuarioEmprestimo.nome"); // Path da entidade nao deve estar aqui
    }

    @Test
    @DisplayName("Deve ser consistente com extractSortableFields")
    void deveSerConsistenteComExtractSortableFields() {
      // When
      Set<String> properties = SortableFieldExtractor.getAllowedSortProperties(ItemListDto.class);
      Map<String, String> mappings =
          SortableFieldExtractor.extractSortableFields(ItemListDto.class);

      // Then
      assertThat(properties).isEqualTo(mappings.keySet());
    }
  }

  // =================================================================
  // Testes de translateToEntityPath()
  // =================================================================

  @Nested
  @DisplayName("translateToEntityPath()")
  class TranslateToEntityPathTests {

    @Test
    @DisplayName("Deve traduzir campo simples para mesmo nome")
    void deveTraduzirCampoSimples() {
      // When
      String path = SortableFieldExtractor.translateToEntityPath(GrupoResponseDto.class, "id");

      // Then
      assertThat(path).isEqualTo("id");
    }

    @Test
    @DisplayName("Deve traduzir campo aninhado para entityPath")
    void deveTraduzirCampoAninhado() {
      // When
      String path =
          SortableFieldExtractor.translateToEntityPath(
              EmprestimoListDto.class, "usuarioEmprestimoNome");

      // Then
      assertThat(path).isEqualTo("usuarioEmprestimo.nome");
    }

    @Test
    @DisplayName("Deve traduzir fornecedorRazaoSocial para fornecedor.razaoSocial")
    void deveTraduzirFornecedor() {
      // When
      String path =
          SortableFieldExtractor.translateToEntityPath(
              CompraListDto.class, "fornecedorRazaoSocial");

      // Then
      assertThat(path).isEqualTo("fornecedor.razaoSocial");
    }

    @Test
    @DisplayName("Deve traduzir grupo para grupo.descricao")
    void deveTraduzirGrupo() {
      // When
      String path = SortableFieldExtractor.translateToEntityPath(ItemListDto.class, "grupo");

      // Then
      assertThat(path).isEqualTo("grupo.descricao");
    }

    @Test
    @DisplayName("Deve retornar campo original se nao encontrado no mapeamento")
    void deveRetornarCampoOriginalSeNaoEncontrado() {
      // When - campo nao anotado com @SortableField
      String path =
          SortableFieldExtractor.translateToEntityPath(ItemListDto.class, "campoInexistente");

      // Then - retorna o proprio nome como fallback
      assertThat(path).isEqualTo("campoInexistente");
    }
  }

  // =================================================================
  // Testes de Cobertura de Todos os DTOs
  // =================================================================

  @ParameterizedTest(name = "Classe {0} deve ter campos ordenáveis")
  @ValueSource(
      classes = {
        ItemListDto.class,
        EmprestimoListDto.class,
        ReservaListDto.class,
        SolicitacaoListDto.class,
        CompraListDto.class,
        FornecedorListDto.class,
        UsuarioListDto.class,
        SaidaListDto.class,
        GrupoResponseDto.class
      })
  void todosOsDtoDevemTerCamposOrdenaveis(Class<?> dtoClass) {
    // When
    Set<String> properties = SortableFieldExtractor.getAllowedSortProperties(dtoClass);

    // Then - todos os DTOs devem ter pelo menos id como ordenavel
    assertThat(properties)
        .as("Classe %s deve ter campos ordenáveis", dtoClass.getSimpleName())
        .isNotEmpty()
        .contains("id");
  }
}

package br.com.utfpr.gerenciamento.server.service.impl;

import static org.assertj.core.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.dto.FornecedorResponseDto;
import br.com.utfpr.gerenciamento.server.model.Cidade;
import br.com.utfpr.gerenciamento.server.model.Estado;
import br.com.utfpr.gerenciamento.server.model.Fornecedor;
import br.com.utfpr.gerenciamento.server.repository.CidadeRepository;
import br.com.utfpr.gerenciamento.server.repository.EstadoRepository;
import br.com.utfpr.gerenciamento.server.repository.FornecedorRepository;
import br.com.utfpr.gerenciamento.server.service.FornecedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integracao para FornecedorService.complete() com getSearchableFieldMappings().
 *
 * <p>Este teste valida especificamente a busca por campos numericos (ID) quando o service usa
 * getSearchableFieldMappings() customizado, garantindo que o SQL gerado seja compativel com
 * PostgreSQL (cast para varchar antes do LIKE).
 *
 * <p>Bug corrigido: "ERROR: operator does not exist: bigint ~~ text" ocorria quando o ID era
 * incluido na busca sem cast explicito para String.
 *
 * @see CrudServiceImpl#createPredicateForPath
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FornecedorCompleteIntegrationTest {

  @Autowired private FornecedorService fornecedorService;

  @Autowired private FornecedorRepository fornecedorRepository;

  @Autowired private CidadeRepository cidadeRepository;

  @Autowired private EstadoRepository estadoRepository;

  private Fornecedor fornecedor1;
  private Fornecedor fornecedor2;

  @BeforeEach
  void setUp() {
    String uniqueSuffix = "_TEST_" + System.currentTimeMillis();

    // Busca cidade e estado existentes (pre-populados via Flyway)
    Estado estado =
        estadoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Nenhum estado encontrado"));
    Cidade cidade =
        cidadeRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Nenhuma cidade encontrada"));

    fornecedor1 = new Fornecedor();
    fornecedor1.setRazaoSocial("ZZZ Empresa Alpha" + uniqueSuffix);
    fornecedor1.setNomeFantasia("Alpha Ltda");
    fornecedor1.setCnpj("12345678000101");
    fornecedor1.setIe("1234567890");
    fornecedor1.setEstado(estado);
    fornecedor1.setCidade(cidade);
    fornecedor1 = fornecedorRepository.save(fornecedor1);

    fornecedor2 = new Fornecedor();
    fornecedor2.setRazaoSocial("ZZZ Empresa Beta" + uniqueSuffix);
    fornecedor2.setNomeFantasia("Beta Ltda");
    fornecedor2.setCnpj("98765432000102");
    fornecedor2.setIe("0987654321");
    fornecedor2.setEstado(estado);
    fornecedor2.setCidade(cidade);
    fornecedor2 = fornecedorRepository.save(fornecedor2);
  }

  /**
   * Testa busca por ID numerico via complete().
   *
   * <p>Este teste garante que a query SQL gerada usa cast(id as varchar) em vez de aplicar LIKE
   * diretamente no bigint, evitando o erro "operator does not exist: bigint ~~ text" no PostgreSQL.
   */
  @Test
  void complete_ComIdNumerico_DeveEncontrarFornecedor() {
    // Given - busca pelo ID do primeiro fornecedor
    String query = fornecedor1.getId().toString();
    PageRequest pageable = PageRequest.of(0, 10);

    // When - executa complete() que usa getSearchableFieldMappings() com campo "id"
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then - deve encontrar o fornecedor pelo ID
    assertThat(result.getContent()).isNotEmpty();
    assertThat(result.getContent().stream().findFirst().orElseThrow().getId())
        .isEqualTo(fornecedor1.getId());
  }

  @Test
  void complete_ComRazaoSocial_DeveEncontrarFornecedor() {
    // Given
    String query = "ZZZ Empresa Alpha";
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().stream().findFirst().orElseThrow().getRazaoSocial())
        .containsIgnoringCase("Alpha");
  }

  @Test
  void complete_ComNomeFantasia_DeveEncontrarFornecedor() {
    // Given
    String query = "Beta Ltda";
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().stream().findFirst().orElseThrow().getNomeFantasia())
        .isEqualTo("Beta Ltda");
  }

  @Test
  void complete_ComCnpj_DeveEncontrarFornecedor() {
    // Given
    String query = "12345678000101";
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).isNotEmpty();
    assertThat(result.getContent().stream().findFirst().orElseThrow().getCnpj())
        .isEqualTo("12345678000101");
  }

  @Test
  void complete_ComQueryVazia_DeveRetornarTodos() {
    // Given
    String query = "";
    PageRequest pageable = PageRequest.of(0, 100);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void complete_ComQueryNull_DeveRetornarTodos() {
    // Given
    PageRequest pageable = PageRequest.of(0, 100);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(null, pageable);

    // Then
    assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void complete_ComQueryInexistente_DeveRetornarVazio() {
    // Given
    String query = "XYZQWERTY_ABSOLUTAMENTE_INEXISTENTE_98765";
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  void complete_CaseInsensitive_DeveEncontrarFornecedor() {
    // Given - busca em minusculas
    String query = "zzz empresa alpha";
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<FornecedorResponseDto> result = fornecedorService.complete(query, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().stream().findFirst().orElseThrow().getRazaoSocial())
        .containsIgnoringCase("Alpha");
  }
}

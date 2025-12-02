package br.com.utfpr.gerenciamento.server.service.impl;

import static org.assertj.core.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.dto.GrupoResponseDto;
import br.com.utfpr.gerenciamento.server.model.Grupo;
import br.com.utfpr.gerenciamento.server.repository.GrupoRepository;
import br.com.utfpr.gerenciamento.server.service.GrupoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integracao para o metodo filterByAllFields() em CrudServiceImpl.
 *
 * <p>Testa os metodos privados createPredicateForPath() atraves da interface publica
 * filterByAllFields().
 *
 * <p>Nota: Testes de LocalDate e nested paths sao cobertos pelos testes de controller que usam
 * busca em producao (ex: EmprestimoController, ReservaController).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FilterByAllFieldsIntegrationTest {

  @Autowired private GrupoService grupoService;

  @Autowired private GrupoRepository grupoRepository;

  private Grupo grupo1;
  private Grupo grupo2;

  @BeforeEach
  void setUp() {
    // Cria grupos com descricoes unicas para evitar conflitos
    // Nao deleta grupos existentes pois podem ter itens vinculados (FK)
    String uniqueSuffix = "_TEST_" + System.currentTimeMillis();

    grupo1 = new Grupo();
    grupo1.setDescricao("ZZZ Eletronicos" + uniqueSuffix);
    grupo1 = grupoRepository.save(grupo1);

    grupo2 = new Grupo();
    grupo2.setDescricao("ZZZ Material" + uniqueSuffix);
    grupo2 = grupoRepository.save(grupo2);
  }

  // =================================================================
  // Testes para createPredicateForPath() - Path Simples (String e Number)
  // =================================================================

  @Test
  void testFilterByAllFields_ComIdNumerico_DeveEncontrarPorId() {
    // Given
    String filter = grupo1.getId().toString();
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).isNotEmpty();
    assertThat(result.getContent().stream().findFirst().orElseThrow().getId())
        .isEqualTo(grupo1.getId());
  }

  @Test
  void testFilterByAllFields_ComDescricao_DeveEncontrarPorTexto() {
    // Given - busca pelo prefixo unico
    String filter = "ZZZ Eletronicos";
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().stream().findFirst().orElseThrow().getDescricao())
        .containsIgnoringCase("ZZZ Eletronicos");
  }

  @Test
  void testFilterByAllFields_ComFiltroVazio_DeveRetornarRegistros() {
    // Given
    String filter = "";
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 100);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then - retorna todos os registros existentes (incluindo os de teste)
    assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void testFilterByAllFields_ComFiltroNull_DeveRetornarRegistros() {
    // Given
    Specification<Grupo> spec = grupoService.filterByAllFields(null);
    PageRequest pageable = PageRequest.of(0, 100);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void testFilterByAllFields_ComEspacosEmBranco_DeveRetornarRegistros() {
    // Given - apenas espacos em branco
    String filter = "   ";
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 100);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void testFilterByAllFields_ComFiltroQueNaoEncontraNada_DeveRetornarVazio() {
    // Given - filtro completamente unico que nao corresponde a nenhum registro
    String filter = "XYZQWERTY_ABSOLUTAMENTE_INEXISTENTE_98765";
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  void testFilterByAllFields_CaseInsensitive_DeveEncontrarIndependenteDoCaso() {
    // Given - busca em minusculas pelo prefixo ZZZ
    String filter = "zzz material";
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().stream().findFirst().orElseThrow().getDescricao())
        .containsIgnoringCase("ZZZ Material");
  }

  @ParameterizedTest(name = "Filter ''{0}'' deve retornar {1} resultado(s)")
  @CsvSource({
    "ZZZ, 2", // busca parcial - ambos registros comecam com ZZZ
    "ZZZ Eletronicos, 1", // caracteres especiais (espaco) - nao deve falhar
    "_TEST_, 2" // sufixo comum - ambos contem _TEST_ na descricao
  })
  void testFilterByAllFields_ComDiversosFiltros_DeveRetornarQuantidadeEsperada(
      String filter, int expectedSize) {
    // Given
    Specification<Grupo> spec = grupoService.filterByAllFields(filter);
    PageRequest pageable = PageRequest.of(0, 10);

    // When
    Page<GrupoResponseDto> result = grupoService.findAllSpecification(spec, pageable);

    // Then
    assertThat(result.getContent()).hasSize(expectedSize);
  }
}

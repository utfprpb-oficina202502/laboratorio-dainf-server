package br.com.utfpr.gerenciamento.server.dto;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.repository.projection.ItemSimpleProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes para ItemSimpleDto")
class ItemSimpleDtoTest {

  @Test
  @DisplayName("fromProjection - Deve converter projeção para DTO corretamente")
  void fromProjection_DeveConverterProjecaoParaDto() {
    // Arrange
    ItemSimpleProjection projection = createProjection(1L, "Notebook Dell");

    // Act
    ItemSimpleDto dto = ItemSimpleDto.fromProjection(projection);

    // Assert
    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals("Notebook Dell", dto.getNome());
  }

  @Test
  @DisplayName("fromProjection - Deve retornar null quando projeção é null")
  void fromProjection_DeveRetornarNullQuandoProjecaoNull() {
    // Act
    ItemSimpleDto dto = ItemSimpleDto.fromProjection(null);

    // Assert
    assertNull(dto);
  }

  @Test
  @DisplayName("fromProjection - Deve converter projeção com nome null")
  void fromProjection_DeveConverterProjecaoComNomeNull() {
    // Arrange
    ItemSimpleProjection projection = createProjection(1L, null);

    // Act
    ItemSimpleDto dto = ItemSimpleDto.fromProjection(projection);

    // Assert
    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertNull(dto.getNome());
  }

  @Test
  @DisplayName("fromProjection - Deve converter projeção com id null")
  void fromProjection_DeveConverterProjecaoComIdNull() {
    // Arrange
    ItemSimpleProjection projection = createProjection(null, "Item Teste");

    // Act
    ItemSimpleDto dto = ItemSimpleDto.fromProjection(projection);

    // Assert
    assertNotNull(dto);
    assertNull(dto.getId());
    assertEquals("Item Teste", dto.getNome());
  }

  @Test
  @DisplayName("builder - Deve criar DTO usando builder")
  void builder_DeveCriarDtoUsandoBuilder() {
    // Act
    ItemSimpleDto dto = ItemSimpleDto.builder().id(1L).nome("Monitor LG").build();

    // Assert
    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals("Monitor LG", dto.getNome());
  }

  @Test
  @DisplayName("noArgsConstructor - Deve criar DTO vazio")
  void noArgsConstructor_DeveCriarDtoVazio() {
    // Act
    ItemSimpleDto dto = new ItemSimpleDto();

    // Assert
    assertNotNull(dto);
    assertNull(dto.getId());
    assertNull(dto.getNome());
  }

  @Test
  @DisplayName("allArgsConstructor - Deve criar DTO com todos os argumentos")
  void allArgsConstructor_DeveCriarDtoComTodosArgumentos() {
    // Act
    ItemSimpleDto dto = new ItemSimpleDto(1L, "Teclado USB");

    // Assert
    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals("Teclado USB", dto.getNome());
  }

  @Test
  @DisplayName("setters - Deve permitir alteração de valores")
  void setters_DevePermitirAlteracaoDeValores() {
    // Arrange
    ItemSimpleDto dto = new ItemSimpleDto();

    // Act
    dto.setId(99L);
    dto.setNome("Mouse Wireless");

    // Assert
    assertEquals(99L, dto.getId());
    assertEquals("Mouse Wireless", dto.getNome());
  }

  @Test
  @DisplayName("equals - DTOs com mesmos valores devem ser iguais")
  void equals_DtosComMesmosValoresDevemSerIguais() {
    // Arrange
    ItemSimpleDto dto1 = new ItemSimpleDto(1L, "Item A");
    ItemSimpleDto dto2 = new ItemSimpleDto(1L, "Item A");

    // Assert
    assertEquals(dto1, dto2);
    assertEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  @DisplayName("equals - DTOs com valores diferentes devem ser diferentes")
  void equals_DtosComValoresDiferentesDevemSerDiferentes() {
    // Arrange
    ItemSimpleDto dto1 = new ItemSimpleDto(1L, "Item A");
    ItemSimpleDto dto2 = new ItemSimpleDto(2L, "Item B");

    // Assert
    assertNotEquals(dto1, dto2);
  }

  @Test
  @DisplayName("toString - Deve conter informações do DTO")
  void toString_DeveConterInformacoesDoDto() {
    // Arrange
    ItemSimpleDto dto = new ItemSimpleDto(1L, "Item Teste");

    // Act
    String toString = dto.toString();

    // Assert
    assertNotNull(toString);
    assertTrue(toString.contains("1"));
    assertTrue(toString.contains("Item Teste"));
  }

  /**
   * Cria uma projeção simplificada de Item para testes.
   *
   * @param id ID do item
   * @param nome Nome do item
   * @return Projeção implementada
   */
  private ItemSimpleProjection createProjection(Long id, String nome) {
    return new ItemSimpleProjection() {
      @Override
      public Long getId() {
        return id;
      }

      @Override
      public String getNome() {
        return nome;
      }
    };
  }
}

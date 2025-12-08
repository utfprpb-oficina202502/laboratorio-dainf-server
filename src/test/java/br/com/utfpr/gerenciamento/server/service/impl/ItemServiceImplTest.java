package br.com.utfpr.gerenciamento.server.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.dto.ItemListDto;
import br.com.utfpr.gerenciamento.server.dto.ItemResponseDto;
import br.com.utfpr.gerenciamento.server.dto.ItemSimpleDto;
import br.com.utfpr.gerenciamento.server.enumeration.TipoItem;
import br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException;
import br.com.utfpr.gerenciamento.server.minio.config.MinioConfig;
import br.com.utfpr.gerenciamento.server.minio.service.MinioService;
import br.com.utfpr.gerenciamento.server.model.Grupo;
import br.com.utfpr.gerenciamento.server.model.Item;
import br.com.utfpr.gerenciamento.server.model.ItemImage;
import br.com.utfpr.gerenciamento.server.repository.EmprestimoItemRepository;
import br.com.utfpr.gerenciamento.server.repository.ItemImageRepository;
import br.com.utfpr.gerenciamento.server.repository.ItemRepository;
import br.com.utfpr.gerenciamento.server.repository.projection.ItemCompleteWithDisponibilidade;
import br.com.utfpr.gerenciamento.server.repository.projection.ItemListProjection;
import br.com.utfpr.gerenciamento.server.repository.projection.ItemSimpleProjection;
import br.com.utfpr.gerenciamento.server.repository.projection.ItemWithQtdeEmprestada;
import br.com.utfpr.gerenciamento.server.service.EmailService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

  @Mock private ItemRepository itemRepository;
  @Mock private EmailService emailService;
  @Mock private MinioService minioService;
  @Mock private MinioConfig minioConfig;
  @Mock private ItemImageRepository itemImageRepository;
  @Mock private EmprestimoItemRepository emprestimoItemRepository;
  @Mock private ModelMapper modelMapper;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ItemServiceImpl service;

  private Item item;

  @BeforeEach
  void setUp() {
    item = createItemPadrao();
  }

  @ParameterizedTest(name = "{index}: Saldo={0}, Emprestado={1} -> Disponibilidade={2} ({3})")
  @CsvSource({
    "10.00, 3.00, 7.00, Permanente com estoque positivo",
    "5.00, 5.00, 0.00, Permanente com estoque zerado",
    "2.00, 5.00, 0.00, Permanente com saldo negativo (limitado a zero)",
    "0.00, 3.00, 0.00, Permanente com saldo zero"
  })
  @DisplayName("Testa cálculo de disponibilidade para itens permanentes com múltiplos cenários")
  void testFindOneWithDisponibilidade_Permanente_Parametrizado(
      String saldo, String qtdeEmprestada, String disponibilidadeEsperada, String descricao) {
    // Arrange
    item.setTipoItem(TipoItem.P);
    item.setSaldo(new BigDecimal(saldo));

    ItemWithQtdeEmprestada projection =
        createItemWithQtdeEmprestada(item, new BigDecimal(qtdeEmprestada));
    when(itemRepository.findByIdWithQtdeEmprestada(1L)).thenReturn(Optional.of(projection));

    // Act
    Item resultItem = service.findOneWithDisponibilidade(1L);

    // Assert
    assertNotNull(resultItem, "Item resultante não deve ser nulo");
    assertEquals(
        new BigDecimal(qtdeEmprestada),
        resultItem.getQuantidadeEmprestada(),
        "Quantidade emprestada deve corresponder ao valor informado");
    assertEquals(
        0,
        new BigDecimal(disponibilidadeEsperada)
            .compareTo(resultItem.getDisponivelEmprestimoCalculado()),
        "Disponibilidade calculada deve corresponder ao valor esperado");
    verify(itemRepository, times(1)).findByIdWithQtdeEmprestada(1L);
  }

  @ParameterizedTest(
      name = "{index}: Saldo={0}, Emprestado={1} -> Sem cálculo de disponibilidade ({2})")
  @CsvSource({
    "100.00, 0.00, Consumível com saldo",
    "0.00, 0.00, Consumível zerado",
    "50.00, 10.00, Consumível com empréstimos (disponibilidade ignorada)"
  })
  @DisplayName("Testa itens consumíveis não têm cálculo de disponibilidade")
  void testFindOneWithDisponibilidade_Consumivel_Parametrizado(
      String saldo, String qtdeEmprestada, String descricao) {
    // Arrange
    item.setTipoItem(TipoItem.C);
    item.setSaldo(new BigDecimal(saldo));

    ItemWithQtdeEmprestada projection =
        createItemWithQtdeEmprestada(item, new BigDecimal(qtdeEmprestada));
    when(itemRepository.findByIdWithQtdeEmprestada(1L)).thenReturn(Optional.of(projection));

    // Act
    Item resultItem = service.findOneWithDisponibilidade(1L);

    // Assert
    assertNotNull(resultItem, "Item resultante não deve ser nulo");
    assertEquals(
        new BigDecimal(qtdeEmprestada),
        resultItem.getQuantidadeEmprestada(),
        "Quantidade emprestada deve corresponder ao valor informado");
    assertNull(
        resultItem.getDisponivelEmprestimoCalculado(),
        "Itens consumíveis não devem ter disponibilidade calculada");
    verify(itemRepository, times(1)).findByIdWithQtdeEmprestada(1L);
  }

  @Test
  void testFindOneWithDisponibilidade_ItemNaoEncontrado() {
    when(itemRepository.findByIdWithQtdeEmprestada(999L)).thenReturn(Optional.empty());

    assertThrows(
        br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException.class,
        () -> service.findOneWithDisponibilidade(999L));
    verify(itemRepository, times(1)).findByIdWithQtdeEmprestada(999L);
  }

  @Test
  void testFindOneWithDisponibilidade_Permanente_QtdeEmprestadaNull() {
    item.setTipoItem(TipoItem.P);
    item.setSaldo(new BigDecimal("10.00"));

    ItemWithQtdeEmprestada projection = createItemWithQtdeEmprestada(item, null);
    when(itemRepository.findByIdWithQtdeEmprestada(1L)).thenReturn(Optional.of(projection));

    Item resultItem = service.findOneWithDisponibilidade(1L);

    assertNotNull(resultItem);
    assertEquals(0, resultItem.getQuantidadeEmprestada().compareTo(BigDecimal.ZERO));
    assertEquals(new BigDecimal("10.00"), resultItem.getDisponivelEmprestimoCalculado());
    verify(itemRepository, times(1)).findByIdWithQtdeEmprestada(1L);
  }

  @ParameterizedTest(name = "{index}: {1} itens abaixo do mínimo -> {2}")
  @CsvSource({
    "0, false, 'Sem itens abaixo do mínimo, não publica evento'",
    "1, true, 'Um item abaixo do mínimo, publica evento'",
    "5, true, 'Múltiplos itens abaixo do mínimo, publica evento'",
    "10, true, 'Muitos itens abaixo do mínimo, publica evento'"
  })
  @DisplayName("Testa envio de notificação baseado na quantidade de itens abaixo do mínimo")
  void testSendNotificationItensAtingiramQtdeMin_Parametrizado(
      long qtdeItensAbaixoMinimo, boolean devePublicarEvento, String descricao) {
    // Arrange
    when(itemRepository.countAllByQtdeMinimaIsLessThanSaldo()).thenReturn(qtdeItensAbaixoMinimo);

    // Act
    service.sendNotificationItensAtingiramQtdeMin();

    // Assert
    verify(itemRepository, times(1)).countAllByQtdeMinimaIsLessThanSaldo();

    if (devePublicarEvento) {
      verify(eventPublisher, times(1))
          .publishEvent(
              any(br.com.utfpr.gerenciamento.server.event.item.EstoqueMinNotificacaoEvent.class));
    } else {
      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  @Test
  void testItemComplete_WithQueryEmpty_DisponivelFalse_ReturnsAllItems() {
    String query = "";
    boolean disponivelParaEmprestimo = false;

    ItemCompleteWithDisponibilidade projection1 =
        createItemCompleteWithDisponibilidade(
            1L, "Item A", TipoItem.C, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
    ItemCompleteWithDisponibilidade projection2 =
        createItemCompleteWithDisponibilidade(
            2L, "Item B", TipoItem.P, new BigDecimal("5"), new BigDecimal("2"), BigDecimal.ZERO);

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection1, projection2);
    when(itemRepository.findCompleteWithDisponibilidade(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(2, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemA =
        result.stream().filter(i -> "Item A".equals(i.getNome())).findFirst().orElseThrow();
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemB =
        result.stream().filter(i -> "Item B".equals(i.getNome())).findFirst().orElseThrow();

    assertEquals(TipoItem.C, itemA.getTipoItem());
    assertEquals(TipoItem.P, itemB.getTipoItem());

    assertEquals(BigDecimal.ZERO, itemA.getQuantidadeEmprestada());
    assertNull(itemA.getDisponivelEmprestimoCalculado());

    assertEquals(new BigDecimal("2"), itemB.getQuantidadeEmprestada());
    assertEquals(new BigDecimal("3"), itemB.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteWithDisponibilidade(query);
  }

  @Test
  void testItemComplete_WithQueryEmpty_DisponivelTrue_ReturnsOnlyAvailableItems() {
    String query = "";
    boolean disponivelParaEmprestimo = true;

    ItemCompleteWithDisponibilidade projection1 =
        createItemCompleteAvailable(
            1L, "Item Disponível", TipoItem.C, new BigDecimal("10"), BigDecimal.ZERO);
    ItemCompleteWithDisponibilidade projection2 =
        createItemCompleteAvailable(
            2L, "Item Permanente", TipoItem.P, new BigDecimal("8"), new BigDecimal("3"));

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection1, projection2);
    when(itemRepository.findCompleteAvailableForLoan(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(2, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemDisponivel =
        result.stream()
            .filter(i -> "Item Disponível".equals(i.getNome()))
            .findFirst()
            .orElseThrow();
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemPermanente =
        result.stream()
            .filter(i -> "Item Permanente".equals(i.getNome()))
            .findFirst()
            .orElseThrow();

    assertNull(itemDisponivel.getDisponivelEmprestimoCalculado());

    assertEquals(new BigDecimal("5"), itemPermanente.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteAvailableForLoan(query);
  }

  @Test
  void testItemComplete_WithQueryAndDisponivelTrue_FiltersCorrectly() {
    String query = "Notebook";
    boolean disponivelParaEmprestimo = true;

    ItemCompleteWithDisponibilidade projection =
        createItemCompleteAvailable(
            1L, "Notebook Dell", TipoItem.P, new BigDecimal("5"), new BigDecimal("2"));

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection);
    when(itemRepository.findCompleteAvailableForLoan(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(1, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemResponse =
        result.stream().findFirst().orElseThrow();
    assertEquals("Notebook Dell", itemResponse.getNome());
    assertEquals(new BigDecimal("3"), itemResponse.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteAvailableForLoan(query);
  }

  @Test
  void testItemComplete_PermanentItem_ZeroSaldo_ReturnsZeroAvailability() {
    String query = "";
    boolean disponivelParaEmprestimo = false;

    ItemCompleteWithDisponibilidade projection =
        createItemCompleteWithDisponibilidade(
            1L, "Item Zerado", TipoItem.P, BigDecimal.ZERO, new BigDecimal("1"), BigDecimal.ZERO);

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection);
    when(itemRepository.findCompleteWithDisponibilidade(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(1, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemResponse =
        result.stream().findFirst().orElseThrow();
    assertEquals("Item Zerado", itemResponse.getNome());
    assertEquals(BigDecimal.ZERO, itemResponse.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteWithDisponibilidade(query);
  }

  @Test
  void testItemComplete_NullQtdeEmprestada_TreatsAsZero() {
    String query = "";
    boolean disponivelParaEmprestimo = false;

    ItemCompleteWithDisponibilidade projection =
        createItemCompleteWithDisponibilidade(
            1L, "Item Sem Emprestimo", TipoItem.P, new BigDecimal("10"), null, BigDecimal.ZERO);

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection);
    when(itemRepository.findCompleteWithDisponibilidade(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(1, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemResponse =
        result.stream().findFirst().orElseThrow();
    assertEquals("Item Sem Emprestimo", itemResponse.getNome());
    assertEquals(BigDecimal.ZERO, itemResponse.getQuantidadeEmprestada());
    assertEquals(new BigDecimal("10"), itemResponse.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteWithDisponibilidade(query);
  }

  @Test
  void testItemComplete_ConsumibleItem_NoDisponibilidadeCalculation() {
    String query = "";
    boolean disponivelParaEmprestimo = false;

    ItemCompleteWithDisponibilidade projection =
        createItemCompleteWithDisponibilidade(
            1L,
            "Item Consumível",
            TipoItem.C,
            new BigDecimal("15"),
            new BigDecimal("5"),
            BigDecimal.ZERO);

    List<ItemCompleteWithDisponibilidade> projections = List.of(projection);
    when(itemRepository.findCompleteWithDisponibilidade(query)).thenReturn(projections);

    List<br.com.utfpr.gerenciamento.server.dto.ItemResponseDto> result =
        service.itemComplete(query, disponivelParaEmprestimo);

    assertEquals(1, result.size());
    br.com.utfpr.gerenciamento.server.dto.ItemResponseDto itemResponse =
        result.stream().findFirst().orElseThrow();
    assertEquals("Item Consumível", itemResponse.getNome());
    assertEquals(TipoItem.C, itemResponse.getTipoItem());
    assertEquals(new BigDecimal("5"), itemResponse.getQuantidadeEmprestada());
    assertNull(itemResponse.getDisponivelEmprestimoCalculado());

    verify(itemRepository, times(1)).findCompleteWithDisponibilidade(query);
  }

  // Métodos auxiliares para criar dados de teste
  private Item createItemPadrao() {
    Item i = new Item();
    i.setId(1L);
    i.setNome("Notebook Dell");
    i.setDescricao("Notebook Dell 15 polegadas");
    i.setTipoItem(TipoItem.P);
    i.setSaldo(new BigDecimal("10.00"));
    i.setQtdeMinima(new BigDecimal("1.00"));
    return i;
  }

  private ItemWithQtdeEmprestada createItemWithQtdeEmprestada(
      Item item, BigDecimal qtdeEmprestada) {
    return new ItemWithQtdeEmprestada() {
      @Override
      public Item getItem() {
        return item;
      }

      @Override
      public BigDecimal getQtdeEmprestada() {
        return qtdeEmprestada;
      }
    };
  }

  private ItemCompleteWithDisponibilidade createItemCompleteWithDisponibilidade(
      Long id,
      String nome,
      TipoItem tipoItem,
      BigDecimal saldo,
      BigDecimal qtdeEmprestada,
      BigDecimal valor) {
    return new ItemCompleteWithDisponibilidade() {
      @Override
      public Long getId() {
        return id;
      }

      @Override
      public String getNome() {
        return nome;
      }

      @Override
      public BigDecimal getSaldo() {
        return saldo;
      }

      @Override
      public TipoItem getTipoItem() {
        return tipoItem;
      }

      @Override
      public BigDecimal getQtdeEmprestada() {
        return qtdeEmprestada;
      }

      @Override
      public BigDecimal getValor() {
        return valor;
      }

      @Override
      public Grupo getGrupo() {
        return createGrupo();
      }
    };
  }

  private ItemCompleteWithDisponibilidade createItemCompleteAvailable(
      Long id, String nome, TipoItem tipoItem, BigDecimal saldo, BigDecimal qtdeEmprestada) {
    return createItemCompleteWithDisponibilidade(
        id, nome, tipoItem, saldo, qtdeEmprestada, BigDecimal.ZERO);
  }

  private Grupo createGrupo() {
    return new Grupo(1L, "Grupo Teste");
  }

  // ==================== Testes para setCoverImage ====================

  @Test
  @DisplayName("setCoverImage - Deve definir imagem como capa e remover das outras")
  void testSetCoverImage_DeveDefinirImagemComoCapa() {
    // Arrange
    ItemImage img1 = new ItemImage();
    img1.setId(1L);
    img1.setCover(false);

    ItemImage img2 = new ItemImage();
    img2.setId(2L);
    img2.setCover(true); // Capa atual

    item.setImageItem(new ArrayList<>(List.of(img1, img2)));
    when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

    // Act
    service.setCoverImage(1L, 1L);

    // Assert
    assertTrue(img1.isCover(), "Imagem 1 deve ser a nova capa");
    assertFalse(img2.isCover(), "Imagem 2 não deve mais ser capa");
    verify(itemRepository).save(item);
  }

  @Test
  @DisplayName("setCoverImage - Item não encontrado deve lançar EntityNotFoundException")
  void testSetCoverImage_ItemNaoEncontrado_DeveLancarException() {
    // Arrange
    when(itemRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(EntityNotFoundException.class, () -> service.setCoverImage(999L, 1L));
    verify(itemRepository, never()).save(any());
  }

  @Test
  @DisplayName("setCoverImage - Imagem não encontrada no item não deve lançar erro")
  void testSetCoverImage_ImagemNaoEncontrada_NaoDeveLancarErro() {
    // Arrange
    ItemImage img1 = new ItemImage();
    img1.setId(1L);
    img1.setCover(true);

    item.setImageItem(new ArrayList<>(List.of(img1)));
    when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

    // Act - tenta definir imagem inexistente como capa
    service.setCoverImage(1L, 999L);

    // Assert - nenhuma imagem deve ser capa
    assertFalse(img1.isCover(), "Imagem 1 não deve mais ser capa");
    verify(itemRepository).save(item);
  }

  @Test
  @DisplayName("setCoverImage - Item sem imagens não deve lançar erro")
  void testSetCoverImage_ItemSemImagens_NaoDeveLancarErro() {
    // Arrange
    item.setImageItem(new ArrayList<>());
    when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

    // Act & Assert - não deve lançar exceção
    assertDoesNotThrow(() -> service.setCoverImage(1L, 1L));
    verify(itemRepository).save(item);
  }

  // ==================== Testes para findByGrupoPaged ====================

  @Test
  @DisplayName("findByGrupoPaged - Deve retornar página de itens do grupo")
  void testFindByGrupoPaged_DeveRetornarPaginaDeItens() {
    // Arrange
    Long grupoId = 1L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 25);

    Item item1 = createItemPadrao();
    Item item2 = new Item();
    item2.setId(2L);
    item2.setNome("Monitor LG");
    item2.setTipoItem(TipoItem.P);
    item2.setSaldo(new BigDecimal("5.00"));

    List<Item> itens = List.of(item1, item2);
    Page<Item> pageResult = new PageImpl<>(itens, pageable, 2);

    ItemResponseDto dto1 = new ItemResponseDto();
    dto1.setId(1L);
    dto1.setNome("Notebook Dell");

    ItemResponseDto dto2 = new ItemResponseDto();
    dto2.setId(2L);
    dto2.setNome("Monitor LG");

    when(itemRepository.findByGrupoIdPaged(grupoId, filter, pageable)).thenReturn(pageResult);
    when(modelMapper.map(item1, ItemResponseDto.class)).thenReturn(dto1);
    when(modelMapper.map(item2, ItemResponseDto.class)).thenReturn(dto2);

    // Act
    Page<ItemResponseDto> result = service.findByGrupoPaged(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.getContent().size());
    assertEquals(2, result.getTotalElements());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    assertEquals("Monitor LG", result.getContent().get(1).getNome());
    verify(itemRepository).findByGrupoIdPaged(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPaged - Deve filtrar itens por nome")
  void testFindByGrupoPaged_DeveFiltrarPorNome() {
    // Arrange
    Long grupoId = 1L;
    String filter = "Notebook";
    Pageable pageable = PageRequest.of(0, 25);

    Item item1 = createItemPadrao();
    List<Item> itens = List.of(item1);
    Page<Item> pageResult = new PageImpl<>(itens, pageable, 1);

    ItemResponseDto dto1 = new ItemResponseDto();
    dto1.setId(1L);
    dto1.setNome("Notebook Dell");

    when(itemRepository.findByGrupoIdPaged(grupoId, filter, pageable)).thenReturn(pageResult);
    when(modelMapper.map(item1, ItemResponseDto.class)).thenReturn(dto1);

    // Act
    Page<ItemResponseDto> result = service.findByGrupoPaged(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    verify(itemRepository).findByGrupoIdPaged(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPaged - Deve retornar página vazia quando grupo não tem itens")
  void testFindByGrupoPaged_DeveRetornarPaginaVazia() {
    // Arrange
    Long grupoId = 999L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 25);

    Page<Item> pageResult = new PageImpl<>(List.of(), pageable, 0);
    when(itemRepository.findByGrupoIdPaged(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemResponseDto> result = service.findByGrupoPaged(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());
    verify(itemRepository).findByGrupoIdPaged(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPaged - Deve respeitar paginação")
  void testFindByGrupoPaged_DeveRespeitarPaginacao() {
    // Arrange
    Long grupoId = 1L;
    String filter = null;
    Pageable pageable = PageRequest.of(1, 10); // Segunda página

    Item item1 = createItemPadrao();
    List<Item> itens = List.of(item1);
    // PageImpl com total explícito de 25 elementos
    Page<Item> pageResult = new PageImpl<>(itens, pageable, 25);

    ItemResponseDto dto1 = new ItemResponseDto();
    dto1.setId(1L);
    dto1.setNome("Notebook Dell");

    when(itemRepository.findByGrupoIdPaged(grupoId, filter, pageable)).thenReturn(pageResult);
    when(modelMapper.map(item1, ItemResponseDto.class)).thenReturn(dto1);

    // Act
    Page<ItemResponseDto> result = service.findByGrupoPaged(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(25, result.getTotalElements());
    assertEquals(1, result.getNumber()); // Página 1 (0-indexed)
    assertEquals(3, result.getTotalPages()); // 25 itens / 10 por página = 3 páginas
    verify(itemRepository).findByGrupoIdPaged(grupoId, filter, pageable);
  }

  // ==================== Testes para findByGrupoPagedSimple ====================

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve retornar página de ItemSimpleDto")
  void testFindByGrupoPagedSimple_DeveRetornarPaginaDeItemSimpleDto() {
    // Arrange
    Long grupoId = 1L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 25);

    ItemSimpleProjection proj1 = createItemSimpleProjection(1L, "Notebook Dell");
    ItemSimpleProjection proj2 = createItemSimpleProjection(2L, "Monitor LG");

    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(proj1, proj2), pageable, 2);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.getContent().size());
    assertEquals(2, result.getTotalElements());
    assertEquals(1L, result.getContent().get(0).getId());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    assertEquals(2L, result.getContent().get(1).getId());
    assertEquals("Monitor LG", result.getContent().get(1).getNome());
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve filtrar por nome")
  void testFindByGrupoPagedSimple_DeveFiltrarPorNome() {
    // Arrange
    Long grupoId = 1L;
    String filter = "Notebook";
    Pageable pageable = PageRequest.of(0, 25);

    ItemSimpleProjection proj1 = createItemSimpleProjection(1L, "Notebook Dell");
    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve filtrar por ID")
  void testFindByGrupoPagedSimple_DeveFiltrarPorId() {
    // Arrange
    Long grupoId = 1L;
    String filter = "123";
    Pageable pageable = PageRequest.of(0, 25);

    ItemSimpleProjection proj1 = createItemSimpleProjection(123L, "Item 123");
    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(123L, result.getContent().get(0).getId());
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve retornar página vazia quando grupo não tem itens")
  void testFindByGrupoPagedSimple_DeveRetornarPaginaVazia() {
    // Arrange
    Long grupoId = 999L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 25);

    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(), pageable, 0);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve respeitar paginação")
  void testFindByGrupoPagedSimple_DeveRespeitarPaginacao() {
    // Arrange
    Long grupoId = 1L;
    String filter = null;
    Pageable pageable = PageRequest.of(1, 10); // Segunda página

    ItemSimpleProjection proj1 = createItemSimpleProjection(11L, "Item 11");
    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 25);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(25, result.getTotalElements());
    assertEquals(1, result.getNumber()); // Página 1 (0-indexed)
    assertEquals(3, result.getTotalPages()); // 25 itens / 10 por página = 3 páginas
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findByGrupoPagedSimple - Deve funcionar com filtro vazio")
  void testFindByGrupoPagedSimple_DeveFuncionarComFiltroVazio() {
    // Arrange
    Long grupoId = 1L;
    String filter = "";
    Pageable pageable = PageRequest.of(0, 25);

    ItemSimpleProjection proj1 = createItemSimpleProjection(1L, "Notebook Dell");
    Page<ItemSimpleProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findByGrupoIdPagedSimple(grupoId, filter, pageable)).thenReturn(pageResult);

    // Act
    Page<ItemSimpleDto> result = service.findByGrupoPagedSimple(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(itemRepository).findByGrupoIdPagedSimple(grupoId, filter, pageable);
  }

  /**
   * Cria uma projeção simplificada de Item para testes.
   *
   * @param id ID do item
   * @param nome Nome do item
   * @return Projeção mockada
   */
  private ItemSimpleProjection createItemSimpleProjection(Long id, String nome) {
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

  // ==================== Testes para findAllPagedList com grupoId ====================

  @Test
  @DisplayName("findAllPagedList(grupoId) - Deve retornar itens filtrados por grupo")
  void testFindAllPagedListWithGrupoId_DeveFiltrarPorGrupo() {
    // Arrange
    Long grupoId = 1L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 10);

    ItemListProjection proj1 = createItemListProjection(1L, "Notebook Dell", "Sala 101");
    ItemListProjection proj2 = createItemListProjection(2L, "Monitor LG", "Sala 102");

    Page<ItemListProjection> pageResult = new PageImpl<>(List.of(proj1, proj2), pageable, 2);
    when(itemRepository.findAllProjectedWithGroupFilter(grupoId, filter, pageable))
        .thenReturn(pageResult);

    // Act
    Page<ItemListDto> result = service.findAllPagedList(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.getContent().size());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    assertEquals("Monitor LG", result.getContent().get(1).getNome());
    verify(itemRepository).findAllProjectedWithGroupFilter(grupoId, filter, pageable);
    verify(itemRepository, never()).findAllProjected(any());
  }

  @Test
  @DisplayName("findAllPagedList(grupoId) - Deve combinar filtro de grupo com filtro de texto")
  void testFindAllPagedListWithGrupoId_DeveCombinarFiltros() {
    // Arrange
    Long grupoId = 1L;
    String filter = "Notebook";
    Pageable pageable = PageRequest.of(0, 10);

    ItemListProjection proj1 = createItemListProjection(1L, "Notebook Dell", "Sala 101");

    Page<ItemListProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findAllProjectedWithGroupFilter(grupoId, filter, pageable))
        .thenReturn(pageResult);

    // Act
    Page<ItemListDto> result = service.findAllPagedList(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("Notebook Dell", result.getContent().get(0).getNome());
    verify(itemRepository).findAllProjectedWithGroupFilter(grupoId, filter, pageable);
  }

  @Test
  @DisplayName("findAllPagedList(grupoId) - Deve usar filtro sem grupo quando grupoId é null")
  void testFindAllPagedListWithGrupoId_SemGrupoComFiltro() {
    // Arrange
    Long grupoId = null;
    String filter = "Dell";
    Pageable pageable = PageRequest.of(0, 10);

    ItemListProjection proj1 = createItemListProjection(1L, "Notebook Dell", "Sala 101");

    Page<ItemListProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findAllProjectedWithGroupFilter(grupoId, filter, pageable))
        .thenReturn(pageResult);

    // Act
    Page<ItemListDto> result = service.findAllPagedList(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(itemRepository).findAllProjectedWithGroupFilter(grupoId, filter, pageable);
  }

  @ParameterizedTest(name = "filter=''{0}'' deve usar findAllProjected")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t", "\n"})
  @DisplayName("findAllPagedList(grupoId) - Deve usar findAllProjected quando filter é inválido")
  void testFindAllPagedListWithGrupoId_FilterInvalido_UsaFindAllProjected(String filter) {
    // Arrange
    Long grupoId = null;
    Pageable pageable = PageRequest.of(0, 10);

    ItemListProjection proj1 = createItemListProjection(1L, "Notebook Dell", "Sala 101");

    Page<ItemListProjection> pageResult = new PageImpl<>(List.of(proj1), pageable, 1);
    when(itemRepository.findAllProjected(pageable)).thenReturn(pageResult);

    // Act
    Page<ItemListDto> result = service.findAllPagedList(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(itemRepository).findAllProjected(pageable);
    verify(itemRepository, never()).findAllProjectedWithGroupFilter(any(), any(), any());
  }

  @Test
  @DisplayName("findAllPagedList(grupoId) - Deve retornar página vazia quando grupo não tem itens")
  void testFindAllPagedListWithGrupoId_GrupoSemItens() {
    // Arrange
    Long grupoId = 999L;
    String filter = null;
    Pageable pageable = PageRequest.of(0, 10);

    Page<ItemListProjection> pageResult = new PageImpl<>(List.of(), pageable, 0);
    when(itemRepository.findAllProjectedWithGroupFilter(grupoId, filter, pageable))
        .thenReturn(pageResult);

    // Act
    Page<ItemListDto> result = service.findAllPagedList(grupoId, filter, pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());
    verify(itemRepository).findAllProjectedWithGroupFilter(grupoId, filter, pageable);
  }

  /**
   * Cria uma projeção de lista de Item para testes.
   *
   * @param id ID do item
   * @param nome Nome do item
   * @param localizacao Localização do item
   * @return Projeção mockada
   */
  private ItemListProjection createItemListProjection(Long id, String nome, String localizacao) {
    return new ItemListProjection() {
      @Override
      public Long getId() {
        return id;
      }

      @Override
      public String getNome() {
        return nome;
      }

      @Override
      public String getLocalizacao() {
        return localizacao;
      }

      @Override
      public BigDecimal getSaldo() {
        return new BigDecimal("10.00");
      }

      @Override
      public Long getGrupoId() {
        return 1L;
      }

      @Override
      public String getGrupoDescricao() {
        return "Grupo Teste";
      }

      @Override
      public String getImagemUrl() {
        return "image.jpg";
      }
    };
  }
}

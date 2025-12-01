package br.com.utfpr.gerenciamento.server.controller;

import br.com.utfpr.gerenciamento.server.dto.ItemResponseDto;
import br.com.utfpr.gerenciamento.server.model.Item;
import br.com.utfpr.gerenciamento.server.model.ItemImage;
import br.com.utfpr.gerenciamento.server.service.CrudService;
import br.com.utfpr.gerenciamento.server.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("item")
public class ItemController extends CrudController<Item, Long, ItemResponseDto> {

  private final ItemService itemService;

  /**
   * ThreadLocal para armazenar imagens a copiar durante o ciclo de vida da requisicao. Evita race
   * conditions em requisicoes concorrentes (controller e singleton).
   */
  private final ThreadLocal<List<ItemImage>> imagesToCopy = new ThreadLocal<>();

  public ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  protected CrudService<Item, Long, ItemResponseDto> getService() {
    return itemService;
  }

  @Override
  public void preSave(Item object) {
    if (object.getId() == null
        && object.getImageItem() != null
        && !object.getImageItem().isEmpty()) {
      this.imagesToCopy.set(object.getImageItem());
      object.setImageItem(null);
    }
  }

  @Override
  @GetMapping("{id}")
  public ItemResponseDto findone(@PathVariable("id") Long id) {
    return itemService.toDto(itemService.findOneWithDisponibilidade(id));
  }

  @Override
  @GetMapping
  public List<ItemResponseDto> findAll() {
    return getService().findAll(Sort.by("id"));
  }

  /**
   * Lista paginada de itens com filtro textual usando DTO simplificado.
   *
   * <p><b>Otimização:</b> Retorna apenas campos necessários para listagem via projeção SQL.
   *
   * @param page Número da página (0-indexed)
   * @param size Tamanho da página
   * @param filter Filtro opcional (busca textual em todos os campos)
   * @param order Campo de ordenação (padrão: "id")
   * @param asc Direção da ordenação (true = ASC, false = DESC, padrão: ASC)
   * @return Página de itens simplificados
   */
  @Override
  @GetMapping("page")
  @SuppressWarnings("unchecked")
  public Page<ItemResponseDto> findAllPaged(
      @RequestParam("page") int page,
      @RequestParam("size") int size,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String order,
      @RequestParam(required = false) Boolean asc) {
    PageRequest pageRequest = PageRequest.of(page, size);
    if (order != null && asc != null) {
      pageRequest =
          PageRequest.of(page, size, asc ? Sort.Direction.ASC : Sort.Direction.DESC, order);
    }
    return (Page<ItemResponseDto>) (Page<?>) itemService.findAllPagedList(filter, pageRequest);
  }

  @Override
  public void postSave(Item object) {
    List<ItemImage> images = this.imagesToCopy.get();
    if (images != null) {
      itemService.copyImagesItem(images, object.getId());
    }
    this.imagesToCopy.remove(); // Limpa ThreadLocal para evitar memory leak
  }

  /**
   * Endpoint paginado para autocomplete de itens com disponibilidade.
   *
   * <p>Sobrescreve o complete generico do CrudController para calcular disponibilidade. Retorna
   * todos os itens (mesmo sem estoque). Para filtrar por disponibilidade, use complete-disponivel.
   *
   * @param query Texto para filtro por nome
   * @param page Numero da pagina (0-indexed, minimo: 0)
   * @param size Tamanho da pagina (default: 10, minimo: 1, maximo: 100)
   * @return Pagina de ItemResponseDto com disponibilidade calculada
   */
  @Override
  @GetMapping("complete")
  public Page<ItemResponseDto> complete(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    // Para Item, usamos o metodo especifico que calcula disponibilidade
    // hasEstoque=false para retornar todos os itens (mesmo sem estoque)
    return itemService.itemCompletePaged(query, false, pageRequest);
  }

  /**
   * Endpoint paginado para autocomplete de itens com filtro de estoque.
   *
   * @param query Texto para filtro por nome
   * @param hasEstoque Se true, filtra apenas itens disponiveis para emprestimo (default: true)
   * @param page Numero da pagina (0-indexed, minimo: 0)
   * @param size Tamanho da pagina (default: 10, minimo: 1, maximo: 100)
   * @return Pagina de ItemResponseDto com disponibilidade calculada
   */
  @GetMapping("complete-disponivel")
  public Page<ItemResponseDto> completeDisponivel(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "true") Boolean hasEstoque,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    return itemService.itemCompletePaged(query, hasEstoque, pageRequest);
  }

  @PostMapping("upload-images")
  public void upload(
      @RequestParam("idItem") Long idItem,
      MultipartHttpServletRequest images,
      HttpServletRequest request) {
    if (images.getFile("anexos[]") != null) {
      itemService.saveImages(images, request, idItem);
    }
  }

  @GetMapping("imagens/{idItem}")
  public List<ItemImage> findAllImagesByItem(@PathVariable("idItem") Long idItem) {
    return itemService.getImagesItem(idItem);
  }

  @PostMapping("delete-image/{idItem}")
  public void deleteImageItem(
      @PathVariable("idItem") Long idItem, @RequestBody ItemImage itemImage) {
    itemService.deleteImage(itemImage, idItem);
  }
}

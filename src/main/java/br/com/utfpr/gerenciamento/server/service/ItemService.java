package br.com.utfpr.gerenciamento.server.service;

import br.com.utfpr.gerenciamento.server.dto.ItemListDto;
import br.com.utfpr.gerenciamento.server.dto.ItemResponseDto;
import br.com.utfpr.gerenciamento.server.model.Item;
import br.com.utfpr.gerenciamento.server.model.ItemImage;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartHttpServletRequest;

public interface ItemService extends CrudService<Item, Long, ItemResponseDto> {

  /**
   * Busca paginada para listagem com DTO simplificado.
   *
   * @param filter Filtro textual opcional
   * @param pageable Configuração de paginação
   * @return Página de DTOs simplificados
   */
  Page<ItemListDto> findAllPagedList(String filter, Pageable pageable);

  List<ItemResponseDto> itemComplete(String query, boolean disponivelParaEmprestimo);

  /**
   * Busca paginada de itens para autocomplete com dados de disponibilidade.
   *
   * @param query Texto para filtro por nome
   * @param disponivelParaEmprestimo Se true, filtra apenas itens disponiveis
   * @param pageable Configuracao de paginacao
   * @return Pagina de ItemResponseDto com disponibilidade calculada
   */
  Page<ItemResponseDto> itemCompletePaged(
      String query, boolean disponivelParaEmprestimo, Pageable pageable);

  List<ItemResponseDto> findByGrupo(Long id);

  /**
   * Busca paginada de itens por grupo com filtro opcional.
   *
   * @param grupoId ID do grupo
   * @param filter Texto para filtro por nome ou id
   * @param pageable Configuracao de paginacao
   * @return Pagina de ItemResponseDto
   */
  Page<ItemResponseDto> findByGrupoPaged(Long grupoId, String filter, Pageable pageable);

  void diminuiSaldoItem(Long idItem, BigDecimal qtde, boolean needValidationSaldo);

  void aumentaSaldoItem(Long idItem, BigDecimal qtde);

  BigDecimal getSaldoItem(Long idItem);

  Boolean saldoItemIsValid(BigDecimal saldoItem, BigDecimal qtdeVerificar);

  void saveImages(MultipartHttpServletRequest files, HttpServletRequest request, Long idItem);

  List<ItemImage> getImagesItem(Long idItem);

  void deleteImage(ItemImage image, Long idItem);

  void sendNotificationItensAtingiramQtdeMin();

  void copyImagesItem(List<ItemImage> itemImages, Long id);

  Item findOneWithDisponibilidade(Long id);

  /**
   * Define uma imagem como capa do item.
   *
   * <p>Remove o status de capa de todas as outras imagens do item e define a imagem especificada
   * como capa.
   *
   * @param itemId ID do item
   * @param imageId ID da imagem a ser definida como capa
   */
  void setCoverImage(Long itemId, Long imageId);
}

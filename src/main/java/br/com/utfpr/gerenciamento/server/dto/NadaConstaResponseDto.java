package br.com.utfpr.gerenciamento.server.dto;

import br.com.utfpr.gerenciamento.server.enumeration.NadaConstaStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NadaConstaResponseDto implements BaseListDto {
  @SortableField private Long id;

  @SortableField(entityPath = "usuario.email")
  private String usuarioEmail;

  @SortableField private NadaConstaStatus status;
  @SortableField private LocalDateTime sendAt;
  @SortableField private LocalDateTime createdAt;
  @SortableField private LocalDateTime updatedAt;
  @SortableField private String createdBy;
  @SortableField private String updatedBy;
}

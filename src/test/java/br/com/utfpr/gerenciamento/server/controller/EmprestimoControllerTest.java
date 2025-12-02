package br.com.utfpr.gerenciamento.server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.utfpr.gerenciamento.server.dto.EmprestimoResponseDto;
import br.com.utfpr.gerenciamento.server.service.EmprestimoService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
class EmprestimoControllerTest {

  private MockMvc mockMvc;

  @Mock private EmprestimoService emprestimoService;

  @InjectMocks private EmprestimoController emprestimoController;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(emprestimoController).build();
  }

  @Test
  void testFindByItemId_DeveRetornarListaDeEmprestimos() throws Exception {
    // Given
    Long itemId = 1L;
    EmprestimoResponseDto emprestimoDto = new EmprestimoResponseDto();
    emprestimoDto.setId(1L);

    PageRequest pageRequest = PageRequest.of(0, 10);
    Page<EmprestimoResponseDto> page = new PageImpl<>(Collections.singletonList(emprestimoDto), pageRequest, 1);
    when(emprestimoService.findAllByItemIdPaged(eq(itemId), any(Pageable.class))).thenReturn(page);

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-by-item/{itemId}", itemId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void testFindByItemId_DeveRetornarListaVaziaQuandoNenhumEmprestimoEncontrado() throws Exception {
    // Given
    Long itemId = 999L;

    PageRequest pageRequest = PageRequest.of(0, 10);
    Page<EmprestimoResponseDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
    when(emprestimoService.findAllByItemIdPaged(eq(itemId), any(Pageable.class))).thenReturn(emptyPage);

    // When & Then
    mockMvc
        .perform(
            get("/emprestimo/find-by-item/{itemId}", itemId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));
  }
}

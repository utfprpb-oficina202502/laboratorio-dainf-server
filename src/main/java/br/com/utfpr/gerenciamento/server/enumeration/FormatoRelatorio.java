package br.com.utfpr.gerenciamento.server.enumeration;

import lombok.Getter;
import org.springframework.http.MediaType;

/**
 * Formatos de exportação disponíveis para relatórios.
 *
 * <p>Suporta exportação em PDF (via Flying Saucer HTML→PDF) e Excel (via Apache POI).
 */
@Getter
public enum FormatoRelatorio {
  PDF("pdf", MediaType.APPLICATION_PDF),

  EXCEL(
      "xlsx",
      MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

  private final String extensao;
  private final MediaType mediaType;

  FormatoRelatorio(String extensao, MediaType mediaType) {
    this.extensao = extensao;
    this.mediaType = mediaType;
  }

  /**
   * Retorna o nome do arquivo com a extensão apropriada.
   *
   * @param nomeBase Nome base do arquivo (sem extensão)
   * @return Nome completo com extensão (ex: "relatorio.pdf")
   */
  public String getNomeArquivo(String nomeBase) {
    return nomeBase + "." + extensao;
  }
}

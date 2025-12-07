package br.com.utfpr.gerenciamento.server.service.report;

import br.com.utfpr.gerenciamento.server.exception.RelatorioException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Serviço para geração de arquivos Excel (XLSX) usando Apache POI.
 *
 * <p>Gera planilhas formatadas com headers, auto-ajuste de colunas e estilos básicos.
 *
 * <p>Segurança: - Limite de registros para prevenir DoS (OWASP A05:2021) - Exceções customizadas
 * sem exposição de detalhes internos (prevenção de Information Disclosure)
 */
@Slf4j
@Service
public class ExcelGeneratorService {

  /** Limite máximo de registros por planilha para prevenir DoS e OutOfMemoryError. */
  private static final int LIMITE_REGISTROS = 50_000;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Gera um arquivo Excel a partir de uma lista de dados.
   *
   * @param <T> Tipo dos dados
   * @param sheetName Nome da planilha
   * @param headers Array de nomes das colunas
   * @param dados Lista de objetos com os dados
   * @param extractors Array de funções para extrair valores de cada coluna
   * @return Array de bytes do arquivo XLSX
   * @throws RelatorioException Se a lista exceder o limite de registros ou ocorrer erro na geração
   */
  @SafeVarargs
  public final <T> byte[] generateExcel(
      String sheetName, String[] headers, List<T> dados, Function<T, Object>... extractors) {

    log.debug("Gerando Excel '{}' com {} registros", sheetName, dados.size());

    // Validar limite de registros (prevenção de DoS)
    if (dados.size() > LIMITE_REGISTROS) {
      log.warn(
          "Tentativa de gerar Excel com {} registros (limite: {})", dados.size(), LIMITE_REGISTROS);
      throw new RelatorioException(
          String.format(
              "Quantidade de registros (%d) excede o limite permitido (%d). "
                  + "Utilize filtros para reduzir o volume de dados.",
              dados.size(), LIMITE_REGISTROS));
    }

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet(sheetName);

      // Estilos
      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle dateStyle = createDateStyle(workbook);

      // Criar linha de cabeçalho
      createHeaderRow(sheet, headers, headerStyle);

      // Criar linhas de dados
      createDataRows(sheet, dados, extractors, dateStyle);

      // Auto-ajustar largura das colunas
      autoSizeColumns(sheet, headers.length);

      workbook.write(outputStream);
      log.debug("Excel gerado com sucesso, tamanho: {} bytes", outputStream.size());
      return outputStream.toByteArray();

    } catch (IOException e) {
      log.error("Erro ao gerar Excel '{}': {}", sheetName, e.getMessage(), e);
      throw new RelatorioException("Erro ao gerar arquivo Excel", e);
    }
  }

  /** Cria estilo para células de cabeçalho. */
  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();

    // Fonte em negrito
    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);

    // Fundo verde (similar ao CSS dos templates HTML)
    style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    // Bordas
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    // Alinhamento
    style.setAlignment(HorizontalAlignment.CENTER);

    return style;
  }

  /** Cria estilo para células de data. */
  private CellStyle createDateStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  /** Cria a linha de cabeçalho. */
  private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }
  }

  /** Cria as linhas de dados. */
  private <T> void createDataRows(
      Sheet sheet, List<T> dados, Function<T, Object>[] extractors, CellStyle dateStyle) {

    int rowNum = 1;
    for (T item : dados) {
      Row row = sheet.createRow(rowNum++);
      for (int col = 0; col < extractors.length; col++) {
        Cell cell = row.createCell(col);
        Object value = extractors[col].apply(item);
        setCellValue(cell, value, dateStyle);
      }
    }
  }

  /** Define o valor da célula baseado no tipo do objeto. */
  private void setCellValue(Cell cell, Object value, CellStyle dateStyle) {
    if (value == null) {
      cell.setCellValue("");
    } else if (value instanceof String s) {
      cell.setCellValue(s);
    } else if (value instanceof Number n) {
      cell.setCellValue(n.doubleValue());
    } else if (value instanceof LocalDate date) {
      cell.setCellValue(date.format(DATE_FORMATTER));
      cell.setCellStyle(dateStyle);
    } else if (value instanceof BigDecimal bd) {
      cell.setCellValue(bd.doubleValue());
    } else {
      cell.setCellValue(value.toString());
    }
  }

  /** Auto-ajusta a largura das colunas baseado no conteúdo. */
  private void autoSizeColumns(Sheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      sheet.autoSizeColumn(i);
      // Adiciona um pouco de padding
      int currentWidth = sheet.getColumnWidth(i);
      sheet.setColumnWidth(i, currentWidth + 512);
    }
  }
}

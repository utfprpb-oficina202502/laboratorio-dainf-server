package br.com.utfpr.gerenciamento.server.mapper;

import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.modelTemplateEmail.ReservaTemplate;
import br.com.utfpr.gerenciamento.server.util.DateUtil;
import java.util.Collections;
import org.springframework.stereotype.Component;

/**
 * Mapper responsável por converter entidades Reserva em dados para templates de email.
 *
 * <p>Extrai a lógica de preparação de template do EmailEventListener, seguindo o princípio de
 * responsabilidade única (SRP). Facilita testes unitários e reutilização da lógica de mapeamento.
 *
 * @author Rodrigo Izidoro
 * @since 2025-12-07
 */
@Component
public class ReservaTemplateMapper {

  /**
   * Converte Reserva para ReservaTemplate para uso em templates de email.
   *
   * <p>Este método mapeia todos os campos necessários para renderização do template de confirmação
   * de reserva, com verificações defensivas para evitar NPEs.
   *
   * @param reserva Entidade Reserva com todas relações carregadas
   * @return ReservaTemplate contendo dados formatados para template FreeMarker
   */
  public ReservaTemplate toTemplateData(Reserva reserva) {
    ReservaTemplate template = new ReservaTemplate();

    // Dados do usuário que fez a reserva
    String usuarioNome = "N/A";
    if (reserva != null && reserva.getUsuario() != null) {
      usuarioNome = reserva.getUsuario().getNome();
    }
    template.setUsuario(usuarioNome);

    // Datas formatadas (dd/MM/yyyy - padrão brasileiro)
    String dtReserva = "";
    if (reserva != null && reserva.getDataReserva() != null) {
      dtReserva = DateUtil.parseLocalDateToString(reserva.getDataReserva());
    }
    template.setDtReserva(dtReserva);

    String dtRetirada = "";
    if (reserva != null && reserva.getDataRetirada() != null) {
      dtRetirada = DateUtil.parseLocalDateToString(reserva.getDataRetirada());
    }
    template.setDtRetirada(dtRetirada);

    // Lista de itens reservados
    if (reserva != null && reserva.getReservaItem() != null) {
      template.setReservaItem(reserva.getReservaItem());
    } else {
      template.setReservaItem(Collections.emptyList());
    }

    return template;
  }
}

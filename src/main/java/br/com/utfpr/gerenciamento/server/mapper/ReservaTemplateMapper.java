package br.com.utfpr.gerenciamento.server.mapper;

import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.modelTemplateEmail.ReservaTemplate;
import br.com.utfpr.gerenciamento.server.util.DateUtil;
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
   * de reserva.
   *
   * @param reserva Entidade Reserva com todas relações carregadas
   * @return ReservaTemplate contendo dados formatados para template FreeMarker
   */
  public ReservaTemplate toTemplateData(Reserva reserva) {
    ReservaTemplate template = new ReservaTemplate();

    // Dados do usuário que fez a reserva
    template.setUsuario(reserva.getUsuario().getNome());

    // Datas formatadas (dd/MM/yyyy - padrão brasileiro)
    template.setDtReserva(DateUtil.parseLocalDateToString(reserva.getDataReserva()));
    template.setDtRetirada(DateUtil.parseLocalDateToString(reserva.getDataRetirada()));

    // Lista de itens reservados
    template.setReservaItem(reserva.getReservaItem());

    return template;
  }
}

package br.com.utfpr.gerenciamento.server.event.reserva;

import br.com.utfpr.gerenciamento.server.event.email.EmailEvent;
import lombok.Getter;

/**
 * Evento publicado quando uma nova reserva é criada no sistema.
 *
 * <p>Dispara envio de email de confirmação de reserva usando template
 * templateConfirmacaoReserva.html.
 *
 * <p>Este evento garante que:
 *
 * <ul>
 *   <li>Email só é enviado se a transação de criação da reserva commit com sucesso
 *   <li>Falha no envio de email não causa rollback da criação da reserva
 *   <li>Reserva pode reenviar email de confirmação posteriormente se falhar
 * </ul>
 *
 * @author Rodrigo Izidoro
 * @since 2025-12-07
 */
@Getter
public class ReservaCriadaEvent extends EmailEvent {

  private final Long reservaId;

  /**
   * Cria evento de reserva criada.
   *
   * @param source Service que publicou o evento
   * @param reservaId ID da reserva criada
   * @param recipient Email do usuário que fez a reserva
   */
  public ReservaCriadaEvent(Object source, Long reservaId, String recipient) {
    super(
        source,
        recipient,
        "Confirmação de Reserva de Materiais",
        "templateConfirmacaoReserva.html");
    this.reservaId = reservaId;
  }
}

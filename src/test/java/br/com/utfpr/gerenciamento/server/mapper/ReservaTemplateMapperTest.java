package br.com.utfpr.gerenciamento.server.mapper;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.ReservaItem;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.model.modelTemplateEmail.ReservaTemplate;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservaTemplateMapperTest {

  private ReservaTemplateMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ReservaTemplateMapper();
  }

  @Test
  void testToTemplateDataWithValidReserva() {
    // Configurar usuário
    Usuario usuario = new Usuario();
    usuario.setNome("João Silva");

    // Configurar reserva
    Reserva reserva = new Reserva();
    reserva.setUsuario(usuario);
    reserva.setDataReserva(LocalDate.of(2025, 12, 1));
    reserva.setDataRetirada(LocalDate.of(2025, 12, 5));
    reserva.setReservaItem(List.of(new ReservaItem()));

    // Executar mapeamento
    ReservaTemplate template = mapper.toTemplateData(reserva);

    // Verificar
    assertNotNull(template);
    assertEquals("João Silva", template.getUsuario());
    assertEquals("01/12/2025", template.getDtReserva());
    assertEquals("05/12/2025", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertEquals(1, template.getReservaItem().size());
  }

  @Test
  void testToTemplateDataWithNullReserva() {
    ReservaTemplate template = mapper.toTemplateData(null);

    assertNotNull(template);
    assertEquals("N/A", template.getUsuario());
    assertEquals("", template.getDtReserva());
    assertEquals("", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertTrue(template.getReservaItem().isEmpty());
  }

  @Test
  void testToTemplateDataWithNullUsuario() {
    Reserva reserva = new Reserva();
    reserva.setUsuario(null);
    reserva.setDataReserva(LocalDate.of(2025, 12, 1));
    reserva.setDataRetirada(LocalDate.of(2025, 12, 5));
    reserva.setReservaItem(List.of(new ReservaItem()));

    ReservaTemplate template = mapper.toTemplateData(reserva);

    assertNotNull(template);
    assertEquals("N/A", template.getUsuario());
    assertEquals("01/12/2025", template.getDtReserva());
    assertEquals("05/12/2025", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertEquals(1, template.getReservaItem().size());
  }

  @Test
  void testToTemplateDataWithNullDataReserva() {
    Usuario usuario = new Usuario();
    usuario.setNome("João Silva");

    Reserva reserva = new Reserva();
    reserva.setUsuario(usuario);
    reserva.setDataReserva(null);
    reserva.setDataRetirada(LocalDate.of(2025, 12, 5));
    reserva.setReservaItem(List.of(new ReservaItem()));

    ReservaTemplate template = mapper.toTemplateData(reserva);

    assertNotNull(template);
    assertEquals("João Silva", template.getUsuario());
    assertEquals("", template.getDtReserva());
    assertEquals("05/12/2025", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertEquals(1, template.getReservaItem().size());
  }

  @Test
  void testToTemplateDataWithNullDataRetirada() {
    Usuario usuario = new Usuario();
    usuario.setNome("João Silva");

    Reserva reserva = new Reserva();
    reserva.setUsuario(usuario);
    reserva.setDataReserva(LocalDate.of(2025, 12, 1));
    reserva.setDataRetirada(null);
    reserva.setReservaItem(List.of(new ReservaItem()));

    ReservaTemplate template = mapper.toTemplateData(reserva);

    assertNotNull(template);
    assertEquals("João Silva", template.getUsuario());
    assertEquals("01/12/2025", template.getDtReserva());
    assertEquals("", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertEquals(1, template.getReservaItem().size());
  }

  @Test
  void testToTemplateDataWithNullReservaItem() {
    Usuario usuario = new Usuario();
    usuario.setNome("João Silva");

    Reserva reserva = new Reserva();
    reserva.setUsuario(usuario);
    reserva.setDataReserva(LocalDate.of(2025, 12, 1));
    reserva.setDataRetirada(LocalDate.of(2025, 12, 5));
    reserva.setReservaItem(null);

    ReservaTemplate template = mapper.toTemplateData(reserva);

    assertNotNull(template);
    assertEquals("João Silva", template.getUsuario());
    assertEquals("01/12/2025", template.getDtReserva());
    assertEquals("05/12/2025", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertTrue(template.getReservaItem().isEmpty());
  }

  @Test
  void testToTemplateDataWithAllNulls() {
    Reserva reserva = new Reserva();
    reserva.setUsuario(null);
    reserva.setDataReserva(null);
    reserva.setDataRetirada(null);
    reserva.setReservaItem(null);

    ReservaTemplate template = mapper.toTemplateData(reserva);

    assertNotNull(template);
    assertEquals("N/A", template.getUsuario());
    assertEquals("", template.getDtReserva());
    assertEquals("", template.getDtRetirada());
    assertNotNull(template.getReservaItem());
    assertTrue(template.getReservaItem().isEmpty());
  }
}

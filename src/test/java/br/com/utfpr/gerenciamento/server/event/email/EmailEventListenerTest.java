package br.com.utfpr.gerenciamento.server.event.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.utfpr.gerenciamento.server.enumeration.FormatoRelatorio;
import br.com.utfpr.gerenciamento.server.event.emprestimo.*;
import br.com.utfpr.gerenciamento.server.event.item.EstoqueMinNotificacaoEvent;
import br.com.utfpr.gerenciamento.server.event.nadaConsta.*;
import br.com.utfpr.gerenciamento.server.event.reserva.ReservaCriadaEvent;
import br.com.utfpr.gerenciamento.server.exception.EntityNotFoundException;
import br.com.utfpr.gerenciamento.server.mapper.EmprestimoTemplateMapper;
import br.com.utfpr.gerenciamento.server.mapper.ReservaTemplateMapper;
import br.com.utfpr.gerenciamento.server.model.Email;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.Reserva;
import br.com.utfpr.gerenciamento.server.model.modelTemplateEmail.ReservaTemplate;
import br.com.utfpr.gerenciamento.server.repository.EmprestimoRepository;
import br.com.utfpr.gerenciamento.server.repository.ReservaRepository;
import br.com.utfpr.gerenciamento.server.service.EmailService;
import br.com.utfpr.gerenciamento.server.service.report.RelatorioGeneratorService;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.springframework.mail.MailException;

class EmailEventListenerTest {
  @InjectMocks EmailEventListener listener;
  @Mock EmailService emailService;
  @Mock EmprestimoRepository emprestimoRepository;
  @Mock EmprestimoTemplateMapper templateMapper;
  @Mock RelatorioGeneratorService relatorioGeneratorService;
  @Mock ReservaRepository reservaRepository;
  @Mock ReservaTemplateMapper reservaTemplateMapper;
  AutoCloseable mocks;

  @BeforeEach
  void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    Field field = EmailEventListener.class.getDeclaredField("emailFrom");
    field.setAccessible(true);
    field.set(listener, "from@email.com");
  }

  static List<Object[]> provideEmprestimoFinalizadoEventParams() {
    Emprestimo emp = mock(Emprestimo.class);
    Map<String, Object> templateData = new HashMap<>();
    return List.of(
        new Object[] {1L, "to@email.com", "subject", "template", emp, templateData, true},
        new Object[] {2L, "to@email.com", "subject", "template", null, null, false});
  }

  @ParameterizedTest
  @MethodSource("provideEmprestimoFinalizadoEventParams")
  void testHandleEmprestimoFinalizadoEventParametrized(
      Long id,
      String recipient,
      String subject,
      String template,
      Emprestimo emp,
      Map<String, Object> templateData,
      boolean shouldSend) {
    EmprestimoFinalizadoEvent event = mock(EmprestimoFinalizadoEvent.class);
    when(event.getEmprestimoId()).thenReturn(id);
    when(event.getRecipient()).thenReturn(recipient);
    when(event.getSubject()).thenReturn(subject);
    when(event.getTemplateName()).thenReturn(template);
    if (emp != null) {
      when(emprestimoRepository.findEmprestimoByIdWithRelations(id)).thenReturn(Optional.of(emp));
      when(templateMapper.toTemplateData(emp)).thenReturn(templateData);
      doNothing()
          .when(emailService)
          .sendEmailWithTemplate(templateData, recipient, subject, template);
    } else {
      when(emprestimoRepository.findEmprestimoByIdWithRelations(id)).thenReturn(Optional.empty());
    }
    listener.handleEmailEvent(event);
    if (shouldSend) {
      verify(emailService).sendEmailWithTemplate(templateData, recipient, subject, template);
    } else {
      verify(emailService, never()).sendEmailWithTemplate(any(), any(), any(), any());
    }
  }

  @Test
  void testHandleEstoqueMinNotificacaoEventSuccess() throws Exception {
    EstoqueMinNotificacaoEvent event = mock(EstoqueMinNotificacaoEvent.class);
    when(event.getRecipient()).thenReturn("to@email.com");
    when(event.getSubject()).thenReturn("subject");
    when(event.getTemplateName()).thenReturn("template");
    byte[] pdf = new byte[] {1, 2, 3};
    when(relatorioGeneratorService.gerarItensQtdeMinima(FormatoRelatorio.PDF)).thenReturn(pdf);
    when(emailService.buildTemplateEmail(null, "template")).thenReturn("conteudo");
    doNothing().when(emailService).enviar(any(Email.class));
    listener.handleEstoqueMinNotificacaoEvent(event);
    verify(emailService).enviar(any(Email.class));
  }

  @Test
  void testHandleEstoqueMinNotificacaoEventException() throws Exception {
    EstoqueMinNotificacaoEvent event = mock(EstoqueMinNotificacaoEvent.class);
    when(event.getRecipient()).thenReturn("to@email.com");
    when(event.getSubject()).thenReturn("subject");
    when(event.getTemplateName()).thenReturn("template");
    when(relatorioGeneratorService.gerarItensQtdeMinima(FormatoRelatorio.PDF))
        .thenThrow(new RuntimeException("fail"));

    // Não deve lançar exceção - método trata erro internamente
    assertDoesNotThrow(() -> listener.handleEstoqueMinNotificacaoEvent(event));
    // Email não deve ser enviado quando relatório falha
    verify(emailService, never()).enviar(any(Email.class));
  }

  @Test
  void testHandleNadaConstaEmitidoEventSuccess() {
    Map<String, Object> templateData = new HashMap<>();
    NadaConstaEmitidoEvent event = new NadaConstaEmitidoEvent(this, "to@email.com", templateData);
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Declaração Nada Consta",
            "nada-consta-declaracao.html"); // 4 args
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Declaração Nada Consta",
            "nada-consta-declaracao.html"); // 4 args
  }

  @Test
  void testHandleNadaConstaEmitidoEventComCC() {
    Map<String, Object> templateData = new HashMap<>();
    String cc = "cc@email.com";
    NadaConstaEmitidoEvent event =
        new NadaConstaEmitidoEvent(this, "to@email.com", templateData, cc);
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Declaração Nada Consta",
            "nada-consta-declaracao.html",
            cc);
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Declaração Nada Consta",
            "nada-consta-declaracao.html",
            cc);
  }

  @Test
  void testHandleNadaConstaPendenciasEventSuccess() {
    Map<String, Object> templateData = new HashMap<>();
    NadaConstaPendenciasEvent event =
        new NadaConstaPendenciasEvent(this, "to@email.com", templateData);
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Pendências de Empréstimos",
            "pendencias-emprestimos.html");
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Pendências de Empréstimos",
            "pendencias-emprestimos.html");
  }

  @Test
  void testProcessEmailWithTemplateSuccess() throws ReflectiveOperationException {
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate("data", "to@email.com", "subject", "template");
    Method m = getProcessEmailWithTemplateMethod();
    m.invoke(listener, "data", "to@email.com", "subject", "template", null);
    verify(emailService).sendEmailWithTemplate("data", "to@email.com", "subject", "template");
  }

  @Test
  void testProcessEmailWithTemplateMailException() throws ReflectiveOperationException {
    doThrow(new MailException("fail") {})
        .when(emailService)
        .sendEmailWithTemplate(any(), any(), any(), any());
    Method m = getProcessEmailWithTemplateMethod();

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> m.invoke(listener, "data", "to@email.com", "subject", "template", null));
    assertInstanceOf(MailException.class, exception.getCause());
  }

  @Test
  void testProcessEmailWithTemplateEntityNotFoundException() throws ReflectiveOperationException {
    doThrow(new EntityNotFoundException("fail"))
        .when(emailService)
        .sendEmailWithTemplate(any(), any(), any(), any());
    Method m = getProcessEmailWithTemplateMethod();

    // EntityNotFoundException é tratada internamente - não deve propagar
    assertDoesNotThrow(
        () -> m.invoke(listener, "data", "to@email.com", "subject", "template", null));
  }

  @Test
  void testProcessEmailWithTemplateIllegalArgumentException() throws ReflectiveOperationException {
    doThrow(new IllegalArgumentException("fail"))
        .when(emailService)
        .sendEmailWithTemplate(any(), any(), any(), any());
    Method m = getProcessEmailWithTemplateMethod();

    // IllegalArgumentException é tratada internamente - não deve propagar
    assertDoesNotThrow(
        () -> m.invoke(listener, "data", "to@email.com", "subject", "template", null));
  }

  private Method getProcessEmailWithTemplateMethod() throws ReflectiveOperationException {
    Method m =
        EmailEventListener.class.getDeclaredMethod(
            "processEmailWithTemplate",
            Object.class,
            String.class,
            String.class,
            String.class,
            String.class);
    m.setAccessible(true);
    return m;
  }

  @Test
  void testProcessEmailWithAttachmentSuccess() throws Exception {
    Email email = criarEmailTeste();
    doNothing().when(emailService).enviar(email);
    Method m = getProcessEmailWithAttachmentMethod();
    m.invoke(listener, email, "to@email.com", "subject");
    verify(emailService).enviar(email);
  }

  @Test
  void testProcessEmailWithAttachmentMailException() throws Exception {
    Email email = criarEmailTeste();
    doThrow(new MailException("fail") {}).when(emailService).enviar(email);
    Method m = getProcessEmailWithAttachmentMethod();

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> m.invoke(listener, email, "to@email.com", "subject"));
    assertInstanceOf(MailException.class, exception.getCause());
  }

  @Test
  void testProcessEmailWithAttachmentGenericException() throws Exception {
    Email email = criarEmailTeste();
    doThrow(new RuntimeException("fail")).when(emailService).enviar(email);
    Method m = getProcessEmailWithAttachmentMethod();

    // RuntimeException genérica é tratada internamente - não deve propagar
    assertDoesNotThrow(() -> m.invoke(listener, email, "to@email.com", "subject"));
  }

  private Method getProcessEmailWithAttachmentMethod() throws ReflectiveOperationException {
    Method m =
        EmailEventListener.class.getDeclaredMethod(
            "processEmailWithAttachment", Email.class, String.class, String.class);
    m.setAccessible(true);
    return m;
  }

  private Email criarEmailTeste() {
    return Email.builder()
        .para("to@email.com")
        .de("from@email.com")
        .titulo("subject")
        .conteudo("body")
        .build();
  }

  @Test
  void testPrepareTemplateDataForEventUnsupportedType() throws ReflectiveOperationException {
    EmailEvent event = mock(EmailEvent.class);
    Method m =
        EmailEventListener.class.getDeclaredMethod("prepareTemplateDataForEvent", EmailEvent.class);
    m.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, () -> m.invoke(listener, event));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testPrepareEmprestimoTemplateDataSuccess() throws ReflectiveOperationException {
    Emprestimo emp = mock(Emprestimo.class);
    when(emprestimoRepository.findEmprestimoByIdWithRelations(10L)).thenReturn(Optional.of(emp));
    Map<String, Object> templateData = new HashMap<>();
    when(templateMapper.toTemplateData(emp)).thenReturn(templateData);
    Method m =
        EmailEventListener.class.getDeclaredMethod("prepareEmprestimoTemplateData", Long.class);
    m.setAccessible(true);
    Map<String, Object> result = (Map<String, Object>) m.invoke(listener, 10L);
    assertEquals(templateData, result);
  }

  @Test
  void testPrepareEmprestimoTemplateDataEntityNotFound() throws ReflectiveOperationException {
    when(emprestimoRepository.findEmprestimoByIdWithRelations(11L)).thenReturn(Optional.empty());
    Method m =
        EmailEventListener.class.getDeclaredMethod("prepareEmprestimoTemplateData", Long.class);
    m.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, () -> m.invoke(listener, 11L));
    assertInstanceOf(EntityNotFoundException.class, exception.getCause());
  }

  @Test
  void testHandleEmprestimoFinalizadoEventComCC() {
    Emprestimo emp = mock(Emprestimo.class);
    when(emprestimoRepository.findEmprestimoByIdWithRelations(10L)).thenReturn(Optional.of(emp));
    Map<String, Object> templateData = new HashMap<>();
    when(templateMapper.toTemplateData(emp)).thenReturn(templateData);
    String cc = "cc@email.com";
    EmprestimoFinalizadoEvent event =
        new EmprestimoFinalizadoEvent(this, 10L, "to@email.com", true, cc);
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Empréstimo",
            "templateConfirmacaoEmprestimo.html",
            cc);
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Empréstimo",
            "templateConfirmacaoEmprestimo.html",
            cc);
  }

  @Test
  void testHandleEmprestimoFinalizadoEventSemCC() {
    Emprestimo emp = mock(Emprestimo.class);
    when(emprestimoRepository.findEmprestimoByIdWithRelations(11L)).thenReturn(Optional.of(emp));
    Map<String, Object> templateData = new HashMap<>();
    when(templateMapper.toTemplateData(emp)).thenReturn(templateData);
    EmprestimoFinalizadoEvent event =
        new EmprestimoFinalizadoEvent(this, 11L, "to@email.com", false);
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Empréstimo",
            "templateConfirmacaoFinalizacaoEmprestimo.html");
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Empréstimo",
            "templateConfirmacaoFinalizacaoEmprestimo.html");
  }

  @Test
  void testHandleReservaCriadaEventSuccess() {
    Reserva reserva = mock(Reserva.class);
    when(reservaRepository.findReservaByIdWithRelations(1L)).thenReturn(Optional.of(reserva));
    ReservaTemplate templateData = new ReservaTemplate();
    when(reservaTemplateMapper.toTemplateData(reserva)).thenReturn(templateData);
    ReservaCriadaEvent event = new ReservaCriadaEvent(this, 1L, "to@email.com");
    doNothing()
        .when(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Reserva de Materiais",
            "templateConfirmacaoReserva.html");
    listener.handleEmailEvent(event);
    verify(emailService)
        .sendEmailWithTemplate(
            templateData,
            "to@email.com",
            "Confirmação de Reserva de Materiais",
            "templateConfirmacaoReserva.html");
  }

  @Test
  void testPrepareReservaTemplateDataSuccess() throws Exception {
    Reserva reserva = mock(Reserva.class);
    when(reservaRepository.findReservaByIdWithRelations(1L)).thenReturn(Optional.of(reserva));
    ReservaTemplate templateData = new ReservaTemplate();
    when(reservaTemplateMapper.toTemplateData(reserva)).thenReturn(templateData);
    Method m = EmailEventListener.class.getDeclaredMethod("prepareReservaTemplateData", Long.class);
    m.setAccessible(true);
    ReservaTemplate result = (ReservaTemplate) m.invoke(listener, 1L);
    assertEquals(templateData, result);
  }

  @Test
  void testPrepareReservaTemplateDataEntityNotFound() throws Exception {
    when(reservaRepository.findReservaByIdWithRelations(2L)).thenReturn(Optional.empty());
    Method m = EmailEventListener.class.getDeclaredMethod("prepareReservaTemplateData", Long.class);
    m.setAccessible(true);
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          try {
            m.invoke(listener, 2L);
          } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof EntityNotFoundException ex) throw ex;
            throw new RuntimeException(e);
          }
        });
  }
}

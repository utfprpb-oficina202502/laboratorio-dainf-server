package br.com.utfpr.gerenciamento.server.component;

import br.com.utfpr.gerenciamento.server.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UsuarioScheduled {

  private final UsuarioService usuarioService;

  public UsuarioScheduled(@Lazy UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @Scheduled(cron = "0 0 1 * * ?") // Executa diariamente às 01:00
  public void deleteUnverifiedUsers() {
    log.info("Iniciando limpeza de usuários não verificados");
    try {
      usuarioService.deleteUnverifiedUsers();
      log.info("Limpeza de usuários não verificados concluída com sucesso");
    } catch (Exception e) {
      log.error("Erro durante limpeza de usuários não verificados", e);
    }
  }
}

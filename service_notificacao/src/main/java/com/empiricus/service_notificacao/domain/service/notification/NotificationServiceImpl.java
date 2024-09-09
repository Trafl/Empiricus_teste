package com.empiricus.service_notificacao.domain.service.notification;

import com.empiricus.service_notificacao.domain.model.Email;
import com.empiricus.service_notificacao.domain.model.Usuario;
import com.empiricus.service_notificacao.domain.service.feing.EmailFeignService;
import com.empiricus.service_notificacao.domain.service.feing.UsuarioFeignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final UsuarioFeignService usuarioFeignService;
    private final EmailFeignService emailFeignService;

    @Override
    @Async
    @KafkaListener(topics = "email-created")
    public void notifyAdminsEmailCreated(Email email) throws ExecutionException, InterruptedException {
        log.info("[{}] - [NotificationServiceImpl] - Executando notifyAdminsEmailCreated()", LocalDateTime.now());

        CompletableFuture<Usuario> usuarioFuture = CompletableFuture.supplyAsync(
                () -> usuarioFeignService.getOneUsuario(email.getUsuario_id()));

        var usuario = usuarioFuture.get();

        String subject = String.format("O email %s foi criado/alterado para o usuário de CPF %s", email.getEmail(), usuario.getCpf());
        String body = String.format(
                "Prezado Administrador,%n%n" +
                        "Informamos que um novo email foi criado no sistema.%n%n" +
                        "Detalhes do Email:%n" +
                        "- Endereço de Email: %s%n" +
                        "- CPF do Usuário: %s%n%n" +
                        "Atenciosamente,%n" +
                        "Sistema de Gestão de Emails",
                email.getEmail(),
                usuario.getCpf()
        );

        sendEmailsForAdmins(subject, body);
    }

    @Override
    @Async
    @KafkaListener(topics = "email-deleted")
    public void notifyAdminsEmailDeleted(Email email) throws ExecutionException, InterruptedException {
        log.info("[{}] - [NotificationServiceImpl] - Executando notifyAdminsEmailDeleted()", LocalDateTime.now());

        CompletableFuture<Usuario> usuarioFuture = CompletableFuture.supplyAsync(
                ()-> usuarioFeignService.getOneUsuario(email.getUsuario_id()));

        var usuario = usuarioFuture.get();

        String subject = String.format("O email %s foi deletado para o usuário de CPF %s", email.getEmail(), usuario.getCpf());
        String body = String.format(
                "Prezado Administrador,%n%n" +
                        "Informamos que um email foi deletado do sistema.%n%n" +
                        "Detalhes do Email Deletado:%n" +
                        "- CPF do Usuário: %s%n%n" +
                        "Data da Deleção: %s%n%n" +
                        "Atenciosamente,%n" +
                        "Sistema de Gestão de Emails",
                usuario.getCpf(),
                LocalDateTime.now()
        );

        sendEmailsForAdmins(subject, body);
    }

    @Async
    private void sendEmailsForAdmins(String subject, String body) throws ExecutionException, InterruptedException {
        log.info("[{}] - [NotificationServiceImpl] - Executando sendEmailsForAdmins()", LocalDateTime.now());

        var adminEmails = findAdminEmails();

        for (String adminEmail : adminEmails) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(adminEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);

                log.info("[{}] - [NotificationServiceImpl] - Email enviado para o administrador: {}", LocalDateTime.now(), adminEmail);
            } catch (Exception e) {
                log.error("[{}] - [NotificationServiceImpl] - Erro ao enviar email para o administrador: {}", LocalDateTime.now(), adminEmail, e);
            }
        }
    }

    @Async
    private List<String> findAdminEmails() throws ExecutionException, InterruptedException {
      CompletableFuture emailsFuture = CompletableFuture.supplyAsync(
              () -> emailFeignService.getAdmins());

      var emails = (List<String>) emailsFuture.get();
      return emails;
    }
}

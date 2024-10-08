package com.empiricus.service_notificacao.domain.service.notification;

import com.empiricus.service_notificacao.domain.model.Email;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface NotificationService {

    void notifyAdminsEmailCreated(Email email);

    void notifyAdminsEmailDeleted(Email email);
}

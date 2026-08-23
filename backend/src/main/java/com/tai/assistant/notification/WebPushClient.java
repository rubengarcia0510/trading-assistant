package com.tai.assistant.notification;

import nl.martijndwars.webpush.Notification;
import org.apache.http.HttpResponse;

/**
 * Interfaz propia de TAI para encapsular las operaciones de Web Push.
 * Aísla la dependencia de la librería externa nl.martijndwars.webpush.
 */
public interface WebPushClient {

    /**
     * Envía una notificación Web Push.
     *
     * @param notification la notificación a enviar
     * @return la respuesta HTTP del servidor
     * @throws Exception si ocurre un error durante el envío
     */
    HttpResponse send(Notification notification) throws Exception;
}

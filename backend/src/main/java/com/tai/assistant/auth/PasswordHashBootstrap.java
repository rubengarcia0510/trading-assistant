package com.tai.assistant.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ayuda única para generar el hash BCrypt de tu contraseña, sin necesitar
 * herramientas externas (openssl, sitios web, etc.).
 *
 * Uso (una sola vez):
 *   1. export TAI_AUTH_PASSWORD_PLAIN="tu-contraseña-elegida"
 *   2. mvn spring-boot:run  -> en la consola va a imprimir el hash BCrypt
 *   3. Copiá ese hash a la variable TAI_AUTH_PASSWORD_HASH (la definitiva)
 *   4. Sacá TAI_AUTH_PASSWORD_PLAIN de tus variables — ya no hace falta,
 *      y no querés dejar la contraseña en texto plano dando vueltas.
 */
@Component
public class PasswordHashBootstrap implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashBootstrap(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String plain = System.getenv("TAI_AUTH_PASSWORD_PLAIN");
        if (plain != null && !plain.isBlank()) {
            String hash = passwordEncoder.encode(plain);
            System.out.println();
            System.out.println("=================================================================");
            System.out.println("[PasswordHashBootstrap] Hash BCrypt generado:");
            System.out.println(hash);
            System.out.println();
            System.out.println("Copiá ese valor a TAI_AUTH_PASSWORD_HASH, y sacá TAI_AUTH_PASSWORD_PLAIN");
            System.out.println("de tus variables de entorno — ya cumplió su función.");
            System.out.println("=================================================================");
            System.out.println();
        }
    }
}

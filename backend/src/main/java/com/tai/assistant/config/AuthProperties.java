package com.tai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales del ÚNICO usuario de la app, configuradas por variables de entorno.
 * No hay endpoint de registro — este proyecto es de un solo usuario permanente
 * (ver Discovery MPDD-8/TAI-17: sin sentido tener alta pública para un caso de uso personal).
 */
@ConfigurationProperties(prefix = "tai.auth")
public class AuthProperties {

    private String username;
    /** Hash BCrypt de la contraseña — nunca la contraseña en texto plano. */
    private String passwordHash;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isConfigured() {
        return username != null && !username.isBlank() && passwordHash != null && !passwordHash.isBlank();
    }
}

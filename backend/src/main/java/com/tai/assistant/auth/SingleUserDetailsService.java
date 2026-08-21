package com.tai.assistant.auth;

import com.tai.assistant.config.AuthProperties;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * No hay base de usuarios — solo existe UNO, configurado por variables de entorno
 * (ver AuthProperties). Sin endpoint de registro, sin colección de usuarios en Mongo.
 */
@Service
public class SingleUserDetailsService implements UserDetailsService {

    private final AuthProperties props;

    public SingleUserDetailsService(AuthProperties props) {
        this.props = props;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!props.isConfigured() || !props.getUsername().equals(username)) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return User.withUsername(props.getUsername())
                .password(props.getPasswordHash())
                .authorities("USER")
                .build();
    }
}

package tn.esprit.autoloc.web.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * spring-boot-starter-security est présent depuis l'Atelier 1 : sans cette configuration, toute
 * requête reçoit 401 (authentification) et les POST/PUT/DELETE 403 (CSRF).
 * Provisoire : l'API reste ouverte tant que l'authentification n'est pas traitée.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                // API REST sans session ni cookie : pas de jeton CSRF à vérifier
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}

package com.vidacruz.triage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String HEADER = "X-API-KEY";

    private static final String CUERPO_401 = """
            {"status":401,"error":"No autorizado","detalles":["API key ausente o inválida. Envíe el header X-API-KEY"]}""";

    @Bean
    public SecurityWebFilterChain filtro(ServerHttpSecurity http,
                                         @Value("${triage.security.api-key}") String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("triage.security.api-key no puede estar vacía");
        }

        ServerAuthenticationEntryPoint respuesta401 = (exchange, error) -> {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] cuerpo = CUERPO_401.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(cuerpo)));
        };

        AuthenticationWebFilter filtroApiKey = new AuthenticationWebFilter(administrador(apiKey));
        filtroApiKey.setServerAuthenticationConverter(exchange -> {
            String recibida = exchange.getRequest().getHeaders().getFirst(HEADER);
            if (recibida == null || recibida.isBlank()) {
                return Mono.empty();   // sin header: lo rechaza más abajo la regla "authenticated()"
            }
            return Mono.just(new UsernamePasswordAuthenticationToken("api-key", recibida));
        });
        filtroApiKey.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(respuesta401));

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())  // sin sesiones
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(reglas -> reglas
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(respuesta401))
                .addFilterAt(filtroApiKey, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    // Compara la key recibida con la esperada en tiempo constante
    private ReactiveAuthenticationManager administrador(String esperada) {
        byte[] esperadaBytes = esperada.getBytes(StandardCharsets.UTF_8);

        return autenticacion -> {
            byte[] recibidaBytes = String.valueOf(autenticacion.getCredentials())
                    .getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(esperadaBytes, recibidaBytes)) {
                return Mono.error(new BadCredentialsException("API key inválida"));
            }
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    "cliente-api", null, List.of(new SimpleGrantedAuthority("ROLE_API"))));
        };
    }
}
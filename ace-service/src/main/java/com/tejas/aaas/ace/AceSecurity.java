package com.tejas.aaas.ace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
/** Rejects ID tokens, wrong issuers and tokens for unrelated app clients. */
@Configuration
public class AceSecurity {
    @Bean @Profile("!live")
    SecurityFilterChain closed(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.anyRequest().denyAll()).build();
    }
    @Bean @Profile("live")
    SecurityFilterChain live(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().authenticated()).oauth2ResourceServer(o -> o.jwt(j -> {})).build();
    }
    @Bean @Profile("live")
    JwtDecoder decoder(@Value("${cognito.issuer-uri}") String issuer, @Value("${cognito.client-id}") String client) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), clientValidator(client)));
        return decoder;
    }
    static OAuth2TokenValidator<Jwt> clientValidator(String client) {
        return jwt -> "access".equals(jwt.getClaimAsString("token_use")) && client.equals(jwt.getClaimAsString("client_id"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }
}

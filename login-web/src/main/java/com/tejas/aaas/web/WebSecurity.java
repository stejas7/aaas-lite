package com.tejas.aaas.web;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
/** Session-based browser security: CSRF stays enabled and tokens never enter browser storage. */
@Configuration
public class WebSecurity {
    @Bean @Profile("!live")
    SecurityFilterChain preview(HttpSecurity http) throws Exception {
        return common(http).authorizeHttpRequests(a -> a.requestMatchers("/", "/login", "/assets/**", "/error").permitAll()
            .anyRequest().denyAll()).build();
    }
    @Bean @Profile("live")
    SecurityFilterChain live(HttpSecurity http, ClientRegistrationRepository clients,
            @Value("${cognito.identity-provider:}") String provider) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(builder -> {
            OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
            if (!provider.isBlank()) builder.additionalParameters(p -> p.put("identity_provider", provider));
        });
        return common(http).authorizeHttpRequests(a -> a.requestMatchers("/", "/login", "/assets/**", "/error").permitAll()
            .anyRequest().authenticated())
            .oauth2Login(o -> o.loginPage("/login").authorizationEndpoint(e -> e.authorizationRequestResolver(resolver))
                .defaultSuccessUrl("/account", true).failureUrl("/login?error"))
            .logout(l -> l.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
    private HttpSecurity common(HttpSecurity http) throws Exception {
        return http.headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
            "default-src 'self'; style-src 'self'; img-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
    }
}

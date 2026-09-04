package com.tejas.aaas.ace;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
class TokenValidationTest {
    Jwt jwt(String use, String client) {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject("subject")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .claim("token_use", use).claim("client_id", client).build();
    }
    @Test void acceptsCorrectAccessTokenClaims() { assertFalse(AceSecurity.clientValidator("app").validate(jwt("access", "app")).hasErrors()); }
    @Test void rejectsIdTokens() { assertTrue(AceSecurity.clientValidator("app").validate(jwt("id", "app")).hasErrors()); }
    @Test void rejectsOtherClient() { assertTrue(AceSecurity.clientValidator("app").validate(jwt("access", "other")).hasErrors()); }
}

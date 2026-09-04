package com.tejas.aaas.ace;
import java.util.Map;
import com.tejas.aaas.identity.Triplet;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
/** All identities come from the validated access token, never a request body subject. */
@RestController @Profile("live")
public class ApiController {
    private final AccessService access;
    private final RegistrationService registration;
    private final CredentialAuthority cognito;
    public ApiController(AccessService access, RegistrationService registration, CredentialAuthority cognito) {
        this.access = access; this.registration = registration; this.cognito = cognito;
    }
    @GetMapping("/api/registration/status")
    ResponseEntity<?> status(@AuthenticationPrincipal Jwt jwt) {
        var account = cognito.current(jwt.getTokenValue());
        if (!jwt.getSubject().equals(account.subject())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status", account.status()));
    }
    @PostMapping("/api/registration/resolve")
    ResponseEntity<?> resolve(@AuthenticationPrincipal Jwt jwt, @RequestBody Triplet triplet) {
        registration.resolve(jwt.getTokenValue(), jwt.getSubject(), triplet);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status", "linked_active"));
    }
    @GetMapping("/api/protected/access")
    ResponseEntity<?> access(@AuthenticationPrincipal Jwt jwt) {
        access.requireAccess(jwt.getTokenValue(), jwt.getSubject());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("allowed", true));
    }
}

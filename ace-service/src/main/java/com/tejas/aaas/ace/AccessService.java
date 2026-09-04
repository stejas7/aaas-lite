package com.tejas.aaas.ace;
import com.tejas.aaas.identity.AccessPolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
/** Single protected-access gate; always obtains a fresh ODL linkage decision. */
@Service @Profile("live")
public class AccessService {
    private final CredentialAuthority cognito;
    private final IdentityAuthority odl;
    public AccessService(CredentialAuthority cognito, IdentityAuthority odl) { this.cognito = cognito; this.odl = odl; }
    public void requireAccess(String token, String subject) {
        var account = cognito.current(token);
        if (!subject.equals(account.subject()) || !"linked_active".equals(account.status())) throw new ResponseStatusException(FORBIDDEN);
        var link = odl.link(subject);
        if (link == null || !AccessPolicy.permits(account.status(), subject, link.subject(), link.active()))
            throw new ResponseStatusException(FORBIDDEN);
    }
}

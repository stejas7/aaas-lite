package com.tejas.aaas.ace;
import java.util.Set;
import com.tejas.aaas.identity.Triplet;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
/** OIDC-only resolution orchestration; ODL must serialize and own attempts per subject. */
@Service @Profile("live")
public class RegistrationService {
    private final CredentialAuthority cognito;
    private final IdentityAuthority odl;
    private final boolean enabled;
    public RegistrationService(CredentialAuthority cognito, IdentityAuthority odl,
        @Value("${aaas.registration-enabled:false}") boolean enabled) {
        this.cognito = cognito; this.odl = odl; this.enabled = enabled;
    }
    public void resolve(String token, String subject, Triplet triplet) {
        if (!enabled) throw new ResponseStatusException(SERVICE_UNAVAILABLE);
        triplet.validate();
        var account = cognito.current(token);
        if (!subject.equals(account.subject()) || !account.federated() || !Set.of(
            "authenticated_unverified", "profile_complete_pending_resolution", "resolution_timeout").contains(account.status()))
            throw new ResponseStatusException(CONFLICT);
        cognito.status(account, "profile_complete_pending_resolution");
        IdentityAuthority.Resolution result;
        try { result = odl.resolve(subject, triplet); }
        catch (ResourceAccessException e) {
            cognito.status(account, "resolution_timeout");
            throw new ResponseStatusException(GATEWAY_TIMEOUT);
        }
        if (result == null || result.resolutionId() == null || result.resolutionId().isBlank() || result.matchCount() < 0)
            throw new ResponseStatusException(BAD_GATEWAY);
        if (result.correlationRequired()) throw new ResponseStatusException(CONFLICT);
        if (result.matchCount() == 1) {
            // ODL must atomically verify that this resolution belongs to this subject and is still eligible.
            odl.activate(subject, result.resolutionId());
            cognito.status(account, "linked_active");
        } else {
            String outcome = result.matchCount() == 0 ? "rejected_no_identity" : "rejected_conflict";
            // Preserve the authoritative event before deleting the failed new federated account.
            odl.recordRejection(subject, result.resolutionId(), outcome);
            cognito.deleteNewFederatedAccount(account);
            throw new ResponseStatusException(UNPROCESSABLE_CONTENT);
        }
    }
}

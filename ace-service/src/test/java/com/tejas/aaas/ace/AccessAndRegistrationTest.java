package com.tejas.aaas.ace;
import com.tejas.aaas.identity.Triplet;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccessAndRegistrationTest {
    final CredentialAuthority cognito = mock(CredentialAuthority.class);
    final IdentityAuthority odl = mock(IdentityAuthority.class);
    final Triplet triplet = new Triplet("employeeId", "TEST1", "dob", "1991-12-29", "12345");
    CredentialAuthority.Account pending() {
        var a = new CredentialAuthority.Account("subject", "federated-name", "authenticated_unverified", true);
        when(cognito.current("token")).thenReturn(a); return a;
    }
    @Test void queriesOdlForEveryProtectedRequest() {
        when(cognito.current("token")).thenReturn(new CredentialAuthority.Account("subject", "user", "linked_active", true));
        when(odl.link("subject")).thenReturn(new IdentityAuthority.Link("subject", true));
        var service = new AccessService(cognito, odl);
        service.requireAccess("token", "subject"); service.requireAccess("token", "subject");
        verify(odl, times(2)).link("subject");
    }
    @Test void staleCognitoStatusDoesNotOverrideInactiveLink() {
        when(cognito.current("token")).thenReturn(new CredentialAuthority.Account("subject", "user", "linked_active", true));
        when(odl.link("subject")).thenReturn(new IdentityAuthority.Link("subject", false));
        assertThrows(ResponseStatusException.class, () -> new AccessService(cognito, odl).requireAccess("token", "subject"));
    }
    @Test void noGrantWhenOdlUnavailable() {
        when(cognito.current("token")).thenReturn(new CredentialAuthority.Account("subject", "user", "linked_active", true));
        when(odl.link("subject")).thenThrow(new ResourceAccessException("offline"));
        assertThrows(ResourceAccessException.class, () -> new AccessService(cognito, odl).requireAccess("token", "subject"));
    }
    @Test void writesOdlBeforeAllowlistingCognito() {
        var a = pending(); when(odl.resolve("subject", triplet)).thenReturn(new IdentityAuthority.Resolution("r1", 1, false));
        new RegistrationService(cognito, odl, true).resolve("token", "subject", triplet);
        var order = inOrder(odl, cognito);
        order.verify(odl).activate("subject", "r1"); order.verify(cognito).status(a, "linked_active");
        verify(cognito, never()).deleteNewFederatedAccount(any());
    }
    @Test void rejectionLogsBeforeDeletingNewAccount() {
        for (int count : new int[]{0, 2}) {
            reset(cognito, odl); var a = pending();
            when(odl.resolve("subject", triplet)).thenReturn(new IdentityAuthority.Resolution("r1", count, false));
            var error = assertThrows(ResponseStatusException.class, () -> new RegistrationService(cognito, odl, true).resolve("token", "subject", triplet));
            assertEquals(422, error.getStatusCode().value());
            var order = inOrder(odl, cognito);
            order.verify(odl).recordRejection(eq("subject"), eq("r1"), anyString());
            order.verify(cognito).deleteNewFederatedAccount(a);
        }
    }
    @Test void timeoutIsRetriableAndNeverDeletesAccount() {
        var a = pending(); when(odl.resolve("subject", triplet)).thenThrow(new ResourceAccessException("timeout"));
        var error = assertThrows(ResponseStatusException.class, () -> new RegistrationService(cognito, odl, true).resolve("token", "subject", triplet));
        assertEquals(504, error.getStatusCode().value()); verify(cognito).status(a, "resolution_timeout");
        verify(cognito, never()).deleteNewFederatedAccount(any());
    }
    @Test void correlationCannotBypassMfa() {
        pending(); when(odl.resolve("subject", triplet)).thenReturn(new IdentityAuthority.Resolution("r1", 1, true));
        assertThrows(ResponseStatusException.class, () -> new RegistrationService(cognito, odl, true).resolve("token", "subject", triplet));
        verify(odl, never()).activate(anyString(), anyString()); verify(cognito, never()).deleteNewFederatedAccount(any());
    }
    @Test void existingAccountCannotBeDeletedByOnboarding() {
        when(cognito.current("token")).thenReturn(new CredentialAuthority.Account("subject", "user", "linked_active", true));
        assertThrows(ResponseStatusException.class, () -> new RegistrationService(cognito, odl, true).resolve("token", "subject", triplet));
        verifyNoInteractions(odl); verify(cognito, never()).deleteNewFederatedAccount(any());
    }
}

package com.tejas.aaas.identity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IdentityRulesTest {
    @Test void acceptsAllFourApprovedCombinations() {
        for (String key : new String[]{"last4Ssn", "employeeId"})
            for (String date : new String[]{"dob", "hireDate"})
                assertDoesNotThrow(() -> new Triplet(key, "1234", date, "1991-12-29", "12345").validate());
    }
    @Test void rejectsUnapprovedOrMissingCombinations() {
        assertThrows(IllegalArgumentException.class, () -> new Triplet("email", "test", "dob", "1991-12-29", "12345").validate());
        assertThrows(IllegalArgumentException.class, () -> new Triplet("employeeId", "1234", "anniversary", "1991-12-29", "12345").validate());
        assertThrows(IllegalArgumentException.class, () -> new Triplet(null, null, null, null, null).validate());
    }
    @Test void rejectsInvalidDatesAndFullSsn() {
        assertThrows(IllegalArgumentException.class, () -> new Triplet("last4Ssn", "123456789", "dob", "1991-12-29", "12345").validate());
        assertThrows(IllegalArgumentException.class, () -> new Triplet("last4Ssn", "1234", "dob", "2099-12-29", "12345").validate());
        assertThrows(IllegalArgumentException.class, () -> new Triplet("last4Ssn", "1234", "dob", "2025-02-30", "12345").validate());
    }
    @Test void requiresStatusAndSameActiveSubject() {
        assertTrue(AccessPolicy.permits("linked_active", "a", "a", true));
        for (Lifecycle state : Lifecycle.values()) if (state != Lifecycle.linked_active)
            assertFalse(AccessPolicy.permits(state.name(), "a", "a", true));
        assertFalse(AccessPolicy.permits("unknown", "a", "a", true));
        assertFalse(AccessPolicy.permits("linked_active", "a", "b", true));
        assertFalse(AccessPolicy.permits("linked_active", "a", "a", false));
    }
    @Test void redactsSensitiveTripletToString() {
        assertEquals("Triplet[REDACTED]", new Triplet("last4Ssn", "1234", "dob", "1991-12-29", "12345").toString());
    }
}

package com.tejas.aaas.identity;
/** Canonical identity-linking states; unknown values must never permit access. */
public enum Lifecycle {
    authenticated_unverified, profile_complete_pending_resolution, linked_active,
    rejected_no_identity, rejected_conflict, resolution_timeout;
    public static boolean allowsAccess(String status) { return linked_active.name().equals(status); }
}

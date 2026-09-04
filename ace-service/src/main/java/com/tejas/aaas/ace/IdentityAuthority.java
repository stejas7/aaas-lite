package com.tejas.aaas.ace;
import com.tejas.aaas.identity.Triplet;
/** ODL integration contract; ACE never queries domain databases directly. */
public interface IdentityAuthority {
    record Link(String subject, boolean active) {}
    record Resolution(String resolutionId, int matchCount, boolean correlationRequired) {}
    Link link(String subject);
    Resolution resolve(String subject, Triplet triplet);
    void activate(String subject, String resolutionId);
    void recordRejection(String subject, String resolutionId, String outcome);
}

package com.tejas.aaas.identity;
/** Requires fresh ODL confirmation for the same immutable Cognito subject. */
public final class AccessPolicy {
    private AccessPolicy() {}
    public static boolean permits(String lifecycle, String subject, String linkedSubject, boolean active) {
        return Lifecycle.allowsAccess(lifecycle) && subject != null && !subject.isBlank()
            && subject.equals(linkedSubject) && active;
    }
}

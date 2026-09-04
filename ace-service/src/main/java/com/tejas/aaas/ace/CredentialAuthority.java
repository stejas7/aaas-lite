package com.tejas.aaas.ace;
/** Cognito is the credential and lifecycle authority; no passwords are held by ACE. */
public interface CredentialAuthority {
    record Account(String subject, String username, String status, boolean federated) {}
    Account current(String accessToken);
    void status(Account account, String status);
    void deleteNewFederatedAccount(Account account);
}

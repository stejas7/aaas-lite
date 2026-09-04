package com.tejas.aaas.ace;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
/** Uses the runtime IAM role for lifecycle writes and cleanup, never client-supplied usernames. */
@Component @Profile("live")
public class CognitoAuthority implements CredentialAuthority, AutoCloseable {
    private final CognitoIdentityProviderClient client;
    private final String pool;
    public CognitoAuthority(@Value("${cognito.region}") String region, @Value("${cognito.pool-id}") String pool) {
        this.pool = pool;
        client = CognitoIdentityProviderClient.builder().region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(2)).socketTimeout(Duration.ofSeconds(4)))
            .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(6))).build();
    }
    public Account current(String token) {
        var user = client.getUser(b -> b.accessToken(token));
        var authoritative = client.adminGetUser(b -> b.userPoolId(pool).username(user.username()));
        Map<String,String> attrs = authoritative.userAttributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value));
        return new Account(attrs.get("sub"), user.username(), attrs.getOrDefault("custom:lifecycle", "unknown"),
            attrs.containsKey("identities") && !attrs.get("identities").equals("[]"));
    }
    public void status(Account account, String status) {
        client.adminUpdateUserAttributes(b -> b.userPoolId(pool).username(account.username())
            .userAttributes(AttributeType.builder().name("custom:lifecycle").value(status).build()));
    }
    public void deleteNewFederatedAccount(Account account) {
        if (!account.federated() || !(account.status().equals("authenticated_unverified")
            || account.status().equals("profile_complete_pending_resolution") || account.status().equals("resolution_timeout"))) {
            throw new IllegalStateException("Account is not eligible for registration cleanup");
        }
        client.adminDeleteUser(b -> b.userPoolId(pool).username(account.username()));
    }
    @Override public void close() { client.close(); }
}

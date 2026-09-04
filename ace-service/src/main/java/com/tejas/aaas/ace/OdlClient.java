package com.tejas.aaas.ace;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import com.tejas.aaas.identity.Triplet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
/** Provisional ODL HTTP adapter; production ODL must implement the documented atomic contract. */
@Component @Profile("live")
public class OdlClient implements IdentityAuthority {
    private final RestClient client;
    public OdlClient(@Value("${odl.base-url}") String url, @Value("${odl.service-token}") String token) {
        if (!url.startsWith("https://") || token.isBlank()) throw new IllegalArgumentException("ODL requires HTTPS and service authentication");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeaders(h -> h.setBearerAuth(token)).build();
    }
    public Link link(String subject) {
        return client.get().uri("/v1/identity-links/{subject}", subject).retrieve().body(Link.class);
    }
    public Resolution resolve(String subject, Triplet triplet) {
        return client.post().uri("/v1/resolutions").body(Map.of("subject", subject, "triplet", triplet))
            .retrieve().body(Resolution.class);
    }
    public void activate(String subject, String resolutionId) {
        client.put().uri("/v1/identity-links/{subject}", subject).body(Map.of("resolutionId", resolutionId))
            .retrieve().toBodilessEntity();
    }
    public void recordRejection(String subject, String resolutionId, String outcome) {
        client.post().uri("/v1/resolution-events").body(Map.of("subject", subject,
            "resolutionId", resolutionId, "outcome", outcome)).retrieve().toBodilessEntity();
    }
}

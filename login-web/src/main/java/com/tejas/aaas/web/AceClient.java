package com.tejas.aaas.web;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import com.tejas.aaas.identity.Triplet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
/** Fixed-destination server-to-server calls; a browser cannot choose the upstream URL. */
@Component @Profile("live")
public class AceClient {
    private final RestClient client;
    public AceClient(@Value("${ace.base-url}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(45));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    @SuppressWarnings("unchecked")
    public Map<String,Object> status(String token) {
        return client.get().uri("/api/registration/status").headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class);
    }
    public void register(String token, Triplet triplet) {
        client.post().uri("/api/registration/resolve").headers(h -> h.setBearerAuth(token)).body(triplet).retrieve().toBodilessEntity();
    }
    public void authorize(String token) {
        client.get().uri("/api/protected/access").headers(h -> h.setBearerAuth(token)).retrieve().toBodilessEntity();
    }
}

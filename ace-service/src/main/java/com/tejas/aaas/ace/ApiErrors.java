package com.tejas.aaas.ace;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import software.amazon.awssdk.core.exception.SdkException;
/** No upstream bodies, triplet values or token details are returned to callers. */
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "invalid_request")); }
    @ExceptionHandler({RestClientException.class, SdkException.class})
    ResponseEntity<?> upstream(Exception e) { return ResponseEntity.status(503).body(Map.of("error", "identity_authority_unavailable")); }
}

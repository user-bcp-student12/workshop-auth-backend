package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AuthTokenService {
    public static final long TOKEN_TTL_SECONDS = 3600;
    public static final String ALGORITHM_HS256 = "HS256";

    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final Set<String> allowedClients;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Inject
    public AuthTokenService(
            @ConfigProperty(name = "auth.token.secret") String secret,
            @ConfigProperty(name = "auth.token.allowed-clients") Set<String> allowedClients,
            ObjectMapper objectMapper
    ) {
        this(secret, allowedClients, objectMapper, Clock.systemUTC());
    }

    AuthTokenService(String secret, ObjectMapper objectMapper, Clock clock) {
        this(secret, Set.of("angular-workshop"), objectMapper, clock);
    }

    AuthTokenService(String secret, Set<String> allowedClients, ObjectMapper objectMapper, Clock clock) {
        this.secret = secret;
        this.allowedClients = Set.copyOf(allowedClients);
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String create(String username, String client, String role) {
        if (!isClientAllowed(client)) {
            throw new IllegalArgumentException("Client is not allowed");
        }

        long issuedAt = clock.instant().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", ALGORITHM_HS256);
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", username);
        claims.put("client", client);
        claims.put("role", role);
        claims.put("iat", issuedAt);
        claims.put("exp", issuedAt + TOKEN_TTL_SECONDS);

        try {
            String signingInput = encodeJson(header) + "." + encodeJson(claims);
            return signingInput + "." + sign(signingInput);
        } catch (JsonProcessingException | GeneralSecurityException error) {
            throw new IllegalStateException("Unable to create authentication token", error);
        }
    }

    boolean isClientAllowed(String client) {
        return client != null && allowedClients.contains(client);
    }

    JsonNode verify(String token) {
        if (token == null) {
            throw invalidToken();
        }

        try {
            String[] segments = token.split("\\.", -1);
            if (segments.length != 3) {
                throw invalidToken();
            }

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[0]));
            if (header == null || !header.isObject() || !ALGORITHM_HS256.equals(header.path("alg").asText())) {
                throw invalidToken();
            }

            String signingInput = segments[0] + "." + segments[1];
            byte[] expectedSignature = Base64.getUrlDecoder().decode(sign(signingInput));
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(segments[2]);
            if (!MessageDigest.isEqual(expectedSignature, suppliedSignature)) {
                throw invalidToken();
            }

            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
            JsonNode expiration = claims == null ? null : claims.get("exp");
            if (claims == null || !claims.isObject() || expiration == null || !expiration.isNumber()
                    || expiration.asLong() <= clock.instant().getEpochSecond()) {
                throw invalidToken();
            }
            return claims;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException error) {
            throw invalidToken();
        }
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("Invalid authentication token");
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        byte[] json = objectMapper.writeValueAsBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
    }

    private String sign(String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));
        byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
}